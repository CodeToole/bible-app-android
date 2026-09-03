package com.waitaminutedigital.biblestudy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun StudyNotesScreen(
    bibleService: IBibleService,
    savedNotes: List<SavedStudyNote>,
    onSaveNote: (String, List<ScripturePassageBlock>) -> Unit,
    onDeleteNote: (SavedStudyNote) -> Unit
) {
    val noteParserService = remember { NoteParserService(bibleService) }
    var referenceInput by remember { mutableStateOf("") }
    var blocks by remember { mutableStateOf<List<ScripturePassageBlock>>(emptyList()) }
    var isInputExpanded by remember { mutableStateOf(true) }
    var showSavedArchive by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    fun getFormattedNoteText(): String {
        val sb = StringBuilder()
        sb.append("STUDY NOTES: $referenceInput\n\n")
        blocks.forEach { block ->
            if (block.isScripture) {
                sb.append("${block.referenceHeader}\n")
                block.verses.forEach { verse ->
                    sb.append("${verse.verseNum} ${verse.text}\n")
                }
                sb.append("\n")
            } else {
                sb.append("${block.plainText}\n\n")
            }
        }
        return sb.toString()
    }

    fun copyToClipboard() {
        val text = getFormattedNoteText()
        clipboardManager.setText(AnnotatedString(text))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(if (isInputExpanded) 16.dp else 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isInputExpanded) "Study Notes Input" else "Notes: ${referenceInput.take(20)}...",
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldText,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isInputExpanded = !isInputExpanded }) {
                        Icon(if (isInputExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }
                }
                
                if (isInputExpanded) {
                    OutlinedTextField(
                        value = referenceInput,
                        onValueChange = { referenceInput = it },
                        label = { Text("Scripture Reference / Study Notes", color = GoldText) },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = GoldAccent
                        )
                    )
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                blocks = noteParserService.parseAndExpand(referenceInput)
                                isInputExpanded = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                    ) {
                        Text("PARSE")
                    }
                }
            }
        }

        if (blocks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { copyToClipboard() }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = GoldAccent)
                }
                IconButton(onClick = { 
                    onSaveNote(referenceInput, blocks)
                }) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Save Note", tint = GoldAccent)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(blocks) { block ->
                if (block.isScripture) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = block.referenceHeader,
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            block.verses.forEach { verse ->
                                Text(
                                    text = buildAnnotatedVerseText("${verse.verseNum} ${verse.text}"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = block.plainText,
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightGray
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showSavedArchive = !showSavedArchive }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SAVED STUDY NOTES (${savedNotes.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldText,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(if (showSavedArchive) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = GoldText)
                }
            }

            if (showSavedArchive) {
                items(savedNotes) { savedNote ->
                    SavedNoteItem(
                        note = savedNote,
                        onClick = {
                            referenceInput = savedNote.inputReference
                            blocks = savedNote.blocks
                            isInputExpanded = false
                        },
                        onDelete = { onDeleteNote(savedNote) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedNoteItem(note: SavedStudyNote, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.inputReference.take(40) + if (note.inputReference.length > 40) "..." else "", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}
