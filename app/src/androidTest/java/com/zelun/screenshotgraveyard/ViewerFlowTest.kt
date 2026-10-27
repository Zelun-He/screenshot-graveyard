package com.zelun.screenshotgraveyard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zelun.screenshotgraveyard.data.ScreenshotEntity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ViewerFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun withScreenshot(block: (ScreenshotEntity) -> Unit) {
        val app = ApplicationProvider.getApplicationContext<GraveyardApplication>()
        val repo = app.repository
        val image = File(repo.directory, "viewer-${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(800, 2400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(240, 234, 219))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(38, 32, 48); textSize = 38f }
        canvas.drawText("THE THINGS WORTH KEEPING", 36f, 90f, paint)
        paint.textSize = 27f
        val lines = listOf("A little note to future me.", "You can zoom into this screenshot.", "Needle: a word inside the image.", "Small moments are worth noticing.", "A new recipe. A useful shortcut.", "A thought you wanted to remember.")
        (0 until 24).forEach { row -> canvas.drawText(lines[row % lines.size], 36f, 180f + row * 85f, paint) }
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val id = runBlocking { repo.dao.insert(ScreenshotEntity(imageName = image.name, sha256 = image.name,
            title = "A long screenshot", extractedText = "Needle is inside this image", ocrState = "READY", bytes = image.length())) }
        val item = runBlocking { repo.dao.get(id)!! }
        try { block(item) } finally { runBlocking { repo.remove(item) } }
    }

    @Test fun fullScreenViewerSupportsZoomFitAndReturnToDetails() = withScreenshot { item ->
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("screenshot-${item.id}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("screenshot-${item.id}").performScrollTo().performClick()
        compose.onNodeWithTag("open-fullscreen").performScrollTo().performClick()
        val image = compose.onNodeWithTag("image-viewport")
        compose.waitUntil(30_000) {
            runCatching { image.fetchSemanticsNode().config[SemanticsProperties.StateDescription] }.getOrNull() == "Zoom 100 percent"
        }
        compose.onNodeWithContentDescription("Zoom in").performClick()
        image.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Zoom 150 percent"))
        compose.onNodeWithContentDescription("Zoom out").performClick()
        image.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Zoom 100 percent"))
        image.performTouchInput {
            val distance = center.x * .2f
            down(0, Offset(center.x - distance, center.y))
            down(1, Offset(center.x + distance, center.y))
            moveTo(0, Offset(center.x - distance * 2f, center.y))
            moveTo(1, Offset(center.x + distance * 2f, center.y))
            up(0); up(1)
        }
        image.assert(SemanticsMatcher("A pinch increases the zoom level") {
            it.config[SemanticsProperties.StateDescription].removePrefix("Zoom ").removeSuffix(" percent").toInt() > 100
        })
        compose.onNodeWithText("Fit", useUnmergedTree = true).performClick()
        image.performTouchInput { doubleClick(center) }
        image.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Zoom 300 percent"))
        image.performTouchInput { swipeUp(durationMillis = 350) }
        image.performTouchInput { swipeRight(durationMillis = 350) }
        capture("viewer.png")
        compose.onNodeWithText("Fit", useUnmergedTree = true).performClick()
        image.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Zoom 100 percent"))
        compose.onNodeWithContentDescription("Close full-screen viewer").performClick()
        compose.onNodeWithTag("image-viewport").assertDoesNotExist()
        compose.onNodeWithText("Extracted text", useUnmergedTree = true).assertExists()
    }

    @Test fun searchResultsStayVisibleWithoutTheHero() = withScreenshot { item ->
        compose.onNodeWithTag("archive-search").performTextClearance()
        compose.onNodeWithTag("archive-hero").assertExists()
        compose.onNodeWithTag("archive-search").performTextInput("needle")
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("screenshot-${item.id}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("archive-hero").assertDoesNotExist()
        compose.onNodeWithTag("screenshot-${item.id}").assertIsDisplayed()
        capture("search-compact.png")
        compose.onNodeWithTag("archive-search").performTextClearance()
        compose.onNodeWithTag("archive-search").performImeAction()
        compose.onNodeWithTag("archive-hero").assertExists()
    }

    private fun capture(name: String) {
        val app = ApplicationProvider.getApplicationContext<GraveyardApplication>()
        val directory = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
