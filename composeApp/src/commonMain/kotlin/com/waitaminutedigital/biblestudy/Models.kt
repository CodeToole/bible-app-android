package com.waitaminutedigital.biblestudy

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Book(
    val bookNumber: Int,
    val shortName: String,
    val longName: String,
    val totalChapters: Int
)

@Serializable
data class Verse(
    val bookNumber: Int,
    val bookName: String? = null,
    val chapter: Int,
    val verseNum: Int,
    val text: String
)

@Serializable
data class BibleData(
    val books: List<Book>,
    val verses: List<Verse>
)

@Serializable
data class ScripturePassageBlock(
    val isScripture: Boolean = false,
    val referenceHeader: String = "",
    val book: String = "",
    val chapter: Int = 0,
    val startVerse: Int = 0,
    val endVerse: Int = 0,
    val verses: List<Verse> = emptyList(),
    val plainText: String = ""
)

@Serializable
data class VerseHighlight(
    val book: String,
    val chapter: Int,
    val verseNumber: Int,
    val colorHex: String,
    val timestamp: Long = 0L
)

typealias Highlight = VerseHighlight

@Serializable
data class ReadingHistory(
    val book: String,
    val chapter: Int,
    val lastReadTimestamp: Long = 0L
)

typealias HistoryItem = ReadingHistory

@Serializable
data class Bookmark(
    val book: String,
    val chapter: Int,
    val verseNumber: Int,
    val createdAt: Long = 0L
)

@Serializable
data class SavedNote(
    val id: String = generateUniqueId("note"),
    val book: String = "",
    val chapter: Int = 0,
    val verseRef: String = "",
    val title: String = "",
    val content: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val blocks: List<ScripturePassageBlock> = emptyList()
) {
    val totalVersesCount: Int
        get() = blocks.filter { it.isScripture }.sumOf { it.verses.size }
}

typealias SavedStudyNote = SavedNote

@Serializable
data class SavedQuestion(
    val id: String = generateUniqueId("q"),
    val book: String = "",
    val chapter: Int = 0,
    val verseRef: String = "",
    val questionText: String = "",
    val answerNotes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isResolved: Boolean = false
)

fun generateUniqueId(prefix: String = "item"): String {
    return "${prefix}_${Random.nextLong(1000000000L, 9999999999L)}"
}
