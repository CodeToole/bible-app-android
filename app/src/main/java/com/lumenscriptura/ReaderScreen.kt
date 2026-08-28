package com.lumenscriptura

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenscriptura.ui.theme.GoldText
import kotlinx.coroutines.launch

@Composable
fun ScriptureReader(
    bibleService: IBibleService,
    book: Book,
    chapter: Int,
    targetScrollVerse: Int? = null,
    onScrollComplete: () -> Unit = {}
) {
    var verses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var highlights by remember { mutableStateOf<List<Highlight>>(emptyList()) }
    val selectedVerseNumbers = remember { mutableStateListOf<Int>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(book, chapter) {
        verses = bibleService.getChapterVerses(book.longName, chapter)
        highlights = bibleService.getHighlights()
        bibleService.addToHistory(HistoryItem(book.longName, chapter))
        selectedVerseNumbers.clear()
    }

    LaunchedEffect(targetScrollVerse, verses) {
        if (targetScrollVerse != null && verses.isNotEmpty()) {
            val index = verses.indexOfFirst { it.verseNum == targetScrollVerse }
            if (index >= 0) {
                // index + 1 because of the header item
                listState.animateScrollToItem(index + 1)
                onScrollComplete()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                val highlight = highlights.find { it.bookName == verse.bookName && it.chapter == verse.chapter && it.verseNum == verse.verseNum }
                val highlightColor = highlight?.let { 
                    try {
                        Color(android.graphics.Color.parseColor(it.colorHex))
                    } catch (_: Exception) {
                        null
                    }
                }

                VerseRow(
                    verse = verse,
                    isSelected = isSelected,
                    highlightColor = highlightColor,
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
                                bibleService.saveHighlight(Highlight(book.longName, chapter, num, colorHex))
                            }
                        }
                        highlights = bibleService.getHighlights()
                        selectedVerseNumbers.clear()
                    }
                },
                onAction = { _ ->
                    // Handle Copy, Share, etc.
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
        Text(
            text = verse.verseNum.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = if (isSelected) Color(0xFFFFC107) else GoldText,
            modifier = Modifier.padding(top = 4.dp, end = 8.dp)
        )
        Text(
            text = verse.text,
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
    onAction: (String) -> Unit
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
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
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
                // Color Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorChip("#FFC107") { onColorSelected("#FFC107") } // Gold
                    ColorChip("#2E7D32") { onColorSelected("#2E7D32") } // Emerald
                    ColorChip("#0288D1") { onColorSelected("#0288D1") } // Sky Blue
                    ColorChip("#EC407A") { onColorSelected("#EC407A") } // Pink
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { onColorSelected(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Block, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onAction("Copy") }) { Icon(Icons.Default.ContentCopy, "Copy", tint = Color.LightGray) }
                    IconButton(onClick = { onAction("Share") }) { Icon(Icons.Default.Share, "Share", tint = Color.LightGray) }
                    IconButton(onClick = { onAction("Note") }) { Icon(Icons.AutoMirrored.Filled.NoteAdd, "Note", tint = Color.LightGray) }
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
            .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
            .clickable { onClick() }
    )
}
