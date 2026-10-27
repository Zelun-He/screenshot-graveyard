package com.zelun.screenshotgraveyard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zelun.screenshotgraveyard.data.ScreenshotEntity
import com.zelun.screenshotgraveyard.domain.ArchiveFilter
import java.io.File

@Composable
fun ArchiveScreen(ui: ArchiveUiState, file: (ScreenshotEntity) -> File,
    onQuery: (String) -> Unit, onFilter: (ArchiveFilter) -> Unit,
    onImport: () -> Unit, onDemo: () -> Unit, onOpen: (ScreenshotEntity) -> Unit, onRitual: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val searchMode = ui.query.isNotBlank()
    LaunchedEffect(searchMode) { gridState.scrollToItem(0) }
    LazyVerticalGrid(columns = GridCells.Adaptive(150.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!searchMode) item(key = "hero", span = { GridItemSpan(maxLineSpan) }) { ArchiveHero(ui, onRitual) }
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(ui.query, onQuery, Modifier.fillMaxWidth().testTag("archive-search"), singleLine = true,
                placeholder = { Text("Search the words you forgot…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp)) },
                trailingIcon = if (ui.query.isNotEmpty()) {{ IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, "Clear search") } }} else null,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Line, focusedBorderColor = Lavender,
                    unfocusedContainerColor = Surface, focusedContainerColor = Surface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }))
        }
        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ArchiveFilter.entries) { filter ->
                    FilterChip(selected = ui.filter == filter, onClick = { onFilter(filter) },
                        label = { Text(filter.label, fontSize = 12.sp) }, shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Lavender, selectedLabelColor = Ink,
                            containerColor = Color.Transparent, labelColor = Muted),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = ui.filter == filter,
                            borderColor = Line, selectedBorderColor = Lavender))
                }
            }
        }
        item(key = "collection-title", span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(if (ui.query.isBlank()) "THE COLLECTION" else "BACK FROM THE DEPTHS", Modifier.weight(1f))
                Text("${ui.visible.size} ${if (ui.visible.size == 1) "soul" else "souls"}", fontSize = 11.sp, color = Muted)
            }
        }
        if (ui.indexing > 0) item(key = "reading-progress", span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.dp, color = Lavender)
                Text("Reading ${ui.indexing} screenshot${if (ui.indexing == 1) "" else "s"}. Search updates as text arrives.",
                    style = MaterialTheme.typography.bodySmall, color = Lavender)
            }
        }
        when {
            !ui.loaded || ui.searching -> item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Lavender) }
            }
            ui.all.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyArchive(ui.importProgress != null, onImport, onDemo)
            }
            ui.visible.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GhostArt(Modifier.size(95.dp))
                    Text(if (ui.query.isNotBlank()) "No ghosts by that name." else "Quiet in this corner.", style = MaterialTheme.typography.titleLarge)
                    Text(if (ui.query.isNotBlank()) "Try a title, tag, or word inside a screenshot." else "Screenshots will appear here as you sort them.",
                        style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 8.dp))
                    TextButton(onClick = { onQuery(""); onFilter(ArchiveFilter.ALL) }) { Text("Back to all souls") }
                }
            }
            else -> items(ui.visible, key = { it.id }, contentType = { "screenshot" }) { item ->
                SoulCard(item, file(item), onClick = { onOpen(item) }, Modifier.animateItem())
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(4.dp).background(Lime, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("On your device. At your own pace.", fontSize = 10.sp, color = Muted)
            }
        }
    }
}

@Composable
private fun ArchiveHero(ui: ArchiveUiState, onRitual: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("archive-hero").clip(RoundedCornerShape(23.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF30253E), Color(0xFF221D2B))))
        .border(1.dp, Color(0xFF493954), RoundedCornerShape(23.dp)).padding(18.dp)) {
        Eyebrow("A LITTLE DIGITAL AFTERLIFE", color = Lavender)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Saved for later.\nLost until now.", fontFamily = Serif, fontSize = 25.sp, lineHeight = 29.sp, color = Cream)
                Text("Find the good stuff. Let the rest rest.", fontSize = 11.sp, lineHeight = 16.sp, color = Muted,
                    modifier = Modifier.padding(top = 6.dp))
            }
            GhostArt(Modifier.width(72.dp).height(88.dp))
        }
        HorizontalDivider(Modifier.padding(top = 14.dp, bottom = 10.dp), color = Lavender.copy(alpha = .14f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(Triple(ui.pending, "UNSORTED", Lavender), Triple(ui.kept, "KEPT CLOSE", Lime), Triple(ui.buried, "AT REST", Muted)).forEach { (count, label, color) ->
                Column {
                    Text(count.toString().padStart(2, '0'), fontFamily = Serif, fontSize = 26.sp, color = color)
                    Text(label, fontSize = 8.sp, letterSpacing = 1.sp, color = Muted)
                }
            }
            if (ui.pending > 0) {
                IconButton(onClick = onRitual, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Start sorting ritual", Modifier.size(20.dp), tint = Lavender)
                }
            }
        }
    }
}

@Composable
private fun EmptyArchive(importing: Boolean, onImport: () -> Unit, onDemo: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Your screenshots deserve\na second life.", style = MaterialTheme.typography.titleLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("Import a few. We'll read the text so you can find them when “later” finally arrives.",
            style = MaterialTheme.typography.bodySmall, color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 10.dp, bottom = 18.dp))
        Button(onClick = onImport, enabled = !importing, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text("Welcome your first screenshots", fontSize = 12.sp)
        }
        TextButton(onClick = onDemo, enabled = !importing) { Text("Or explore a sample graveyard", fontSize = 12.sp, color = Muted) }
    }
}

@Composable
fun SoulCard(item: ScreenshotEntity, file: File, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().testTag("screenshot-${item.id}"),
        shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line.copy(alpha = .7f))) {
        Box(Modifier.fillMaxWidth().aspectRatio(.98f).clip(RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp))) {
            AsyncImage(model = file, contentDescription = null, contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .10f), Color.Transparent, Ink.copy(alpha = .65f)))))
            StatusPill(item.status, Modifier.align(Alignment.TopStart).padding(9.dp))
            if (item.isDemo) Text("SAMPLE", color = Cream.copy(alpha = .8f), fontSize = 8.sp, letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
            if (item.ocrState == "QUEUED" || item.ocrState == "PROCESSING") {
                LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter), color = Lavender, trackColor = Line)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = Cream, maxLines = 2,
                overflow = TextOverflow.Ellipsis, minLines = 2)
            Spacer(Modifier.height(8.dp))
            val footer = when {
                item.ocrState == "FAILED" -> "Tap to retry reading"
                item.tags.isNotBlank() -> "#${item.tags.substringBefore(',').trim()}"
                item.ocrState != "READY" -> "Reading…"
                else -> "A little uncategorized soul"
            }
            Text(footer, fontSize = 10.sp, color = if (item.ocrState == "FAILED") Peach else Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
