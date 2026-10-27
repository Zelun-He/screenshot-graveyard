package com.zelun.screenshotgraveyard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class ArchiveFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun importRecognizeSearchKeepBuryReviveAndDelete() {
        val app = ApplicationProvider.getApplicationContext<GraveyardApplication>()
        val repo = app.repository
        val source = File(repo.directory, "test-source-${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(720, 960, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 48f }
        canvas.drawText("Ramen ritual recipe", 40f, 150f, paint)
        canvas.drawText("Miso noodles and scallions", 40f, 240f, paint)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", source)
        var importedId: Long? = null
        try {
            val first = runBlocking { repo.importUris(listOf(uri)) { _, _ -> } }
            assertEquals(1, first.imported)
            assertEquals(1, runBlocking { repo.importUris(listOf(uri)) { _, _ -> } }.duplicates)
            val hash = MessageDigest.getInstance("SHA-256").digest(source.readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val item = runBlocking { repo.dao.getByHash(hash)!! }
            importedId = item.id
            val archiveUri = FileProvider.getUriForFile(app, "${app.packageName}.files", repo.file(item))
            assertEquals("image/png", app.contentResolver.getType(archiveUri))
            compose.waitUntil(120_000) { runBlocking { repo.dao.get(item.id)?.ocrState == "READY" } }
            val recognized = runBlocking { repo.dao.get(item.id)!!.extractedText.lowercase() }
            assertTrue(recognized.contains("ramen"))
            assertTrue(recognized.contains("noodles"))
            compose.onNodeWithTag("archive-search").performTextClearance()
            compose.onNodeWithTag("archive-search").performTextInput("noodl")
            compose.onNodeWithTag("archive-search").performImeAction()
            compose.waitUntil(30_000) { compose.onAllNodesWithTag("screenshot-${item.id}").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithTag("screenshot-${item.id}").performScrollTo().performClick()
            compose.onNodeWithText("Keep", useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) { runBlocking { repo.dao.get(item.id)?.status == "KEPT" } }
            compose.onNodeWithText("Bury", useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) { runBlocking { repo.dao.get(item.id)?.status == "BURIED" } }
            compose.onNodeWithText("Revive", useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) { runBlocking { repo.dao.get(item.id)?.status == "PENDING" } }
            compose.onNodeWithText("Remove archive copy", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Remove copy", useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) { runBlocking { repo.dao.get(item.id) == null } }
            assertFalse(repo.file(item).exists())
            assertTrue("Deleting an archive copy must preserve the gallery source", source.exists())
        } finally {
            source.delete()
            importedId?.let { id -> runBlocking { repo.dao.get(id)?.let { repo.remove(it) } } }
        }
    }

    @Test fun sampleTourAndScreenshots() {
        val app = ApplicationProvider.getApplicationContext<GraveyardApplication>()
        val repo = app.repository
        runBlocking { repo.loadDemo() }
        compose.waitUntil(300_000) { runBlocking { repo.dao.observeAll().first().filter { it.isDemo }.let { it.size == 6 && it.all { row -> row.ocrState == "READY" } } } }
        compose.onNodeWithTag("archive-search").performTextClearance()
        compose.waitForIdle()
        capture("archive.png")
        compose.onNodeWithText("The ritual").performClick()
        compose.waitForIdle()
        capture("ritual.png")
        compose.onNodeWithText("Archive", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("archive-search").performTextInput("ramen")
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("screenshot-1")).fetchSemanticsNodes().isNotEmpty() || runBlocking {
            repo.dao.searchIds("\"ramen*\"").first().isNotEmpty()
        } }
        compose.waitForIdle()
        capture("search.png")
    }

    private fun capture(name: String) {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
