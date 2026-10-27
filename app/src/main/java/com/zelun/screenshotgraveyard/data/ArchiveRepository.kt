package com.zelun.screenshotgraveyard.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.work.*
import com.zelun.screenshotgraveyard.domain.ArchiveRules
import com.zelun.screenshotgraveyard.worker.OcrWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class ImportResult(val imported: Int = 0, val duplicates: Int = 0, val failed: Int = 0)

class ArchiveRepository(private val context: Context, val dao: ScreenshotDao) {
    val directory = File(context.filesDir, "screenshots").apply { mkdirs() }
    private val importMutex = Mutex()
    private val workManager get() = WorkManager.getInstance(context)
    fun file(item: ScreenshotEntity) = File(directory, item.imageName)

    suspend fun importUris(uris: List<Uri>, progress: (Int, Int) -> Unit): ImportResult = importMutex.withLock {
        var result = ImportResult()
        val limited = uris.distinct().take(ArchiveRules.MAX_IMPORT_BATCH)
        limited.forEachIndexed { index, uri ->
            coroutineContext.ensureActive()
            try {
                require(uri.scheme == "content") { "Select an image from your photo library." }
                val name = withContext(Dispatchers.IO) {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else null
                    } ?: "New screenshot"
                }
                val inserted = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("The image is no longer available.")
                    stream.use { importStream(it, name) }
                }
                result = if (inserted) result.copy(imported = result.imported + 1)
                    else result.copy(duplicates = result.duplicates + 1)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { result = result.copy(failed = result.failed + 1) }
            progress(index + 1, limited.size)
        }
        result
    }

    private suspend fun importStream(stream: InputStream, name: String, demo: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("import-", ".tmp", context.cacheDir)
        var committed = false
        var destination: File? = null
        var contentHash: String? = null
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            temp.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = stream.read(buffer)
                    if (count == -1) break
                    total += count
                    if (total > ArchiveRules.MAX_IMPORT_BYTES) throw IOException("Image exceeds the 25 MB limit.")
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            contentHash = hash
            if (dao.getByHash(hash) != null) return@withContext false
            ImageFiles.decode(temp, 128).recycle() // Validate before storing or scheduling OCR.
            // Keep a real extension so FileProvider exposes the correct MIME type to recipients.
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(ImageFiles.mimeType(temp))
                ?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,10}")) } ?: "img"
            destination = File(directory, "$hash.$extension")
            temp.copyTo(destination, overwrite = true)
            val id = dao.insert(ScreenshotEntity(imageName = destination.name, sha256 = hash,
                title = ArchiveRules.suggestedTitle("", name), bytes = total, isDemo = demo))
            committed = id != -1L
            if (committed) enqueueOcr(id)
            committed
        } finally {
            temp.delete()
            // Once inserted, WorkManager recovery on startup can resume unfinished OCR.
            if (!committed && destination != null && contentHash != null && dao.getByHash(contentHash) == null) destination.delete()
        }
    }

    suspend fun loadDemo(): ImportResult = importMutex.withLock {
        var result = ImportResult()
        val names = listOf("Midnight miso ramen", "Git survival kit", "A little coffee detour", "A night worth saving", "Kyoto someday", "Make room for possibility")
        names.forEachIndexed { index, title ->
            val inserted = context.assets.open("demo/${index + 1}.png").use { importStream(it, title, demo = true) }
            result = if (inserted) result.copy(imported = result.imported + 1) else result.copy(duplicates = result.duplicates + 1)
        }
        result
    }

    fun enqueueOcr(id: Long, replace: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<OcrWorker>()
            .setInputData(workDataOf("screenshotId" to id))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build()
        workManager.enqueueUniqueWork("ocr-$id", if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, request)
    }

    suspend fun recoverOcr() { dao.unfinishedIds().forEach { enqueueOcr(it) } }

    suspend fun remove(item: ScreenshotEntity) = importMutex.withLock {
        withContext(Dispatchers.IO) {
            workManager.cancelUniqueWork("ocr-${item.id}").result.get()
            // Remove the file first so a failure keeps the recoverable database record.
            val image = file(item)
            if (image.exists() && !image.delete()) throw IOException("Could not remove this archive copy.")
            dao.delete(item)
        }
    }
}
