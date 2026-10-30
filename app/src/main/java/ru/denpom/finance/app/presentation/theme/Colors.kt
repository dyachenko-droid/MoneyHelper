package ru.denpom.finance.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val bg: Color,
    val s1: Color,
    val s2: Color,
    val s3: Color,
    val on: Color,
    val on2: Color,
    val pri: Color,
    val onPri: Color,
    val priC: Color,
    val onPriC: Color,
    val amb: Color,
    val ambC: Color,
    val onAmbC: Color,
    val warn: Color,
    val warnC: Color,
    val onWarnC: Color,
    val exp: Color,
    val inc: Color,
    val hero: Color,
    val onHero: Color,
    val invPri: Color,
    val isDark: Boolean,
    private val catBgL: Float,
    private val catBgC: Float,
    private val catFgL: Float,
    private val catFgC: Float,
) {
    fun catBg(hue: Float): Color = Oklch.color(catBgL, catBgC, hue)
    fun catFg(hue: Float): Color = Oklch.color(catFgL, catFgC, hue)
    fun chart(hue: Float): Color = Oklch.color(0.66f, 0.11f, hue)
}

val LightColors = AppColors(
    bg = Color(0xFFF3F7FC),
    s1 = Color(0xFFFCFEFF),
    s2 = Color(0xFFE6EEF8),
    s3 = Color(0xFFD8E3F1),
    on = Color(0xFF131B26),
    on2 = Color(0xFF535F6E),
    pri = Color(0xFF1364CE),
    onPri = Color(0xFFF8FAFD),
    priC = Color(0xFFCDE3FF),
    onPriC = Color(0xFF053069),
    amb = Color(0xFF32B364),
    ambC = Color(0xFFD1F6D9),
    onAmbC = Color(0xFF0C4D26),
    warn = Color(0xFFE29936),
    warnC = Color(0xFFFDEACC),
    onWarnC = Color(0xFF61370D),
    exp = Color(0xFFBB584A),
    inc = Color(0xFF198044),
    hero = Color(0xFF0F3A90),
    onHero = Color(0xFFF2F7FE),
    invPri = Color(0xFFA4C6F8),
    isDark = false,
    catBgL = 0.93f,
    catBgC = 0.045f,
    catFgL = 0.43f,
    catFgC = 0.11f,
)

val DarkColors = AppColors(
    bg = Color(0xFF0A0F14),
    s1 = Color(0xFF12181E),
    s2 = Color(0xFF1A212A),
    s3 = Color(0xFF232B35),
    on = Color(0xFFE5EAF0),
    on2 = Color(0xFFA3ACB8),
    pri = Color(0xFF80B3FD),
    onPri = Color(0xFF031D44),
    priC = Color(0xFF173766),
    onPriC = Color(0xFFCFE3FF),
    amb = Color(0xFF65D68A),
    ambC = Color(0xFF1A3E26),
    onAmbC = Color(0xFFBFEFCB),
    warn = Color(0xFFEEB154),
    warnC = Color(0xFF402C12),
    onWarnC = Color(0xFFF7DAAA),
    exp = Color(0xFFEF9687),
    inc = Color(0xFF78D694),
    hero = Color(0xFF0E2C67),
    onHero = Color(0xFFEFF4FA),
    invPri = Color(0xFF1D5AB0),
    isDark = true,
    catBgL = 0.31f,
    catBgC = 0.05f,
    catFgL = 0.86f,
    catFgC = 0.09f,
)

val ScanAccent = Color(0xFF65D68A)
val BudgetBarOnHero = Color(0xFF88E6A3)
val ScannerBg = Color(0xFF0A0E12)

val LocalAppColors = staticCompositionLocalOf { LightColors }
