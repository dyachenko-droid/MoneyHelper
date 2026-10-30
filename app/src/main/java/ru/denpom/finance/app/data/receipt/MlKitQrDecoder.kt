package ru.denpom.finance.app.data.receipt

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ru.denpom.finance.app.domain.repository.QrImageDecoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MlKitQrDecoder @Inject constructor() : QrImageDecoder {

    private val scanner by lazy {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }

    override suspend fun decode(imagePath: String): List<String> {
        val bitmap = withContext(Dispatchers.IO) { ImageLoader.load(imagePath, 1600) } ?: return emptyList()
        return suspendCancellableCoroutine { cont ->
            scanner.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { codes -> cont.resume(codes.mapNotNull { it.rawValue }) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
    }
}
