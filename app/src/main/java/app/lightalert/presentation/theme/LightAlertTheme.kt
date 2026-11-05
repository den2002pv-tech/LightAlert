package app.lightalert.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF2457C5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = Color(0xFF001849),
    secondary = Color(0xFF555F76),
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF121B2E),
    tertiary = Color(0xFF006B5B),
    background = Color(0xFFF8F9FF),
    surface = Color(0xFFF8F9FF),
    surfaceVariant = Color(0xFFE2E5F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB5C6FF),
    onPrimary = Color(0xFF002B73),
    primaryContainer = Color(0xFF123F99),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFFBEC6DD),
    secondaryContainer = Color(0xFF3D475E),
    onSecondaryContainer = Color(0xFFDCE2F9),
    tertiary = Color(0xFF75D7C2),
    background = Color(0xFF10131B),
    surface = Color(0xFF10131B),
    surfaceVariant = Color(0xFF424653),
)

@Composable
fun LightAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
