package com.zelun.screenshotgraveyard.domain

import org.junit.Assert.*
import org.junit.Test

class ArchiveRulesTest {
    @Test fun emptySearchDoesNotGenerateInvalidMatch() {
        assertNull(ArchiveRules.ftsExpression(" \n "))
        assertNull(ArchiveRules.ftsExpression("\" * : () -"))
    }
    @Test fun searchIsPrefixAndAcrossAllWords() {
        assertEquals("miso* ram*", ArchiveRules.ftsExpression("miso ram"))
    }
    @Test fun ftsOperatorsAndQuotesAreTreatedAsWords() {
        assertEquals("ramen* or* title* git*",
            ArchiveRules.ftsExpression("ramen\" OR title:git*"))
    }
    @Test fun unicodeWordsArePreserved() {
        assertEquals("café* 广州*", ArchiveRules.ftsExpression("café 广州"))
    }
    @Test fun oversizedSearchIsBounded() {
        assertTrue(ArchiveRules.ftsExpression("a ".repeat(1000))!!.split(" ").size <= 12)
        assertTrue(ArchiveRules.ftsExpression("a".repeat(1000))!!.length <= 83)
    }
    @Test fun tagsAreNormalizedDeduplicatedAndBounded() {
        assertEquals("food, recipes, late night", ArchiveRules.normalizeTags("#FOOD, recipes,food\n late   night"))
        assertEquals(8, ArchiveRules.normalizeTags((1..20).joinToString(",") { "tag$it" }).split(", ").size)
        assertEquals(30, ArchiveRules.normalizeTags("a".repeat(100)).length)
    }
    @Test fun titleSkipsClockAndNoise() {
        assertEquals("Midnight miso ramen", ArchiveRules.suggestedTitle("12:45\n87%\nMidnight miso ramen\nIngredients", "image.png"))
    }
    @Test fun emptyRecognitionKeepsUsableFilename() {
        assertEquals("Screenshot 42", ArchiveRules.suggestedTitle("", "Screenshot 42.png"))
        assertEquals("Untitled soul", ArchiveRules.suggestedTitle("", ".png"))
    }
    @Test fun titleLengthIsBounded() {
        assertEquals(70, ArchiveRules.suggestedTitle("a".repeat(1000), "fallback").length)
    }
    @Test fun suggestionsAreDeterministicAndExplainable() {
        assertEquals("food", ArchiveRules.suggestedTags("Miso RAMEN recipe and ingredients"))
        assertEquals("code", ArchiveRules.suggestedTags("git stash push -m wip"))
        assertEquals("", ArchiveRules.suggestedTags("a wholly unclassified note"))
    }
    @Test fun byteLabelsDoNotPretendBurialFreesStorage() {
        assertEquals("20 B", ArchiveRules.formatBytes(20))
        assertEquals("1 KB", ArchiveRules.formatBytes(1024))
        assertEquals("1.5 MB", ArchiveRules.formatBytes(1572864))
    }
}
