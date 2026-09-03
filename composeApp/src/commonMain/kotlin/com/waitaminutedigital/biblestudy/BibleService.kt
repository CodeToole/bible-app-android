package com.waitaminutedigital.biblestudy

import biblestudyap.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface IBibleService {
    suspend fun load(onSuccess: suspend () -> Unit, onError: (String) -> Unit)
    suspend fun getBookByName(name: String): Book?
    suspend fun getAllBooks(): List<Book>
    suspend fun getChapterVerses(bookName: String, chapter: Int): List<Verse>
    suspend fun getVerseRange(bookName: String, chapter: Int, start: Int, end: Int): List<Verse>
    suspend fun getVerses(bookName: String, chapter: Int, ranges: List<NoteParserService.VerseRange>): List<Verse>
    suspend fun search(query: String): List<Verse>

    // Highlights
    suspend fun saveHighlight(highlight: VerseHighlight)
    suspend fun removeHighlight(book: String, chapter: Int, verseNumber: Int)
    suspend fun getHighlights(): List<VerseHighlight>

    // Bookmarks
    suspend fun getBookmarks(): List<Bookmark>
    suspend fun addBookmark(bookmark: Bookmark)
    suspend fun removeBookmark(book: String, chapter: Int, verseNumber: Int)
    suspend fun isBookmarked(book: String, chapter: Int, verseNumber: Int): Boolean

    // History
    suspend fun addToHistory(history: ReadingHistory)
    suspend fun getHistory(): List<ReadingHistory>
    suspend fun removeHistoryItem(book: String, chapter: Int)
    suspend fun clearHistory()

    // Saved Notes
    suspend fun getNotes(): List<SavedNote>
    suspend fun saveNote(note: SavedNote)
    suspend fun deleteNote(id: String)

    // Saved Questions
    suspend fun getQuestions(): List<SavedQuestion>
    suspend fun saveQuestion(question: SavedQuestion)
    suspend fun deleteQuestion(id: String)
}

class BibleService : IBibleService {
    private var bibleData: BibleData? = null
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val highlights = mutableListOf<VerseHighlight>()
    private val bookmarks = mutableListOf<Bookmark>()
    private val history = mutableListOf<ReadingHistory>()
    private val notes = mutableListOf<SavedNote>()
    private val questions = mutableListOf<SavedQuestion>()

    companion object {
        private const val KEY_HIGHLIGHTS = "biblestudy_highlights"
        private const val KEY_BOOKMARKS = "biblestudy_bookmarks"
        private const val KEY_HISTORY = "biblestudy_history"
        private const val KEY_NOTES = "biblestudy_notes"
        private const val KEY_QUESTIONS = "biblestudy_questions"
    }

    override suspend fun load(onSuccess: suspend () -> Unit, onError: (String) -> Unit) {
        if (bibleData != null) {
            onSuccess()
            return
        }

        try {
            println("BibleService: Reading files/kjv.json via Compose Resources...")
            val content = withContext(Dispatchers.Default) {
                try {
                    val bytes = Res.readBytes("files/kjv.json")
                    bytes.decodeToString()
                } catch (e: Throwable) {
                    println("Error reading files/kjv.json: ${e.message}")
                    ""
                }
            }

            if (content.isBlank()) {
                println("Error: kjv.json content is empty or missing")
                onError("Bible dataset is missing or unresolvable")
                return
            }

            println("BibleService: Read ${content.length} characters. Decoding JSON...")
            val parsedData = json.decodeFromString<BibleData>(content)
            bibleData = parsedData

            // Load local persistent storage
            restoreLocalStorage()

            println("BibleService: Successfully loaded ${parsedData.books.size} books and ${parsedData.verses.size} verses.")
            onSuccess()
        } catch (e: Throwable) {
            println("Unexpected error loading Bible data: ${e.message}")
            onError("An unexpected error occurred loading Bible data")
        }
    }

    private fun restoreLocalStorage() {
        try {
            val hStr = StorageProvider.getString(KEY_HIGHLIGHTS, "")
            if (hStr.isNotBlank()) {
                highlights.clear()
                highlights.addAll(json.decodeFromString<List<VerseHighlight>>(hStr))
            }
        } catch (e: Throwable) { println("Error restoring highlights: ${e.message}") }

        try {
            val bStr = StorageProvider.getString(KEY_BOOKMARKS, "")
            if (bStr.isNotBlank()) {
                bookmarks.clear()
                bookmarks.addAll(json.decodeFromString<List<Bookmark>>(bStr))
            }
        } catch (e: Throwable) { println("Error restoring bookmarks: ${e.message}") }

        try {
            val histStr = StorageProvider.getString(KEY_HISTORY, "")
            if (histStr.isNotBlank()) {
                history.clear()
                history.addAll(json.decodeFromString<List<ReadingHistory>>(histStr))
            }
        } catch (e: Throwable) { println("Error restoring history: ${e.message}") }

        try {
            val nStr = StorageProvider.getString(KEY_NOTES, "")
            if (nStr.isNotBlank()) {
                notes.clear()
                notes.addAll(json.decodeFromString<List<SavedNote>>(nStr))
            }
        } catch (e: Throwable) { println("Error restoring notes: ${e.message}") }

        try {
            val qStr = StorageProvider.getString(KEY_QUESTIONS, "")
            if (qStr.isNotBlank()) {
                questions.clear()
                questions.addAll(json.decodeFromString<List<SavedQuestion>>(qStr))
            }
        } catch (e: Throwable) { println("Error restoring questions: ${e.message}") }
    }

