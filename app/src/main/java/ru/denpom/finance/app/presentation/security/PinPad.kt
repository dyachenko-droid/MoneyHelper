package ru.denpom.finance.app.presentation.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText

const val PIN_LENGTH = 4

@Composable
fun PinDots(filled: Int, errorTick: Int) {
    val c = App.colors
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            listOf(14f, -12f, 9f, -6f, 3f, 0f).forEach { shake.animateTo(it, androidx.compose.animation.core.tween(45)) }
        }
    }
    Row(Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PIN_LENGTH) { i ->
            Box(Modifier.size(14.dp).clip(CircleShape).background(if (i < filled) c.pri else c.s3))
        }
    }
}

@Composable
fun PinPad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onBiometric: (() -> Unit)?) {
    val c = App.colors
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { d -> KeyLabel(d.toString()) { haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap); onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            if (onBiometric != null) {
                Box(Modifier.size(72.dp).clip(CircleShape).clickable(onClick = onBiometric), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Fingerprint, "Отпечаток", tint = c.pri, modifier = Modifier.size(32.dp))
                }
            } else Box(Modifier.width(72.dp))
            KeyLabel("0") { haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap); onDigit('0') }
            Box(Modifier.size(72.dp).clip(CircleShape).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, "Стереть", tint = c.on2, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun KeyLabel(label: String, onClick: () -> Unit) {
    val c = App.colors
    Box(Modifier.size(72.dp).clip(CircleShape).background(c.s2).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, style = AppText.amountM.copy(fontSize = 26.sp), color = c.on)
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

fun canUseBiometric(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

fun showBiometric(context: Context, title: String, onSuccess: () -> Unit) {
    val activity = context.findFragmentActivity() ?: return
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Ввести PIN")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
    )
}
