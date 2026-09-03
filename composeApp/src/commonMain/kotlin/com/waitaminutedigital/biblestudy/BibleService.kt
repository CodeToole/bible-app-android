package com.waitaminutedigital.biblestudy

import biblestudyap.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface IBibleService {
    suspend fun load(onSuccess: () -> Unit, onError: (String) -> Unit)
    suspend fun getBookByName(name: String): Book?
    suspend fun getAllBooks(): List<Book>
    suspend fun getChapterVerses(bookName: String, chapter: Int): List<Verse>
    suspend fun getVerseRange(bookName: String, chapter: Int, start: Int, end: Int): List<Verse>
    suspend fun getVerses(bookName: String, chapter: Int, ranges: List<NoteParserService.VerseRange>): List<Verse>
    suspend fun search(query: String): List<Verse>
    suspend fun saveHighlight(highlight: Highlight)
    suspend fun removeHighlight(bookName: String, chapter: Int, verseNum: Int)
    suspend fun getHighlights(): List<Highlight>
    suspend fun addToHistory(historyItem: HistoryItem)
    suspend fun getHistory(): List<HistoryItem>
}

class BibleService : IBibleService {
    private var bibleData: BibleData? = null
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun load(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (bibleData != null) {
            onSuccess()
            return
        }

        try {
            val content = withContext(Dispatchers.Default) {
                val bytes = Res.readBytes("files/kjv.json")
                bytes.decodeToString()
            }

            bibleData = try {
                json.decodeFromString<BibleData>(content)
            } catch (e: Exception) {
                println("Error parsing kjv.json: ${e.message}")
                onError("Failed to parse Bible data")
                null
            }

            if (bibleData != null) {
                onSuccess()
            }
        } catch (e: Exception) {
            println("Unexpected error loading Bible data: ${e.message}")
            onError("An unexpected error occurred loading Bible data")
        }
    }

    private suspend fun ensureLoaded() {
        if (bibleData == null) {
            load({}, {})
        }
    }

    override suspend fun getBookByName(name: String): Book? {
        ensureLoaded()
        return bibleData?.books?.find { 
            it.longName.equals(name, ignoreCase = true) || 
            it.shortName.equals(name, ignoreCase = true) 
        }
    }

    override suspend fun getAllBooks(): List<Book> {
        ensureLoaded()
        return bibleData?.books ?: emptyList()
    }

    override suspend fun getChapterVerses(bookName: String, chapter: Int): List<Verse> {
        ensureLoaded()
        return bibleData?.verses?.filter {
            it.bookName.equals(bookName, ignoreCase = true) &&
            it.chapter == chapter
        } ?: emptyList()
    }

    override suspend fun getVerseRange(bookName: String, chapter: Int, start: Int, end: Int): List<Verse> {
        ensureLoaded()
        return bibleData?.verses?.filter { 
            it.bookName.equals(bookName, ignoreCase = true) && 
            it.chapter == chapter && 
            it.verseNum in start..end 
        } ?: emptyList()
    }

    override suspend fun getVerses(bookName: String, chapter: Int, ranges: List<NoteParserService.VerseRange>): List<Verse> {
        ensureLoaded()
        val allVerses = bibleData?.verses ?: return emptyList()
        val result = mutableListOf<Verse>()
        val seen = mutableSetOf<Int>()
        
        for (range in ranges) {
            val rangeVerses = allVerses.filter {
                it.bookName.equals(bookName, ignoreCase = true) &&
                it.chapter == chapter &&
                it.verseNum in range.start..range.end
            }
            for (v in rangeVerses) {
                if (seen.add(v.verseNum)) {
                    result.add(v)
                }
            }
        }
        return result.sortedBy { it.verseNum }
    }

    override suspend fun search(query: String): List<Verse> {
        ensureLoaded()
        if (query.length < 3) return emptyList()
        return bibleData?.verses?.filter { 
            it.text.contains(query, ignoreCase = true) || 
            it.bookName?.contains(query, ignoreCase = true) == true 
        }?.take(50) ?: emptyList()
    }

    private val highlights = mutableListOf<Highlight>()
    private val history = mutableListOf<HistoryItem>()

    override suspend fun saveHighlight(highlight: Highlight) {
        highlights.removeAll { it.bookName == highlight.bookName && it.chapter == highlight.chapter && it.verseNum == highlight.verseNum }
        highlights.add(highlight)
    }

    override suspend fun removeHighlight(bookName: String, chapter: Int, verseNum: Int) {
        highlights.removeAll { it.bookName == bookName && it.chapter == chapter && it.verseNum == verseNum }
    }

    override suspend fun getHighlights(): List<Highlight> = highlights

    override suspend fun addToHistory(historyItem: HistoryItem) {
        history.add(0, historyItem)
        if (history.size > 100) history.removeAt(history.lastIndex)
    }

    override suspend fun getHistory(): List<HistoryItem> = history
}
