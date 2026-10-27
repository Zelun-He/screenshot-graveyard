package com.zelun.screenshotgraveyard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zelun.screenshotgraveyard.data.ScreenshotEntity
import com.zelun.screenshotgraveyard.domain.ArchiveRules
import com.zelun.screenshotgraveyard.domain.SoulStatus
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(item: ScreenshotEntity, file: File, onDismiss: () -> Unit,
    onStatus: (SoulStatus) -> Unit, onEdit: (String, String) -> Unit,
    onRetry: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    var showText by remember(item.id) { mutableStateOf(false) }
    var editing by remember(item.id) { mutableStateOf(false) }
    var deleting by remember(item.id) { mutableStateOf(false) }
    var viewerOpen by rememberSaveable(item.id) { mutableStateOf(false) }
    var title by remember(item.id, editing) { mutableStateOf(item.title) }
    var tags by remember(item.id, editing) { mutableStateOf(item.tags) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink, contentColor = Cream, dragHandle = { BottomSheetDefaults.DragHandle(color = Line) }) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleLarge)
                    Text("${dateLabel(item.createdAt)} · ${ArchiveRules.formatBytes(item.bytes)}${if (item.isDemo) " · Sample" else ""}",
                        fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 6.dp))
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close screenshot") }
            }
            Row(Modifier.padding(start = 22.dp, top = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!showText, { showText = false }, label = { Text("Screenshot") }, shape = CircleShape)
                FilterChip(showText, { showText = true }, label = { Text("Extracted text") }, shape = CircleShape)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
                if (!showText) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp)
                        .background(Surface, RoundedCornerShape(16.dp)).testTag("open-fullscreen")
                        .clickable(role = Role.Button, onClickLabel = "Open full-screen screenshot") { viewerOpen = true }, contentAlignment = Alignment.Center) {
                        AsyncImage(file, item.title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp))
                    }
                    TextButton(onClick = { viewerOpen = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Open full screen") }
                } else {
                    Surface(shape = RoundedCornerShape(16.dp), color = Surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            SelectionContainer {
                                Text(item.extractedText.ifBlank { ocrLabel(item) }, style = MaterialTheme.typography.bodyMedium,
                                    color = if (item.extractedText.isBlank()) Muted else Cream)
                            }
                            if (item.extractedText.isNotBlank()) TextButton(onClick = { scope.launch {
                                clipboard.setClipEntry(ClipEntry(android.content.ClipData.newPlainText("Screenshot text", item.extractedText)))
                            } }) {
                                Text("Copy text")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(item.status)
                    Spacer(Modifier.width(10.dp))
                    Text(ocrLabel(item), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                }
                if (item.ocrState == "FAILED") TextButton(onClick = onRetry) { Text("Try reading the text again") }
                if (item.tags.isNotBlank()) Text(item.tags.split(", ").joinToString("   ") { "#$it" },
                    fontSize = 12.sp, color = Lavender, modifier = Modifier.padding(top = 16.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { editing = true }) { Text("Edit title & tags", fontSize = 12.sp) }
                    TextButton(onClick = onShare) { Icon(Icons.Outlined.Share, null, Modifier.size(15.dp)); Spacer(Modifier.width(7.dp)); Text("Share", fontSize = 12.sp) }
                }
                TextButton(onClick = { deleting = true }) { Text("Remove archive copy", fontSize = 11.sp, color = Peach) }
                Spacer(Modifier.height(12.dp))
            }
            HorizontalDivider(color = Line)
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.status == "BURIED") {
                    Button(onClick = { onStatus(SoulStatus.PENDING) }, modifier = Modifier.weight(1f), shape = CircleShape) {
                        Icon(Icons.Outlined.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Revive")
                    }
                } else {
                    OutlinedButton(onClick = { onStatus(SoulStatus.BURIED) }, modifier = Modifier.weight(1f), shape = CircleShape) { Text("Bury") }
                }
                Button(onClick = { onStatus(if (item.status == "KEPT") SoulStatus.PENDING else SoulStatus.KEPT) },
                    modifier = Modifier.weight(1f), shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) {
                    Text(if (item.status == "KEPT") "Unkeep" else "Keep")
                }
            }
        }
    }
    if (viewerOpen) ScreenshotViewer(file, item.title, onDismiss = { viewerOpen = false })
    if (editing) AlertDialog(onDismissRequest = { editing = false }, containerColor = Surface,
        title = { Text("A name worth remembering", fontFamily = Serif) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it.take(100) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            OutlinedTextField(tags, { tags = it.take(300) }, label = { Text("Tags, separated by commas") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            Text("Up to 8 tags. Words in the screenshot stay searchable too.", color = Muted, fontSize = 11.sp)
        } },
        confirmButton = { TextButton(onClick = { onEdit(title, tags); editing = false }) { Text("Save details") } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } })
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, containerColor = Surface,
        title = { Text("Let this copy go?", fontFamily = Serif) },
        text = { Text("This permanently removes the image and its searchable text from Screenshot Graveyard. Your original photo stays in your gallery.") },
        confirmButton = { TextButton(onClick = { deleting = false; onDelete() }) { Text("Remove copy", color = Peach) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Keep it here") } })
}
