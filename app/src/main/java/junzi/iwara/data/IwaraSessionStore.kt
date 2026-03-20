package junzi.iwara.data

import android.content.Context

class IwaraSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("iwara_session", Context.MODE_PRIVATE)

    fun readRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun readAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun save(refreshToken: String, accessToken: String?) {
        prefs.edit()
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}

