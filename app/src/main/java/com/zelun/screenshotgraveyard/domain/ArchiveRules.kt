package com.zelun.screenshotgraveyard.domain

import java.util.Locale

enum class SoulStatus(val label: String) { PENDING("Unsorted"), KEPT("Kept"), BURIED("Buried") }
enum class OcrState { QUEUED, PROCESSING, READY, FAILED }
enum class ArchiveFilter(val label: String) { ALL("All souls"), PENDING("Unsorted"), KEPT("Kept"), BURIED("Buried") }
enum class AppSection { ARCHIVE, RITUAL, ABOUT }

object ArchiveRules {
    const val MAX_IMPORT_BYTES = 25L * 1024 * 1024
    const val MAX_IMPORT_BATCH = 20

    /** Lowercase lexical tokens cannot become FTS operators. Whitespace is portable implicit AND. */
    fun ftsExpression(query: String): String? = Regex("[\\p{L}\\p{N}]+")
        .findAll(query.take(500)).map { it.value.lowercase(Locale.ROOT).take(80) }.take(12).toList()
        .takeIf { it.isNotEmpty() }?.joinToString(" ") { "$it*" }

    fun normalizeTags(value: String): String = value.split(',', '\n', '#')
        .map { it.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").take(30) }
        .filter { it.isNotEmpty() }.distinct().take(8).joinToString(", ")

    fun suggestedTitle(text: String, fallback: String): String = text.lineSequence()
        .map { it.trim() }.firstOrNull { line ->
            line.length >= 5 && line.any { it.isLetter() } && !line.matches(Regex("[\\d\\s:%.+/-]+"))
        }?.take(70) ?: fallback.substringBeforeLast('.').ifBlank { "Untitled soul" }.take(70)

    fun suggestedTags(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val categories = linkedMapOf(
            "food" to listOf("recipe", "ramen", "ingredients", "restaurant", "tablespoon", "coffee", "cafe"),
            "code" to listOf("git ", "docker", "function", "kotlin", "npm ", "typescript", "const "),
            "travel" to listOf("flight", "boarding", "hotel", "itinerary", "departure"),
            "events" to listOf("concert", "admission", "doors open", "ticket"),
            "shopping" to listOf("order total", "subtotal", "receipt", "shipping"),
            "inspiration" to listOf("remember", "create", "possibility", "quote"),
        )
        return categories.filter { (_, words) -> words.any(lower::contains) }.keys.take(3).joinToString(", ")
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
