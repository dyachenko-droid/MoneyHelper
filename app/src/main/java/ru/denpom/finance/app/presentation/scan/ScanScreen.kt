package ru.denpom.finance.app.presentation.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.denpom.finance.app.domain.model.FiscalQr
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.presentation.AddRequest
import ru.denpom.finance.app.presentation.LocalAppActions
import ru.denpom.finance.app.presentation.components.PrimaryButton
import ru.denpom.finance.app.presentation.components.SheetHandle
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.components.rememberPermissionRequest
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import ru.denpom.finance.app.presentation.theme.ScanAccent
import ru.denpom.finance.app.presentation.theme.ScannerBg
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun ScanScreen(
    onClose: () -> Unit,
    onDraft: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val haptics = LocalHapticFeedback.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberPermissionRequest { granted = it }
    LaunchedEffect(Unit) { if (!granted) permission(Manifest.permission.CAMERA) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ScanEvent.OpenDraft -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDraft()
                }
                is ScanEvent.QuickAdd -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    quickAdd(event.qr, onClose, actions)
                }
            }
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        copyToReceipts(context, uri)?.let { viewModel.onPhoto(it) }
    }

    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    var camera by remember { mutableStateOf<Camera?>(null) }

    Box(Modifier.fillMaxSize().background(ScannerBg)) {
        if (granted) {
            CameraPreview(
                imageCapture = imageCapture,
                qrEnabled = { state.mode == ScanMode.QR && !viewModel.busy },
                onQr = { viewModel.onQrDetected(it) },
                onCamera = { camera = it },
            )
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.NoPhotography, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                Text("Нужен доступ к камере, чтобы сканировать чеки", style = AppText.body, color = Color.White, textAlign = TextAlign.Center)
                PrimaryButton("Разрешить", height = 48.dp, container = Color.White, content = ScannerBg) {
                    permission(Manifest.permission.CAMERA)
                }
            }
        }
        LaunchedEffect(state.flash, camera) { camera?.cameraControl?.enableTorch(state.flash) }

        Column(Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                CircleButton(Icons.Rounded.Close, Color.Black.copy(alpha = 0.35f), Color.White, "Закрыть", onClose)
                Spacer(Modifier.weight(1f))
                CircleButton(
                    if (state.flash) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    if (state.flash) ScanAccent else Color.Black.copy(alpha = 0.35f),
                    if (state.flash) Color(0xFF0C2815) else Color.White,
                    "Вспышка",
                    viewModel::toggleFlash,
                )
            }
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                ScanFrame(state.mode) {
                    if (state.mode == ScanMode.PHOTO) capture(context, imageCapture) { viewModel.onPhoto(it) }
                }
                Text(
                    state.hint ?: if (state.mode == ScanMode.QR) "Наведите на QR-код — заполним сумму и дату" else "Поместите чек целиком в рамку",
                    style = AppText.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(4.dp)) {
                    ModeButton("QR-код", state.mode == ScanMode.QR) { viewModel.setMode(ScanMode.QR) }
                    ModeButton("Фото чека", state.mode == ScanMode.PHOTO) { viewModel.setMode(ScanMode.PHOTO) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SideAction(Icons.Rounded.PhotoLibrary, "Галерея") {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(76.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (state.mode == ScanMode.PHOTO) capture(context, imageCapture) { viewModel.onPhoto(it) }
                            else viewModel.setMode(ScanMode.PHOTO)
                        }.padding(8.dp),
                    ) { Box(Modifier.fillMaxSize().clip(CircleShape).background(ScanAccent)) }
                    Spacer(Modifier.weight(1f))
                    SideAction(Icons.Rounded.Keyboard, "Вручную") {
                        onClose()
                        actions.openAdd(AddRequest(OperationType.EXPENSE))
                    }
                }
            }
        }

        when (val phase = state.phase) {
            is ScanPhase.Loading -> LoadingOverlay(phase.title, phase.body)
            is ScanPhase.Failed -> ResultSheet(
                icon = Icons.Rounded.ErrorOutline,
                title = phase.message,
                body = "Попробуйте ещё раз, отсканируйте QR-код или введите сумму вручную.",
                primary = "Ввести вручную",
                secondary = "Повторить",
                onPrimary = {
                    onClose()
                    actions.openAdd(AddRequest(OperationType.EXPENSE))
                },
                onSecondary = viewModel::reset,
                onDismiss = viewModel::reset,
            )
            ScanPhase.Idle -> Unit
        }
    }
}

private fun quickAdd(qr: FiscalQr, onClose: () -> Unit, actions: ru.denpom.finance.app.presentation.AppActions) {
    onClose()
    actions.openAdd(
        AddRequest(
            type = OperationType.EXPENSE,
            amount = qr.sum,
            title = "Покупка по чеку",
            timestamp = qr.timestamp,
        )
    )
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    qrEnabled: () -> Boolean,
    onQr: (String) -> Unit,
    onCamera: (Camera) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val enabled by rememberUpdatedState(qrEnabled)
    val callback by rememberUpdatedState(onQr)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        QrAnalyzer(enabled = { enabled() }, onCode = { code -> ContextCompat.getMainExecutor(context).execute { callback(code) } })
    }

    LaunchedEffect(lifecycleOwner) {
        val provider = cameraProvider(context)
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, analyzer) }
        provider.unbindAll()
        runCatching {
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis, imageCapture)
        }.onSuccess(onCamera)
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            analyzer.close()
            executor.shutdown()
        }
    }
    AndroidView({ previewView }, Modifier.fillMaxSize())
}

