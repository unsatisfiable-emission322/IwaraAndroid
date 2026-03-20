package junzi.iwara

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BrowserBridge {
    private const val PREWARM_URL = "https://www.iwara.tv/"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = ConcurrentHashMap<String, CompletableDeferred<BrowserResponse>>()

    @Volatile
    private var webView: WebView? = null
    @Volatile
    private var activeLoad: CompletableDeferred<Unit>? = null
    @Volatile
    private var bridgeReady = false

    fun isAttached(): Boolean = webView != null

    fun attach(candidate: WebView) {
        if (webView === candidate) return
        webView = candidate
        bridgeReady = false
        candidate.settings.javaScriptEnabled = true
        candidate.settings.domStorageEnabled = true
        candidate.settings.loadsImagesAutomatically = false
        candidate.settings.mediaPlaybackRequiresUserGesture = false
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(candidate, true)
        candidate.addJavascriptInterface(BridgeJsInterface, "IwaraBridge")
        candidate.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                probePageReady(view)
            }
        }
        beginLoad(PREWARM_URL)
    }

    suspend fun fetch(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): BrowserResponse {
        ensureReady()
        return executeFetch(method, url, headers, body, allowChallengeRetry = true)
    }

    private suspend fun executeFetch(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
        allowChallengeRetry: Boolean,
    ): BrowserResponse {
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<BrowserResponse>()
        pending[requestId] = deferred

        val headersJson = org.json.JSONObject(headers).toString()
        val bodyJson = body?.let(org.json.JSONObject::quote) ?: "null"
        val escapedUrl = org.json.JSONObject.quote(url)
        val escapedMethod = org.json.JSONObject.quote(method)
        val script = """
            (function() {
              const requestId = ${org.json.JSONObject.quote(requestId)};
              const options = { method: $escapedMethod, headers: $headersJson, credentials: 'include' };
              const body = $bodyJson;
              if (body !== null) options.body = body;
              fetch($escapedUrl, options)
                .then(async (response) => {
                  const text = await response.text();
                  IwaraBridge.postResult(requestId, JSON.stringify({ status: response.status, body: text }));
                })
                .catch((error) => {
                  IwaraBridge.postResult(requestId, JSON.stringify({ status: 0, body: String(error) }));
                });
            })();
        """.trimIndent()

        mainHandler.post { webView?.evaluateJavascript(script, null) }
        val response = deferred.await()
        if (allowChallengeRetry && response.isCloudflareChallenge()) {
            resolveChallenge(url)
            return executeFetch(method, url, headers, body, allowChallengeRetry = false)
        }
        return response
    }

    private suspend fun ensureReady() {
        if (bridgeReady) return
        activeLoad?.await()
    }

    private suspend fun resolveChallenge(url: String) {
        beginLoad(url)
        activeLoad?.await()
        beginLoad(PREWARM_URL)
        activeLoad?.await()
    }

    private fun beginLoad(url: String) {
        val deferred = CompletableDeferred<Unit>()
        activeLoad = deferred
        bridgeReady = false
        mainHandler.post {
            webView?.loadUrl(url)
        }
    }

    private fun probePageReady(view: WebView) {
        view.evaluateJavascript(
            "(function(){return JSON.stringify({title:document.title, body: (document.body && document.body.innerText ? document.body.innerText.slice(0,400) : '')});})()",
        ) { raw ->
            val payload = raw?.trim()?.removePrefix("\"")?.removeSuffix("\"")?.replace("\\\"", "\"")?.replace("\\n", "\n")
            val text = payload ?: ""
            val waiting = text.contains("Just a moment", ignoreCase = true) ||
                text.contains("Enable JavaScript and cookies to continue", ignoreCase = true)
            if (waiting) {
                mainHandler.postDelayed({ probePageReady(view) }, 2000)
            } else {
                bridgeReady = true
                activeLoad?.complete(Unit)
                activeLoad = null
            }
        }
    }

    private object BridgeJsInterface {
        @JavascriptInterface
        fun postResult(requestId: String, payload: String) {
            val result = runCatching {
                val json = org.json.JSONObject(payload)
                BrowserResponse(
                    statusCode = json.optInt("status"),
                    body = json.optString("body"),
                )
            }.getOrElse {
                BrowserResponse(0, payload)
            }
            pending.remove(requestId)?.complete(result)
        }
    }
}

data class BrowserResponse(
    val statusCode: Int,
    val body: String,
) {
    fun isCloudflareChallenge(): Boolean {
        return body.contains("Just a moment", ignoreCase = true) ||
            body.contains("__cf_chl_opt", ignoreCase = true) ||
            body.contains("Enable JavaScript and cookies to continue", ignoreCase = true)
    }
}