    private fun persistHighlights() {
        try {
            StorageProvider.putString(KEY_HIGHLIGHTS, json.encodeToString(highlights))
        } catch (e: Throwable) { println("Error persisting highlights: ${e.message}") }
    }

    private fun persistBookmarks() {
        try {
            StorageProvider.putString(KEY_BOOKMARKS, json.encodeToString(bookmarks))
        } catch (e: Throwable) { println("Error persisting bookmarks: ${e.message}") }
    }

    private fun persistHistory() {
        try {
            StorageProvider.putString(KEY_HISTORY, json.encodeToString(history))
        } catch (e: Throwable) { println("Error persisting history: ${e.message}") }
    }

    private fun persistNotes() {
        try {
            StorageProvider.putString(KEY_NOTES, json.encodeToString(notes))
        } catch (e: Throwable) { println("Error persisting notes: ${e.message}") }
    }

    private fun persistQuestions() {
        try {
            StorageProvider.putString(KEY_QUESTIONS, json.encodeToString(questions))
        } catch (e: Throwable) { println("Error persisting questions: ${e.message}") }
    }

    override suspend fun getBookByName(name: String): Book? {
        return bibleData?.books?.find { 
            it.longName.equals(name, ignoreCase = true) || 
            it.shortName.equals(name, ignoreCase = true) 
        }
    }

    override suspend fun getAllBooks(): List<Book> {
        return bibleData?.books ?: emptyList()
    }

    override suspend fun getChapterVerses(bookName: String, chapter: Int): List<Verse> {
        return bibleData?.verses?.filter {
            it.bookName.equals(bookName, ignoreCase = true) &&
            it.chapter == chapter
        } ?: emptyList()
    }

    override suspend fun getVerseRange(bookName: String, chapter: Int, start: Int, end: Int): List<Verse> {
        return bibleData?.verses?.filter { 
            it.bookName.equals(bookName, ignoreCase = true) && 
            it.chapter == chapter && 
            it.verseNum in start..end 
        } ?: emptyList()
    }

    override suspend fun getVerses(bookName: String, chapter: Int, ranges: List<NoteParserService.VerseRange>): List<Verse> {
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
        if (query.length < 3) return emptyList()
        return bibleData?.verses?.filter { 
            it.text.contains(query, ignoreCase = true) || 
            it.bookName?.contains(query, ignoreCase = true) == true 
        }?.take(50) ?: emptyList()
    }

    // Highlights
    override suspend fun saveHighlight(highlight: VerseHighlight) {
        highlights.removeAll { it.book.equals(highlight.book, ignoreCase = true) && it.chapter == highlight.chapter && it.verseNumber == highlight.verseNumber }
        highlights.add(highlight)
        persistHighlights()
    }

    override suspend fun removeHighlight(book: String, chapter: Int, verseNumber: Int) {
        highlights.removeAll { it.book.equals(book, ignoreCase = true) && it.chapter == chapter && it.verseNumber == verseNumber }
        persistHighlights()
    }

    override suspend fun getHighlights(): List<VerseHighlight> = highlights.toList()

    // Bookmarks
    override suspend fun getBookmarks(): List<Bookmark> = bookmarks.toList()

    override suspend fun addBookmark(bookmark: Bookmark) {
        bookmarks.removeAll { it.book.equals(bookmark.book, ignoreCase = true) && it.chapter == bookmark.chapter && it.verseNumber == bookmark.verseNumber }
        bookmarks.add(0, bookmark)
        persistBookmarks()
    }

    override suspend fun removeBookmark(book: String, chapter: Int, verseNumber: Int) {
        bookmarks.removeAll { it.book.equals(book, ignoreCase = true) && it.chapter == chapter && it.verseNumber == verseNumber }
        persistBookmarks()
    }

    override suspend fun isBookmarked(book: String, chapter: Int, verseNumber: Int): Boolean {
        return bookmarks.any { it.book.equals(book, ignoreCase = true) && it.chapter == chapter && it.verseNumber == verseNumber }
    }

    // History
    override suspend fun addToHistory(history: ReadingHistory) {
        this.history.removeAll { it.book.equals(history.book, ignoreCase = true) && it.chapter == history.chapter }
        this.history.add(0, history)
        if (this.history.size > 100) this.history.removeAt(this.history.lastIndex)
        persistHistory()
    }

    override suspend fun getHistory(): List<ReadingHistory> = history.toList()

    override suspend fun removeHistoryItem(book: String, chapter: Int) {
        this.history.removeAll { it.book.equals(book, ignoreCase = true) && it.chapter == chapter }
        persistHistory()
    }

    override suspend fun clearHistory() {
        this.history.clear()
        persistHistory()
    }

    // Notes
    override suspend fun getNotes(): List<SavedNote> = notes.toList()

    override suspend fun saveNote(note: SavedNote) {
        val index = notes.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            notes[index] = note
        } else {
            notes.add(0, note)
        }
        persistNotes()
    }

    override suspend fun deleteNote(id: String) {
        notes.removeAll { it.id == id }
        persistNotes()
    }

    // Questions
    override suspend fun getQuestions(): List<SavedQuestion> = questions.toList()

    override suspend fun saveQuestion(question: SavedQuestion) {
        val index = questions.indexOfFirst { it.id == question.id }
        if (index >= 0) {
            questions[index] = question
        } else {
            questions.add(0, question)
        }
        persistQuestions()
    }

    override suspend fun deleteQuestion(id: String) {
        questions.removeAll { it.id == id }
        persistQuestions()
    }
}
