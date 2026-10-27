package com.zelun.screenshotgraveyard.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelun.screenshotgraveyard.data.ScreenshotEntity
import com.zelun.screenshotgraveyard.domain.SoulStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GhostArt(modifier: Modifier = Modifier, tint: Color = Lavender, animated: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "ghost float")
    val floating by transition.animateFloat(-3f, 3f,
        infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "floating")
    Canvas(modifier) {
        val unit = size.minDimension / 150f
        val x = size.width / 2
        val y = size.height / 2 + if (animated) floating * unit else 0f
        drawOval(tint.copy(alpha = .09f), Offset(x - 41 * unit, size.height * .85f), Size(82 * unit, 12 * unit))
        translate(x - 50 * unit, y - 57 * unit) {
            val body = Path().apply {
                moveTo(10 * unit, 104 * unit)
                lineTo(10 * unit, 48 * unit)
                cubicTo(10 * unit, -5 * unit, 90 * unit, -5 * unit, 90 * unit, 48 * unit)
                lineTo(90 * unit, 104 * unit)
                quadraticTo(78 * unit, 89 * unit, 70 * unit, 103 * unit)
                quadraticTo(59 * unit, 117 * unit, 50 * unit, 102 * unit)
                quadraticTo(40 * unit, 89 * unit, 30 * unit, 103 * unit)
                quadraticTo(19 * unit, 116 * unit, 10 * unit, 104 * unit)
                close()
            }
            drawPath(body, tint.copy(alpha = .13f))
            drawPath(body, tint, style = Stroke(1.5f * unit))
            drawOval(tint, Offset(32 * unit, 43 * unit), Size(5 * unit, 11 * unit))
            drawOval(tint, Offset(63 * unit, 43 * unit), Size(5 * unit, 11 * unit))
            drawArc(tint, 5f, 170f, false, Offset(45 * unit, 58 * unit), Size(10 * unit, 7 * unit), style = Stroke(1.2f * unit))
        }
        fun sparkle(cx: Float, cy: Float, radius: Float) {
            drawLine(tint.copy(alpha = .7f), Offset(cx - radius, cy), Offset(cx + radius, cy), 1.2f * unit)
            drawLine(tint.copy(alpha = .7f), Offset(cx, cy - radius), Offset(cx, cy + radius), 1.2f * unit)
        }
        sparkle(x - 60 * unit, y - 35 * unit, 5 * unit)
        sparkle(x + 59 * unit, y - 15 * unit, 7 * unit)
        drawCircle(tint.copy(alpha = .5f), 1.5f * unit, Offset(x + 43 * unit, y - 56 * unit))
        drawCircle(tint.copy(alpha = .4f), unit, Offset(x - 55 * unit, y + 29 * unit))
    }
}

@Composable
fun TinyGhost(modifier: Modifier = Modifier, tint: Color = Lavender) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * .17f, h * .85f); lineTo(w * .17f, h * .4f)
            cubicTo(w * .17f, -h * .02f, w * .83f, -h * .02f, w * .83f, h * .4f)
            lineTo(w * .83f, h * .85f); lineTo(w * .66f, h * .72f)
            lineTo(w * .5f, h * .87f); lineTo(w * .34f, h * .72f); close()
        }
        drawPath(path, tint)
        drawCircle(Ink, w * .045f, Offset(w * .38f, h * .39f))
        drawCircle(Ink, w * .045f, Offset(w * .62f, h * .39f))
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun StatusPill(status: String, modifier: Modifier = Modifier) {
    val color = when (status) { "KEPT" -> Lime; "BURIED" -> Muted; else -> Lavender }
    val text = SoulStatus.entries.firstOrNull { it.name == status }?.label ?: "Unsorted"
    Row(modifier.clip(CircleShape).background(Ink.copy(alpha = .87f)).border(.5.dp, color.copy(alpha = .3f), CircleShape)
        .padding(horizontal = 9.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(Modifier.size(4.dp).background(color, CircleShape))
        Text(text, fontSize = 10.sp, color = color)
    }
}

fun dateLabel(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

fun ocrLabel(item: ScreenshotEntity): String = when (item.ocrState) {
    "QUEUED" -> "Waiting to read…"
    "PROCESSING" -> "Reading the fine print…"
    "FAILED" -> "Text recognition needs another try"
    else -> if (item.extractedText.isBlank()) "No readable text found" else "Text ready to search"
}
