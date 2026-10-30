package ru.denpom.finance.app.presentation.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText

@Composable
fun LockScreen(
    biometric: Boolean,
    check: suspend (String) -> Boolean,
    onUnlock: () -> Unit,
    onExit: () -> Unit,
) {
    val c = App.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var errors by remember { mutableIntStateOf(0) }
    val canBio = biometric && remember { canUseBiometric(context) }
    val bio: (() -> Unit)? = if (canBio) ({ showBiometric(context, "Вход в «Денежный помощник»", onUnlock) }) else null

    BackHandler(onBack = onExit)
    LaunchedEffect(Unit) { bio?.invoke() }

    Box(
        Modifier.fillMaxSize().background(c.bg).clickable(remember { MutableInteractionSource() }, null) {},
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding().navigationBarsPadding().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(64.dp).clip(CircleShape).background(c.priC), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lock, null, tint = c.onPriC, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(if (errors > 0) "Неверный PIN" else "Введите PIN", style = AppText.title.copy(fontSize = 20.sp), color = if (errors > 0) c.exp else c.on)
            Spacer(Modifier.height(24.dp))
            PinDots(pin.length, errors)
            Spacer(Modifier.weight(1f))
            PinPad(
                onDigit = { d ->
                    if (pin.length >= PIN_LENGTH) return@PinPad
                    pin += d
                    if (pin.length == PIN_LENGTH) {
                        val attempt = pin
                        scope.launch {
                            if (check(attempt)) onUnlock() else {
                                errors++
                                pin = ""
                            }
                        }
                    }
                },
                onDelete = { pin = pin.dropLast(1) },
                onBiometric = bio,
            )
        }
    }
}
