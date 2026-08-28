package com.lumenscriptura

import kotlinx.serialization.Serializable

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
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class HistoryItem(
    val bookName: String,
    val chapter: Int,
    val timestamp: Long = System.currentTimeMillis()
)
