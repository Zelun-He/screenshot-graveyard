package com.zelun.screenshotgraveyard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zelun.screenshotgraveyard.data.ScreenshotEntity
import com.zelun.screenshotgraveyard.domain.SoulStatus
import java.io.File

@Composable
fun RitualScreen(ui: ArchiveUiState, file: (ScreenshotEntity) -> File,
    onStatus: (ScreenshotEntity, SoulStatus) -> Unit, onOpen: (ScreenshotEntity) -> Unit,
    onArchive: () -> Unit, onImport: () -> Unit) {
    val next = ui.nextReview
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(horizontal = 24.dp, vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow("A MOMENT OF DIGITAL PEACE", color = Lavender)
        Text("Keep it. Or let it go.", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
        Text("One screenshot. One small decision.", style = MaterialTheme.typography.bodySmall, color = Muted,
            modifier = Modifier.padding(top = 7.dp, bottom = 22.dp))
        if (next != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("OLDEST SOUL FIRST", Modifier.weight(1f))
                Text("${ui.pending} left to sort", color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(14.dp))
            AnimatedContent(targetState = next.id, label = "next screenshot") { id ->
                val item = ui.all.firstOrNull { it.id == id } ?: next
                var drag by remember(id) { mutableFloatStateOf(0f) }
                val threshold = with(LocalDensity.current) { 100.dp.toPx() }
                val verdict = when { drag > threshold / 2 -> "A KEEPER"; drag < -threshold / 2 -> "REST IN PEACE"; else -> null }
                Card(onClick = { onOpen(item) },
                    modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = drag; rotationZ = drag / 50f }
                        .pointerInput(id) {
                            detectHorizontalDragGestures(onDragCancel = { drag = 0f }, onDragEnd = {
                                when {
                                    drag > threshold -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onStatus(item, SoulStatus.KEPT) }
                                    drag < -threshold -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onStatus(item, SoulStatus.BURIED) }
                                }
                                drag = 0f
                            }) { change, amount -> change.consume(); drag += amount }
                        },
                    shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 310.dp).aspectRatio(1.05f).background(Elevated)) {
                        AsyncImage(file(item), "Screenshot being reviewed: ${item.title}", contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
                        if (verdict != null) Surface(Modifier.align(Alignment.Center), color = Ink.copy(alpha = .92f),
                            shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(2.dp, if (drag > 0) Lime else Lavender)) {
                            Text(verdict, color = if (drag > 0) Lime else Lavender, fontSize = 20.sp,
                                modifier = Modifier.padding(20.dp))
                        }
                        StatusPill(item.status, Modifier.align(Alignment.TopStart).padding(14.dp))
                    }
                    Column(Modifier.padding(18.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                        Text(if (item.tags.isBlank()) ocrLabel(item) else item.tags.split(", ").joinToString("  ") { "#$it" },
                            fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 7.dp), maxLines = 2)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onStatus(next, SoulStatus.BURIED) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                    TinyGhost(Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text("Bury", color = Lavender)
                }
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onStatus(next, SoulStatus.KEPT) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) {
                    Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text("Keep")
                }
            }
            Text("Swipe left to bury · right to keep", style = MaterialTheme.typography.bodySmall, color = Muted,
                modifier = Modifier.padding(top = 17.dp))
            Text("Buried screenshots can always be revived.", fontSize = 10.sp, color = Muted,
                modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
        } else {
            GhostArt(Modifier.size(200.dp).padding(top = 10.dp), tint = Lime)
            Text(if (ui.all.isEmpty()) "No souls to sort. Yet." else "A beautifully quiet graveyard.",
                style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(if (ui.all.isEmpty()) "Import a few screenshots to start your first ritual." else "Everything has a place. Come back when life leaves you more screenshots.",
                style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 25.dp))
            Button(onClick = if (ui.all.isEmpty()) onImport else onArchive, shape = CircleShape) {
                Text(if (ui.all.isEmpty()) "Import screenshots" else "Visit the archive")
            }
        }
    }
}
