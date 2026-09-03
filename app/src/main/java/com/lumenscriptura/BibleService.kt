package com.lumenscriptura

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException

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

class BibleService(private val context: Context) : IBibleService {
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

        withContext(Dispatchers.IO) {
            try {
                val content = try {
                    context.assets.open("kjv.json").bufferedReader().use { it.readText() }
                } catch (e: FileNotFoundException) {
                    Log.e("BibleService", "kjv.json not found in assets", e)
                    withContext(Dispatchers.Main) { onError("Bible data file missing") }
                    return@withContext
                } catch (e: Exception) {
                    Log.e("BibleService", "Error reading kjv.json", e)
                    withContext(Dispatchers.Main) { onError("Failed to read Bible data") }
                    return@withContext
                }

                bibleData = try {
                    json.decodeFromString<BibleData>(content)
                } catch (e: Exception) {
                    Log.e("BibleService", "Error parsing kjv.json", e)
                    withContext(Dispatchers.Main) { onError("Failed to parse Bible data") }
                    null
                }

                if (bibleData != null) {
                    withContext(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) {
                Log.e("BibleService", "Unexpected error during load", e)
                withContext(Dispatchers.Main) { onError("An unexpected error occurred") }
            }
        }
    }

    private suspend fun ensureLoaded() {
        if (bibleData == null) {
            // In a real app, we might want to throw or trigger a reload
            // For now, we'll try a silent load but this shouldn't be the primary path
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
