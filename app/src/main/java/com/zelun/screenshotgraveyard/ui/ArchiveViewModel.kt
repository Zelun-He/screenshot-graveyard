package com.zelun.screenshotgraveyard.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zelun.screenshotgraveyard.GraveyardApplication
import com.zelun.screenshotgraveyard.data.ScreenshotEntity
import com.zelun.screenshotgraveyard.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class UndoAction(val id: Long, val expected: String, val restore: String)
data class Notice(val message: String, val undo: UndoAction? = null)
private data class SearchResult(val query: String, val ids: Set<Long>?)
private data class Controls(val query: String, val filter: ArchiveFilter, val section: AppSection, val progress: String?)

data class ArchiveUiState(
    val all: List<ScreenshotEntity> = emptyList(),
    val visible: List<ScreenshotEntity> = emptyList(),
    val query: String = "",
    val filter: ArchiveFilter = ArchiveFilter.ALL,
    val section: AppSection = AppSection.ARCHIVE,
    val importProgress: String? = null,
    val searching: Boolean = false,
    val loaded: Boolean = false,
) {
    val pending get() = all.count { it.status == SoulStatus.PENDING.name }
    val kept get() = all.count { it.status == SoulStatus.KEPT.name }
    val buried get() = all.count { it.status == SoulStatus.BURIED.name }
    val nextReview get() = all.lastOrNull { it.status == SoulStatus.PENDING.name }
    val indexing get() = all.count { it.ocrState == "QUEUED" || it.ocrState == "PROCESSING" }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ArchiveViewModel(application: Application) : AndroidViewModel(application) {
    val repository = (application as GraveyardApplication).repository
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ArchiveFilter.ALL)
    private val section = MutableStateFlow(AppSection.ARCHIVE)
    private val progress = MutableStateFlow<String?>(null)
    private val noticesChannel = Channel<Notice>(Channel.BUFFERED)
    val notices = noticesChannel.receiveAsFlow()
    private val controls = combine(query, filter, section, progress, ::Controls)
    private val search = query.debounce(160).flatMapLatest { value ->
        val expression = ArchiveRules.ftsExpression(value)
        when {
            value.isBlank() -> flowOf(SearchResult(value, null))
            expression == null -> flowOf(SearchResult(value, emptySet()))
            else -> repository.dao.searchIds(expression).map { SearchResult(value, it.toSet()) }
        }
    }
    val ui = combine(repository.dao.observeAll(), controls, search) { all, inputs, matches ->
        val searching = matches.query != inputs.query
        ArchiveUiState(all = all, visible = all.filter { item ->
            val inCategory = when (inputs.filter) {
                ArchiveFilter.ALL -> true
                else -> item.status == inputs.filter.name
            }
            inCategory && (inputs.query.isBlank() || (!searching && matches.ids?.contains(item.id) == true))
        }, query = inputs.query, filter = inputs.filter, section = inputs.section,
            importProgress = inputs.progress, searching = searching, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveUiState())

    fun query(value: String) { query.value = value.take(500) }
    fun filter(value: ArchiveFilter) { filter.value = value }
    fun section(value: AppSection) { section.value = value }
    fun message(value: String) { viewModelScope.launch { noticesChannel.send(Notice(value)) } }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (progress.value != null) { message("An import is already in progress."); return }
        progress.value = "Welcoming screenshots…"
        viewModelScope.launch {
            try {
                val result = repository.importUris(uris) { done, total -> progress.value = "Importing $done of $total…" }
                section.value = AppSection.ARCHIVE
                filter.value = ArchiveFilter.ALL
                query.value = ""
                val parts = buildList {
                    if (result.imported > 0) add("${result.imported} screenshot${if (result.imported == 1) "" else "s"} welcomed")
                    if (result.duplicates > 0) add("${result.duplicates} already here")
                    if (result.failed > 0) add("${result.failed} couldn't be imported (use readable images under 25 MB)")
                    if (uris.distinct().size > ArchiveRules.MAX_IMPORT_BATCH) add("Only the first 20 were processed; import the rest next")
                }
                noticesChannel.send(Notice(parts.joinToString(" · ").ifBlank { "Nothing to import." }))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { noticesChannel.send(Notice("Import interrupted. Your existing screenshots are safe.")) }
            finally { progress.value = null }
        }
    }

    fun demo() {
        if (progress.value != null) return
        progress.value = "Preparing a few friendly ghosts…"
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.loadDemo() }
                noticesChannel.send(Notice(if (result.imported > 0) "${result.imported} sample screenshots added. Try searching ‘ramen’ once recognition finishes." else "The sample screenshots are already here."))
                section.value = AppSection.ARCHIVE
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { noticesChannel.send(Notice("Couldn't load the sample archive. Please try again.")) }
            finally { progress.value = null }
        }
    }

    fun status(item: ScreenshotEntity, next: SoulStatus) {
        if (item.status == next.name) return
        viewModelScope.launch {
            try {
                if (repository.dao.transition(item.id, item.status, next.name) > 0) {
                    val message = when (next) {
                        SoulStatus.KEPT -> "A keeper. Saved for another day."
                        SoulStatus.BURIED -> "Laid to rest. You can revive it anytime."
                        SoulStatus.PENDING -> "Revived and ready for another look."
                    }
                    noticesChannel.send(Notice(message, UndoAction(item.id, next.name, item.status)))
                }
            } catch (_: Exception) { noticesChannel.send(Notice("Couldn't update this screenshot. Please try again.")) }
        }
    }

    fun undo(action: UndoAction) { viewModelScope.launch { repository.dao.transition(action.id, action.expected, action.restore) } }
    fun edit(item: ScreenshotEntity, title: String, tags: String) {
        viewModelScope.launch {
            try {
                repository.dao.edit(item.id, title.trim().ifBlank { item.title }.take(100), ArchiveRules.normalizeTags(tags))
                noticesChannel.send(Notice("Details saved."))
            } catch (_: Exception) { noticesChannel.send(Notice("Couldn't save these details. Please try again.")) }
        }
    }
    fun retry(item: ScreenshotEntity) {
        viewModelScope.launch {
            repository.dao.setOcrState(item.id, "QUEUED")
            repository.enqueueOcr(item.id, replace = true)
        }
    }
    fun remove(item: ScreenshotEntity) {
        viewModelScope.launch {
            try { repository.remove(item); noticesChannel.send(Notice("Archive copy removed.")) }
            catch (_: Exception) { noticesChannel.send(Notice("Couldn't remove this copy. Please try again.")) }
        }
    }
}
