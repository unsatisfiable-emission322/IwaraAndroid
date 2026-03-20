package junzi.iwara.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8D2A1D),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6A4B2F),
    tertiary = Color(0xFF1F5C5C),
    background = Color(0xFFF5EFE6),
    surface = Color(0xFFFFFAF4),
    surfaceVariant = Color(0xFFE6DED2),
)

@Suppress("unused")
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A7),
    secondary = Color(0xFFE1C29F),
    tertiary = Color(0xFF96D5D5),
)

@Composable
fun IwaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}

