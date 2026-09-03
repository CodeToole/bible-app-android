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
data class Highlight(
    val bookName: String,
    val chapter: Int,
    val verseNum: Int,
    val colorHex: String,
    val timestamp: Long = 0L
)

@Serializable
data class HistoryItem(
    val bookName: String,
    val chapter: Int,
    val timestamp: Long = 0L
)

@Serializable
data class SavedStudyNote(
    val id: String = generateUniqueId(),
    val inputReference: String,
    val blocks: List<ScripturePassageBlock>,
    val timestamp: Long = 0L
)

private fun generateUniqueId(): String {
    return "note_${Random.nextLong(1000000000L, 9999999999L)}"
}
