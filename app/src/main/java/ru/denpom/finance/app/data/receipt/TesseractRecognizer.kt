package ru.denpom.finance.app.data.receipt

import android.content.Context
import com.googlecode.tesseract.android.TessBaseAPI
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.denpom.finance.app.domain.model.Receipt
import ru.denpom.finance.app.domain.model.ReceiptStatus
import ru.denpom.finance.app.domain.repository.ReceiptRecognizer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TesseractRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: ReceiptTextParser,
) : ReceiptRecognizer {

    private val mutex = Mutex()

    override suspend fun recognize(imagePath: String): Receipt? = withContext(Dispatchers.Default) {
        mutex.withLock {
            val bitmap = ImageLoader.load(imagePath) ?: return@withLock null
            val prepared = ImageLoader.forOcr(bitmap)
            val api = TessBaseAPI()
            val text = try {
                if (!api.init(dataDir().absolutePath, "rus")) return@withLock null
                api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                api.setImage(prepared)
                api.utF8Text.orEmpty()
            } finally {
                api.recycle()
            }
            val parsed = parser.parse(text)
            if (parsed.items.isEmpty() && parsed.total == null) return@withLock null
            val itemsTotal = parsed.items.sumOf { it.sum }
            Receipt(
                shop = parsed.shop.ifBlank { "Чек" },
                address = "",
                timestamp = parsed.timestamp ?: System.currentTimeMillis(),
                total = parsed.total ?: itemsTotal,
                qrRaw = null,
                status = ReceiptStatus.RECOGNIZED,
                photoPath = imagePath,
                warrantyUntil = null,
                items = parsed.items,
            )
        }
    }

    private fun dataDir(): File {
        val root = File(context.filesDir, "ocr")
        val tess = File(root, "tessdata").apply { mkdirs() }
        val model = File(tess, "rus.traineddata")
        if (!model.exists() || model.length() == 0L) {
            context.assets.open("tessdata/rus.traineddata").use { input ->
                model.outputStream().use { input.copyTo(it) }
            }
        }
        return root
    }
}
