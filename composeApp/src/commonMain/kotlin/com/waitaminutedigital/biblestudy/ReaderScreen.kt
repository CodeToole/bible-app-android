package com.waitaminutedigital.biblestudy

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

fun buildAnnotatedVerseText(rawText: String): AnnotatedString {
    if (!rawText.contains("<red>")) {
        return AnnotatedString(rawText)
    }

    return buildAnnotatedString {
        val tokens = rawText.split("<red>", "</red>")
        var isRed = rawText.startsWith("<red>")

        for (token in tokens) {
            if (isRed) {
                pushStyle(SpanStyle(color = RedLetterColor))
                append(token)
                pop()
            } else {
                append(token)
            }
            isRed = !isRed
        }
    }
}

@Composable
fun ScriptureReader(
    bibleService: IBibleService,
    book: Book,
    chapter: Int,
    targetScrollVerse: Int? = null,
    onScrollComplete: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onPreviousChapter: () -> Unit = {},
    onQuickAddNote: (String, Int) -> Unit = { _, _ -> },
    onQuickAddQuestion: (String, Int) -> Unit = { _, _ -> }
) {
    var verses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var highlights by remember { mutableStateOf<List<VerseHighlight>>(emptyList()) }
    var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }
    val selectedVerseNumbers = remember { mutableStateListOf<Int>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(book, chapter) {
        verses = bibleService.getChapterVerses(book.longName, chapter)
        highlights = bibleService.getHighlights()
        bookmarks = bibleService.getBookmarks()
        bibleService.addToHistory(ReadingHistory(book.longName, chapter))
        selectedVerseNumbers.clear()
    }

    LaunchedEffect(targetScrollVerse, verses) {
        if (targetScrollVerse != null && verses.isNotEmpty()) {
            val index = verses.indexOfFirst { it.verseNum == targetScrollVerse }
            if (index >= 0) {
                listState.animateScrollToItem(index + 1)
                onScrollComplete()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(book, chapter) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset < -120f) {
                            onNextChapter()
                        } else if (dragOffset > 120f) {
                            onPreviousChapter()
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book.longName.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldText
                    )
                    Text(
                        text = "CHAPTER $chapter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        letterSpacing = 2.sp
                    )
                }
            }

            items(verses) { verse ->
                val isSelected = selectedVerseNumbers.contains(verse.verseNum)
                val highlight = highlights.find { 
                    it.book.equals(verse.bookName ?: book.longName, ignoreCase = true) && 
                    it.chapter == verse.chapter && 
                    it.verseNumber == verse.verseNum 
                }
                val isBookmarked = bookmarks.any { 
                    it.book.equals(verse.bookName ?: book.longName, ignoreCase = true) && 
                    it.chapter == verse.chapter && 
                    it.verseNumber == verse.verseNum 
                }
                val highlightColor = highlight?.let { parseHexColor(it.colorHex) }

                VerseRow(
                    verse = verse,
                    isSelected = isSelected,
                    highlightColor = highlightColor,
                    isBookmarked = isBookmarked,
                    onToggleSelection = {
                        if (isSelected) selectedVerseNumbers.remove(verse.verseNum)
                        else selectedVerseNumbers.add(verse.verseNum)
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        AnimatedVisibility(
            visible = selectedVerseNumbers.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionActionBar(
                bookName = book.longName,
                chapter = chapter,
                selectedVerses = selectedVerseNumbers.sorted(),
                onColorSelected = { colorHex ->
                    scope.launch {
                        selectedVerseNumbers.forEach { num ->
                            if (colorHex == null) {
                                bibleService.removeHighlight(book.longName, chapter, num)
                            } else {
                                bibleService.saveHighlight(VerseHighlight(book.longName, chapter, num, colorHex))
                            }
                        }
                        highlights = bibleService.getHighlights()
                        selectedVerseNumbers.clear()
                    }
                },
                onBookmarkToggle = {
                    scope.launch {
                        selectedVerseNumbers.forEach { num ->
                            val alreadyBookmarked = bibleService.isBookmarked(book.longName, chapter, num)
                            if (alreadyBookmarked) {
                                bibleService.removeBookmark(book.longName, chapter, num)
                            } else {
                                bibleService.addBookmark(Bookmark(book.longName, chapter, num))
                            }
                        }
                        bookmarks = bibleService.getBookmarks()
                        selectedVerseNumbers.clear()
                    }
                },
                onAddNote = { rangeText ->
                    onQuickAddNote(if (rangeText.isNotBlank()) rangeText else "${book.longName} $chapter", chapter)
                    selectedVerseNumbers.clear()
                },
                onAddQuestion = { rangeText ->
                    onQuickAddQuestion(if (rangeText.isNotBlank()) rangeText else "${book.longName} $chapter", chapter)
                    selectedVerseNumbers.clear()
                }
            )
        }
    }
}

@Composable
fun VerseRow(
    verse: Verse,
    isSelected: Boolean,
    highlightColor: Color?,
    isBookmarked: Boolean,
    onToggleSelection: () -> Unit
) {
    val dottedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!isSelected && highlightColor != null) highlightColor.copy(alpha = 0.25f) else Color.Transparent)
            .clickable { onToggleSelection() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .drawBehind {
                if (isSelected) {
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = Color(0xFFFFC107),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth,
                        pathEffect = dottedEffect
                    )
                }
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = verse.verseNum.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = if (isSelected) Color(0xFFFFC107) else GoldText,
                modifier = Modifier.padding(top = 4.dp, end = 6.dp)
            )
            if (isBookmarked) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Bookmarked",
                    tint = GoldAccent,
                    modifier = Modifier.size(12.dp).padding(top = 2.dp)
                )
            }
        }
        Text(
            text = buildAnnotatedVerseText(verse.text),
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 28.sp,
            color = if (isSelected) Color(0xFFFFC107).copy(alpha = 0.9f) else Color.Unspecified
        )
    }
}