private suspend fun cameraProvider(context: Context): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
}

private fun capture(context: Context, imageCapture: ImageCapture, onSaved: (String) -> Unit) {
    val file = newReceiptFile(context)
    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSaved(file.absolutePath)
            override fun onError(exception: ImageCaptureException) = Unit
        },
    )
}

private fun newReceiptFile(context: Context): File =
    File(File(context.filesDir, "receipts").apply { mkdirs() }, "r_${System.currentTimeMillis()}.jpg")

private fun copyToReceipts(context: Context, uri: Uri): String? = runCatching {
    val file = newReceiptFile(context)
    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
    file.absolutePath
}.getOrNull()

@Composable
private fun ScanFrame(mode: ScanMode, onTap: () -> Unit) {
    val w by animateDpAsState(if (mode == ScanMode.QR) 232.dp else 248.dp, label = "w")
    val h by animateDpAsState(if (mode == ScanMode.QR) 232.dp else 340.dp, label = "h")
    val transition = rememberInfiniteTransition(label = "scan")
    val line by transition.animateFloat(0.08f, 0.88f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "line")
    Box(Modifier.width(w).height(h).clip(RoundedCornerShape(20.dp)).clickable(onClick = onTap)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val len = 36.dp.toPx()
            val r = 20.dp.toPx()
            val half = stroke / 2
            val corners = listOf(
                Offset(half, half) to 180f,
                Offset(size.width - half - 2 * r, half) to 270f,
                Offset(size.width - half - 2 * r, size.height - half - 2 * r) to 0f,
                Offset(half, size.height - half - 2 * r) to 90f,
            )
            corners.forEach { (o, start) ->
                drawArc(Color.White, start, 90f, false, o, Size(2 * r, 2 * r), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val straight = len - r
            drawLine(Color.White, Offset(half, half + r), Offset(half, half + r + straight), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(half + r, half), Offset(half + r + straight, half), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(size.width - half, half + r), Offset(size.width - half, half + r + straight), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(size.width - half - r, half), Offset(size.width - half - r - straight, half), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(half, size.height - half - r), Offset(half, size.height - half - r - straight), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(half + r, size.height - half), Offset(half + r + straight, size.height - half), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(size.width - half, size.height - half - r), Offset(size.width - half, size.height - half - r - straight), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(size.width - half - r, size.height - half), Offset(size.width - half - r - straight, size.height - half), stroke, StrokeCap.Round)
            val y = size.height * line
            drawRoundRect(ScanAccent.copy(alpha = 0.25f), Offset(size.width * 0.1f, y - 5.dp.toPx()), Size(size.width * 0.8f, 12.dp.toPx()), CornerRadius(6.dp.toPx()))
            drawRoundRect(ScanAccent, Offset(size.width * 0.1f, y), Size(size.width * 0.8f, 2.dp.toPx()), CornerRadius(1.dp.toPx()))
        }
    }
}

@Composable
private fun ModeButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(36.dp).clip(RoundedCornerShape(16.dp)).background(if (active) Color.White else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = AppText.label, color = if (active) ScannerBg else Color.White) }
}

@Composable
private fun CircleButton(icon: ImageVector, bg: Color, fg: Color, label: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(bg).clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SideAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, style = AppText.small, color = Color.White)
    }
}

@Composable
private fun BoxScope.LoadingOverlay(title: String, body: String) {
    val c = App.colors
    Box(Modifier.fillMaxSize().background(Color(0xB804060A)).clickable(enabled = true) {}, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(32.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.s1).padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CircularProgressIndicator(color = c.pri, trackColor = c.s3, strokeWidth = 4.dp, modifier = Modifier.size(44.dp))
            Text(title, style = AppText.bodyMedium.copy(fontSize = 16.sp), color = c.on, textAlign = TextAlign.Center)
            Text(body, style = AppText.caption, color = c.on2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BoxScope.ResultSheet(
    icon: ImageVector,
    title: String,
    body: String,
    primary: String,
    secondary: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = App.colors
    Box(Modifier.fillMaxSize().background(Color(0x9904060A)).clickable(onClick = onDismiss))
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(c.s1)
            .clickable(enabled = true) {}
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SheetHandle(Modifier.padding(bottom = 8.dp))
        Box(Modifier.size(48.dp).clip(CircleShape).background(c.warnC), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.onWarnC, modifier = Modifier.size(24.dp))
        }
        Text(title, style = AppText.title.copy(fontSize = 20.sp, lineHeight = 26.sp), color = c.on)
        Text(
            body,
            style = AppText.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = c.on2,
        )
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(primary, Modifier.fillMaxWidth(), onClick = onPrimary)
            TextAction(secondary, Modifier.fillMaxWidth(), onClick = onSecondary)
        }
    }
}
