package com.zelun.screenshotgraveyard.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zelun.screenshotgraveyard.data.ImageFiles
import com.zelun.screenshotgraveyard.data.ScreenshotEntity
import com.zelun.screenshotgraveyard.domain.AppSection
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraveyardApp(viewModel: ArchiveViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val snackbar = remember { SnackbarHostState() }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { viewModel.import(it) }
    val import = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    LaunchedEffect(ui.section) { focus.clearFocus() }
    LaunchedEffect(ui.importProgress != null) {
        if (ui.importProgress != null) { focus.clearFocus(); detailId = null }
    }
    LaunchedEffect(viewModel) {
        viewModel.notices.collectLatest { notice ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(notice.message, actionLabel = if (notice.undo != null) "Undo" else null,
                withDismissAction = true, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) notice.undo?.let(viewModel::undo)
        }
    }
    Scaffold(containerColor = Ink, contentWindowInsets = WindowInsets(0),
        topBar = { BrandBar(ui.importProgress != null, import) },
        bottomBar = { BottomBar(ui.section, viewModel::section) },
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it, containerColor = Elevated, contentColor = Cream, actionColor = Lime) } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (ui.importProgress != null) {
                Row(Modifier.fillMaxWidth().background(Elevated).padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Lavender)
                    Text(ui.importProgress!!, style = MaterialTheme.typography.bodySmall, color = Lavender)
                }
            }
            when (ui.section) {
                AppSection.ARCHIVE -> ArchiveScreen(ui, viewModel.repository::file, viewModel::query, viewModel::filter,
                    import, viewModel::demo, { focus.clearFocus(); detailId = it.id }, { viewModel.section(AppSection.RITUAL) })
                AppSection.RITUAL -> RitualScreen(ui, viewModel.repository::file, viewModel::status,
                    { focus.clearFocus(); detailId = it.id }, { viewModel.section(AppSection.ARCHIVE) }, import)
                AppSection.ABOUT -> AboutScreen(ui, viewModel::demo, import)
            }
        }
    }
    ui.all.firstOrNull { it.id == detailId }?.let { item ->
        DetailSheet(item, viewModel.repository.file(item), onDismiss = { detailId = null },
            onStatus = { viewModel.status(item, it) }, onEdit = { title, tags -> viewModel.edit(item, title, tags) },
            onRetry = { viewModel.retry(item) }, onDelete = { viewModel.remove(item); detailId = null },
            onShare = {
                try {
                    val file = viewModel.repository.file(item)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = ImageFiles.mimeType(file)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = android.content.ClipData.newRawUri("Screenshot", uri)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share screenshot"))
                } catch (_: Exception) { viewModel.message("Couldn't open sharing. Please try again.") }
            })
    }
}

@Composable
private fun BrandBar(importing: Boolean, onImport: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(start = 22.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Elevated), contentAlignment = Alignment.Center) {
            TinyGhost(Modifier.size(25.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("screenshot", fontSize = 12.sp, color = Muted, letterSpacing = 1.sp)
            Text("graveyard", fontSize = 22.sp, fontFamily = Serif, color = Cream, lineHeight = 23.sp)
        }
        FilledTonalButton(onClick = onImport, enabled = !importing, shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Lavender, contentColor = Ink)) {
            Icon(Icons.Outlined.Add, null, Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text("Import", fontSize = 12.sp)
        }
    }
}

@Composable
private fun BottomBar(section: AppSection, onSection: (AppSection) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Ink)) {
        HorizontalDivider(color = Line.copy(alpha = .7f))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            AppSection.entries.forEach { tab ->
                val selected = section == tab
                val color = if (selected) Lavender else Muted
                TextButton(onClick = { onSection(tab) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.textButtonColors(containerColor = if (selected) Elevated else androidx.compose.ui.graphics.Color.Transparent, contentColor = color)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        when (tab) {
                            AppSection.ARCHIVE -> TinyGhost(Modifier.size(20.dp), color)
                            AppSection.RITUAL -> Icon(Icons.Outlined.Refresh, null, Modifier.size(20.dp))
                            AppSection.ABOUT -> Icon(Icons.Outlined.Info, null, Modifier.size(20.dp))
                        }
                        Text(when (tab) { AppSection.ARCHIVE -> "Archive"; AppSection.RITUAL -> "The ritual"; AppSection.ABOUT -> "Field notes" }, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