@Composable
fun SelectionActionBar(
    bookName: String,
    chapter: Int,
    selectedVerses: List<Int>,
    onColorSelected: (String?) -> Unit,
    onBookmarkToggle: () -> Unit,
    onAddNote: (String) -> Unit,
    onAddQuestion: (String) -> Unit
) {
    val rangeText = remember(selectedVerses) {
        if (selectedVerses.isEmpty()) ""
        else {
            val ranges = mutableListOf<String>()
            var start = selectedVerses[0]
            var prev = start
            for (i in 1 until selectedVerses.size) {
                if (selectedVerses[i] != prev + 1) {
                    ranges.add(if (start == prev) "$start" else "$start-$prev")
                    start = selectedVerses[i]
                }
                prev = selectedVerses[i]
            }
            ranges.add(if (start == prev) "$start" else "$start-$prev")
            "$bookName $chapter:${ranges.joinToString(", ")}"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = rangeText,
                style = MaterialTheme.typography.titleMedium,
                color = GoldText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorChip("#FFC107") { onColorSelected("#FFC107") }
                    ColorChip("#2E7D32") { onColorSelected("#2E7D32") }
                    ColorChip("#0288D1") { onColorSelected("#0288D1") }
                    ColorChip("#EC407A") { onColorSelected("#EC407A") }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { onColorSelected(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Block, contentDescription = "Clear Highlight", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onBookmarkToggle) { Icon(Icons.Default.Bookmark, "Bookmark", tint = GoldAccent) }
                    IconButton(onClick = {
                        onAddNote(if (rangeText.isNotBlank()) rangeText else "$bookName $chapter")
                    }) { Icon(Icons.AutoMirrored.Filled.NoteAdd, "Add Note", tint = Color.LightGray) }
                    IconButton(onClick = {
                        onAddQuestion(if (rangeText.isNotBlank()) rangeText else "$bookName $chapter")
                    }) { Icon(Icons.Default.QuestionAnswer, "Ask Question", tint = Color.LightGray) }
                }
            }
        }
    }
}

@Composable
fun ColorChip(hex: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(parseHexColor(hex), CircleShape)
            .clickable { onClick() }
    )
}
