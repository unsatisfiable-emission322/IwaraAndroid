package junzi.iwara.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun AsyncRemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var bitmapState by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loading by remember(url) { mutableStateOf(url != null) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            bitmapState = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        bitmapState = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
        loading = false
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = bitmapState
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (loading) {
            CircularProgressIndicator()
        }
    }
}

