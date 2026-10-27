package com.zelun.screenshotgraveyard.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.zelun.screenshotgraveyard.GraveyardApplication
import com.zelun.screenshotgraveyard.data.ImageFiles
import com.zelun.screenshotgraveyard.domain.ArchiveRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class OcrWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    companion object {
        private val recognitionGate = Semaphore(1)
        private val sharedRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    }

    // A batch can schedule 20 jobs. Keep only one decoded bitmap/model active at a time.
    override suspend fun doWork(): Result = recognitionGate.withPermit { withContext(Dispatchers.IO) {
        val repository = (applicationContext as GraveyardApplication).repository
        val dao = repository.dao
        val id = inputData.getLong("screenshotId", -1)
        val original = dao.get(id) ?: return@withContext Result.success()
        if (original.ocrState == "READY") return@withContext Result.success()
        dao.setOcrState(id, "PROCESSING")
        val recognizer = sharedRecognizer
        var bitmap: android.graphics.Bitmap? = null
        try {
            bitmap = ImageFiles.decode(repository.file(original))
            // ML Kit tasks are not cancellable. Finish reading before recycling their bitmap.
            val result = withContext(NonCancellable) { recognizer.process(InputImage.fromBitmap(bitmap, 0)).await() }
            // Conditional SQL preserves edits even when the user saves at this exact moment.
            dao.completeOcr(id, result.text.take(100_000), original.title,
                ArchiveRules.suggestedTitle(result.text, original.title), ArchiveRules.suggestedTags(result.text))
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (runAttemptCount < 2) { dao.setOcrState(id, "QUEUED"); Result.retry() }
            else { dao.setOcrState(id, "FAILED"); Result.failure() }
        } finally { bitmap?.recycle() }
    } }
}
