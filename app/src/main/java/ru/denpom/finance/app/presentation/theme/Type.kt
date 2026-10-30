package ru.denpom.finance.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.denpom.finance.app.R

val Onest = FontFamily(
    Font(R.font.onest_regular, FontWeight.Normal),
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
    Font(R.font.onest_bold, FontWeight.Bold),
)

private const val TABULAR = "tnum"

object AppText {
    val amountXL = TextStyle(fontFamily = Onest, fontSize = 42.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em, lineHeight = 44.sp, fontFeatureSettings = TABULAR)
    val amountL = TextStyle(fontFamily = Onest, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em, lineHeight = 38.sp, fontFeatureSettings = TABULAR)
    val ring = TextStyle(fontFamily = Onest, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em, lineHeight = 40.sp, fontFeatureSettings = TABULAR)
    val title = TextStyle(fontFamily = Onest, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em, lineHeight = 30.sp)
    val headline = TextStyle(fontFamily = Onest, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.015).em, lineHeight = 32.sp)
    val bar = TextStyle(fontFamily = Onest, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
    val section = TextStyle(fontFamily = Onest, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
    val body = TextStyle(fontFamily = Onest, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val bodyMedium = TextStyle(fontFamily = Onest, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    val amount = TextStyle(fontFamily = Onest, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, fontFeatureSettings = TABULAR)
    val amountM = TextStyle(fontFamily = Onest, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp, fontFeatureSettings = TABULAR)
    val label = TextStyle(fontFamily = Onest, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = Onest, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, fontFeatureSettings = TABULAR)
    val small = TextStyle(fontFamily = Onest, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, fontFeatureSettings = TABULAR)
    val tiny = TextStyle(fontFamily = Onest, fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp, fontFeatureSettings = TABULAR)
}

val MaterialTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Onest),
        displayMedium = t.displayMedium.copy(fontFamily = Onest),
        displaySmall = t.displaySmall.copy(fontFamily = Onest),
        headlineLarge = t.headlineLarge.copy(fontFamily = Onest),
        headlineMedium = t.headlineMedium.copy(fontFamily = Onest),
        headlineSmall = t.headlineSmall.copy(fontFamily = Onest),
        titleLarge = t.titleLarge.copy(fontFamily = Onest),
        titleMedium = t.titleMedium.copy(fontFamily = Onest),
        titleSmall = t.titleSmall.copy(fontFamily = Onest),
        bodyLarge = t.bodyLarge.copy(fontFamily = Onest),
        bodyMedium = t.bodyMedium.copy(fontFamily = Onest),
        bodySmall = t.bodySmall.copy(fontFamily = Onest),
        labelLarge = t.labelLarge.copy(fontFamily = Onest),
        labelMedium = t.labelMedium.copy(fontFamily = Onest),
        labelSmall = t.labelSmall.copy(fontFamily = Onest),
    )
}
