package com.zelun.screenshotgraveyard.domain

import org.junit.Assert.*
import org.junit.Test

class ImageViewportTest {
    private val square = ImageViewportBounds.fit(100f, 100f, 500f, 500f)

    @Test fun portraitFitPreservesAspectRatio() {
        val bounds = ImageViewportBounds.fit(300f, 600f, 1000f, 4000f)
        assertEquals(150f, bounds.fittedWidth, .001f)
        assertEquals(600f, bounds.fittedHeight, .001f)
    }

    @Test fun aFittedImageCannotBeDraggedAway() {
        val moved = ImageViewportMath.transform(ImageViewport(), square, 1f, panX = 200f, panY = -200f)
        assertEquals(0f, moved.x, .001f)
        assertEquals(0f, moved.y, .001f)
    }

    @Test fun zoomKeepsThePointUnderTheFinger() {
        val value = ImageViewportMath.transform(ImageViewport(), square, 2f, centroidX = 25f, centroidY = 25f)
        assertEquals(2f, value.scale, .001f)
        assertEquals(25f, value.x, .001f)
        assertEquals(25f, value.y, .001f)
        assertEquals(25f, 50f + value.x + value.scale * -25f, .001f)
    }

    @Test fun dragUsesScreenPixelsAtEveryZoomLevel() {
        val value = ImageViewportMath.transform(ImageViewport(3f), square, 1f, panX = 10f, panY = -12f)
        assertEquals(10f, value.x, .001f)
        assertEquals(-12f, value.y, .001f)
    }

    @Test fun aLongScreenshotStaysWithinItsEdges() {
        val bounds = ImageViewportBounds.fit(300f, 600f, 1000f, 4000f)
        val value = ImageViewportMath.clamp(ImageViewport(4f, 9999f, -9999f), bounds)
        assertEquals(150f, value.x, .001f)
        assertEquals(-900f, value.y, .001f)
    }

    @Test fun narrowImagesStayHorizontallyCenteredUntilTheyFillTheView() {
        val bounds = ImageViewportBounds.fit(300f, 600f, 100f, 4000f)
        assertEquals(0f, ImageViewportMath.clamp(ImageViewport(8f, 100f, 200f), bounds).x, .001f)
    }

    @Test fun zoomLimitsAndFitAreEnforced() {
        assertEquals(8f, ImageViewportMath.transform(ImageViewport(4f), square, 10f).scale, .001f)
        val fitted = ImageViewportMath.transform(ImageViewport(3f, 50f, 50f), square, .01f)
        assertEquals(1f, fitted.scale, .001f)
        assertEquals(0f, fitted.x, .001f)
        assertEquals(0f, fitted.y, .001f)
    }

    @Test fun doubleTapZoomsThenResets() {
        val zoomed = ImageViewportMath.doubleTap(ImageViewport(), square, 50f, 50f)
        assertEquals(3f, zoomed.scale, .001f)
        assertEquals(ImageViewport(), ImageViewportMath.doubleTap(zoomed, square, 10f, 10f))
    }

    @Test fun rotatingReclampsAnExistingPan() {
        val landscape = ImageViewportBounds.fit(600f, 300f, 1000f, 4000f)
        val value = ImageViewportMath.clamp(ImageViewport(2f, 80f, 900f), landscape)
        assertEquals(0f, value.x, .001f)
        assertEquals(150f, value.y, .001f)
    }

    @Test fun invalidGesturesCannotCreateAnInvalidTransform() {
        assertEquals(ImageViewport(), ImageViewportMath.transform(ImageViewport(), square, Float.NaN))
        assertEquals(ImageViewport(), ImageViewportMath.transform(ImageViewport(), square, -1f))
        val value = ImageViewportMath.clamp(ImageViewport(Float.POSITIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY), square)
        assertEquals(1f, value.scale, .001f)
        assertTrue(value.x.isFinite() && value.y.isFinite())
    }
}
