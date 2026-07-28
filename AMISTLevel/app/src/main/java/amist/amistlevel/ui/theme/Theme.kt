package amist.amistlevel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme( background = Color( 0xFF333333 ),
                                                onBackground = Color( 0xFF333333 ),
                                                primary = Color( 0xFF555555 ),
                                                onPrimary = Color.White,
                                                secondary = Color( 0xFF777777 ),
                                                onSecondary = Color.White,
                                                tertiary = Color( 0xFF999999 ),
                                                onTertiary = Color.White,
                                                surface = Color( 0xFF212121 ),
                                                onSurface = Color.White )

@Composable
fun AMISTLevelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}