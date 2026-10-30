package ru.denpom.finance.app.presentation.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object Oklch {
    fun color(l: Float, c: Float, hue: Float, alpha: Float = 1f): Color {
        val rad = Math.toRadians(hue.toDouble())
        val a = c * cos(rad)
        val b = c * sin(rad)
        val l1 = l + 0.3963377774 * a + 0.2158037573 * b
        val m1 = l - 0.1055613458 * a - 0.0638541728 * b
        val s1 = l - 0.0894841775 * a - 1.2914855480 * b
        val ll = l1 * l1 * l1
        val mm = m1 * m1 * m1
        val ss = s1 * s1 * s1
        val r = 4.0767416621 * ll - 3.3077115913 * mm + 0.2309699292 * ss
        val g = -1.2684380046 * ll + 2.6097574011 * mm - 0.3413193965 * ss
        val bl = -0.0041960863 * ll - 0.7034186147 * mm + 1.7076147010 * ss
        return Color(gamma(r), gamma(g), gamma(bl), alpha)
    }

    private fun gamma(x: Double): Float {
        val v = if (x > 0.0031308) 1.055 * x.pow(1 / 2.4) - 0.055 else 12.92 * x
        return v.coerceIn(0.0, 1.0).toFloat()
    }
}
