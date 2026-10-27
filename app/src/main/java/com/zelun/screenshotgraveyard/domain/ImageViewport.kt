package com.zelun.screenshotgraveyard.domain

import kotlin.math.max
import kotlin.math.min

data class ImageViewport(val scale: Float = 1f, val x: Float = 0f, val y: Float = 0f)

/** Pixel-based geometry shared by touch gestures and accessible zoom buttons. */
data class ImageViewportBounds(val width: Float, val height: Float, val fittedWidth: Float, val fittedHeight: Float) {
    companion object {
        fun fit(width: Float, height: Float, imageWidth: Float, imageHeight: Float): ImageViewportBounds {
            require(listOf(width, height, imageWidth, imageHeight).all { it.isFinite() && it > 0f })
            val ratio = min(width / imageWidth, height / imageHeight)
            return ImageViewportBounds(width, height, imageWidth * ratio, imageHeight * ratio)
        }
    }
}

object ImageViewportMath {
    const val MAX_SCALE = 8f

    fun clamp(value: ImageViewport, bounds: ImageViewportBounds): ImageViewport {
        val scale = value.scale.takeIf { it.isFinite() }?.coerceIn(1f, MAX_SCALE) ?: 1f
        val maxX = max(0f, (bounds.fittedWidth * scale - bounds.width) / 2f)
        val maxY = max(0f, (bounds.fittedHeight * scale - bounds.height) / 2f)
        return ImageViewport(scale,
            value.x.takeIf { it.isFinite() }?.coerceIn(-maxX, maxX) ?: 0f,
            value.y.takeIf { it.isFinite() }?.coerceIn(-maxY, maxY) ?: 0f)
    }

    fun transform(value: ImageViewport, bounds: ImageViewportBounds, zoom: Float,
        centroidX: Float = bounds.width / 2f, centroidY: Float = bounds.height / 2f,
        panX: Float = 0f, panY: Float = 0f): ImageViewport {
        val current = clamp(value, bounds)
        if (!listOf(zoom, centroidX, centroidY, panX, panY).all { it.isFinite() } || zoom <= 0f) return current
        val nextScale = (current.scale * zoom).coerceIn(1f, MAX_SCALE)
        val ratio = nextScale / current.scale
        // Preserve the content beneath the finger, then constrain the image to the viewport.
        return clamp(ImageViewport(nextScale,
            current.x * ratio + (centroidX - bounds.width / 2f) * (1f - ratio) + panX,
            current.y * ratio + (centroidY - bounds.height / 2f) * (1f - ratio) + panY), bounds)
    }

    fun doubleTap(value: ImageViewport, bounds: ImageViewportBounds, x: Float, y: Float): ImageViewport =
        if (value.scale > 1.01f) ImageViewport() else transform(value, bounds, 3f, x, y)
}
