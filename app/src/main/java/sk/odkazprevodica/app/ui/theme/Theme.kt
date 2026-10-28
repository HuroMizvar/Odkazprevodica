package sk.odkazprevodica.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Farby prevedenia A1 · Signál (tmavé, jantárová). */
object AppColors {
    val Background = Color(0xFF121417)
    val Surface = Color(0xFF1C1F24)
    val Outline = Color(0xFF2A2E35)
    val OutlineDashed = Color(0xFF3A3F48)
    val Text = Color(0xFFF2F2F0)
    val TextMuted = Color(0xFFA3A8B0)
    val TextSoft = Color(0xFFD4D7DC)
    val Amber = Color(0xFFF5B301)
    val OnAmber = Color(0xFF121417)
    val OnAmberSoft = Color(0xFF3A2E00)
    val AmberSelected = Color(0xFF2A2410)
    val IconIdle = Color(0xFFC9CDD3)
    val Urgent = Color(0xFFFF8A7A)
    val PlateWhite = Color(0xFFF2F2F0)
    val PlateBlue = Color(0xFF1F3FA8)
}

// TODO: nahradiť písmom Archivo (Google Fonts) – zatiaľ systémové bezpätkové.
private val AppFont = FontFamily.SansSerif

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.2.sp),
    labelSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val AppColorScheme = darkColorScheme(
    primary = AppColors.Amber,
    onPrimary = AppColors.OnAmber,
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Surface,
    onSurface = AppColors.Text,
    surfaceContainerLow = AppColors.Surface,
    onSurfaceVariant = AppColors.TextMuted,
    outline = AppColors.Outline,
    error = AppColors.Urgent,
)

@Composable
fun OdkazTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content,
    )
}
