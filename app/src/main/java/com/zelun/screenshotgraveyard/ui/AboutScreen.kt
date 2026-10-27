package com.zelun.screenshotgraveyard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelun.screenshotgraveyard.BuildConfig
import com.zelun.screenshotgraveyard.domain.ArchiveRules

@Composable
fun AboutScreen(ui: ArchiveUiState, onDemo: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(horizontal = 24.dp, vertical = 15.dp)) {
        Eyebrow("FIELD NOTES", color = Lavender)
        Text("For all the things\nyou meant to revisit.", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 15.dp, bottom = 16.dp))
        Text("Recipes. Rabbit holes. That one line of code. A little home for your digital unfinished business.",
            color = Muted, style = MaterialTheme.typography.bodyMedium)
        GhostArt(Modifier.align(Alignment.CenterHorizontally).size(150.dp).padding(vertical = 10.dp))
        Note("01", "Welcome a few souls", "Import up to 20 images at a time, or share images here from another app. Exact duplicates are skipped.")
        Note("02", "Find the words you forgot", "Text recognition runs on this device. Search titles, tags, or words inside a screenshot. Search matches word beginnings; every search word must match.")
        Note("03", "Give “later” a little closure", "Keep the useful ones. Bury the finished ones. Revive a buried screenshot whenever you need it.")
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().background(Elevated, RoundedCornerShape(18.dp))
            .border(1.dp, Line, RoundedCornerShape(18.dp)).padding(18.dp)) {
            Eyebrow("A PRIVATE LITTLE ARCHIVE", color = Lime)
            Text("Images and extracted text stay in app storage. No account or server is needed. Device backups are disabled for this archive.",
                style = MaterialTheme.typography.bodySmall, color = Cream, modifier = Modifier.padding(top = 9.dp))
            Text("Importing stores a separate copy. Burying changes its status; it does not free storage. Removing an archive copy frees its space here and leaves your gallery original alone. Uninstalling clears the archive.",
                style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 10.dp))
            HorizontalDivider(Modifier.padding(vertical = 15.dp), color = Line)
            Text("${ui.all.size} screenshots · ${ArchiveRules.formatBytes(ui.all.sumOf { it.bytes })} in archive copies",
                fontSize = 12.sp, color = Lavender)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onImport, enabled = ui.importProgress == null, shape = CircleShape, modifier = Modifier.fillMaxWidth()) { Text("Import screenshots") }
        OutlinedButton(onClick = onDemo, enabled = ui.importProgress == null, shape = CircleShape, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Explore sample screenshots") }
        Text("SCREENSHOT GRAVEYARD  /  ${BuildConfig.VERSION_NAME}\nMade for a calmer camera roll.",
            color = Muted, fontSize = 10.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 25.dp, bottom = 20.dp).align(Alignment.CenterHorizontally),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun Note(number: String, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(number, fontFamily = Serif, fontSize = 23.sp, color = Lavender.copy(alpha = .7f))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 5.dp))
        }
    }
    HorizontalDivider(color = Line.copy(alpha = .6f))
}
