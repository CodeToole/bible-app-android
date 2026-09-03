package com.waitaminutedigital.biblestudy

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class NotesTab {
    STUDY_NOTES, QUESTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyNotesScreen(
    bibleService: IBibleService,
    activeTabInitial: NotesTab = NotesTab.STUDY_NOTES,
    initialRef: String = ""
) {
    var activeNotesTab by remember { mutableStateOf(activeTabInitial) }
    var notes by remember { mutableStateOf<List<SavedNote>>(emptyList()) }
    var questions by remember { mutableStateOf<List<SavedQuestion>>(emptyList()) }

    var editingNote by remember { mutableStateOf<SavedNote?>(null) }
    var editingQuestion by remember { mutableStateOf<SavedQuestion?>(null) }

    val scope = rememberCoroutineScope()
    val noteParserService = remember { NoteParserService(bibleService) }

    var referenceInput by remember { mutableStateOf(initialRef) }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }
    var blocks by remember { mutableStateOf<List<ScripturePassageBlock>>(emptyList()) }
    var isParsing by remember { mutableStateOf(false) }
    var isInputExpanded by remember { mutableStateOf(true) }

    var qVerseRef by remember { mutableStateOf(initialRef) }
    var qText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        notes = bibleService.getNotes()
        questions = bibleService.getQuestions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeNotesTab == NotesTab.STUDY_NOTES) GoldAccent else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeNotesTab = NotesTab.STUDY_NOTES }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "STUDY NOTES (${notes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (activeNotesTab == NotesTab.STUDY_NOTES) Color.Black else GoldText
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeNotesTab == NotesTab.QUESTIONS) GoldAccent else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeNotesTab = NotesTab.QUESTIONS }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "QUESTIONS FOR STUDY (${questions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (activeNotesTab == NotesTab.QUESTIONS) Color.Black else GoldText
                )
            }
        }

        if (activeNotesTab == NotesTab.STUDY_NOTES) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(if (isInputExpanded) 16.dp else 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isInputExpanded) "Create / Parse Study Notes" else "Input: ${referenceInput.take(20)}...",
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
                            value = noteTitleInput,
                            onValueChange = { noteTitleInput = it },
                            label = { Text("Note Title / Topic", color = GoldText) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        OutlinedTextField(
                            value = referenceInput,
                            onValueChange = { referenceInput = it },
                            label = { Text("Scripture Reference(s) (Supports Multi-Line)", color = GoldText) },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isParsing = true
                                        blocks = noteParserService.parseAndExpand(referenceInput)
                                        isParsing = false
                                    }
                                },
                                enabled = !isParsing && referenceInput.isNotBlank(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldText)
                            ) {
                                if (isParsing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GoldText, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PARSING...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PARSE SCRIPTURES", fontSize = 12.sp)
                                }
                            }

                            if (blocks.isNotEmpty()) {
                                Text(
                                    "${blocks.filter { it.isScripture }.size} passage(s) verified",
                                    fontSize = 12.sp,
                                    color = Color.Green
                                )
                            }
                        }

                        if (blocks.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Parsed Passage Preview (${blocks.filter { it.isScripture }.sumOf { it.verses.size }} total verses):",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GoldText,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { blocks = emptyList() }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear Preview", tint = Color.Gray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    blocks.forEach { block ->
                                        if (block.isScripture) {
                                            Text(
                                                block.referenceHeader,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = GoldAccent,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                            block.verses.forEach { v ->
                                                Text(
                                                    "${v.verseNum}. ${v.text}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.LightGray,
                                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                                )
                                            }
                                        } else if (block.plainText.isNotBlank()) {
                                            Text(
                                                block.plainText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = noteContentInput,
                            onValueChange = { noteContentInput = it },
                            label = { Text("Personal Notes & Commentary", color = GoldText) },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val finalBlocks = if (blocks.isEmpty() && referenceInput.isNotBlank()) {
                                            noteParserService.parseAndExpand(referenceInput)
                                        } else {
                                            blocks
                                        }
                                        val newNote = SavedNote(
                                            title = if (noteTitleInput.isBlank()) "Study Note" else noteTitleInput,
                                            verseRef = referenceInput,
                                            content = noteContentInput,
                                            blocks = finalBlocks
                                        )
                                        bibleService.saveNote(newNote)
                                        notes = bibleService.getNotes()
                                        noteTitleInput = ""
                                        referenceInput = ""
                                        noteContentInput = ""
                                        blocks = emptyList()
                                        isInputExpanded = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SAVE NOTE")
                            }
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                items(notes) { note ->
                    SavedNoteCard(
                        note = note,
                        onClick = { editingNote = note },
                        onDelete = {
                            scope.launch {
                                bibleService.deleteNote(note.id)
                                notes = bibleService.getNotes()
                            }
                        }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ask a Question for Church / Bible Study", style = MaterialTheme.typography.titleSmall, color = GoldText)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qVerseRef,
                        onValueChange = { qVerseRef = it },
                        label = { Text("Verse / Passage Reference", color = GoldText) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent, unfocusedBorderColor = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qText,
                        onValueChange = { qText = it },
                        label = { Text("Question to Ask Pastor / Teacher", color = GoldText) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent, unfocusedBorderColor = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (qText.isNotBlank()) {
                                scope.launch {
                                    val newQ = SavedQuestion(
                                        verseRef = qVerseRef,
                                        questionText = qText,
                                        answerNotes = "",
                                        isResolved = false
                                    )
                                    bibleService.saveQuestion(newQ)
                                    questions = bibleService.getQuestions()
                                    qText = ""
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                    ) {
                        Text("RECORD QUESTION")
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                items(questions) { question ->
                    SavedQuestionCard(
                        question = question,
                        onClick = { editingQuestion = question },
                        onToggleResolved = {
                            scope.launch {
                                bibleService.saveQuestion(question.copy(isResolved = !question.isResolved))
                                questions = bibleService.getQuestions()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                bibleService.deleteQuestion(question.id)
                                questions = bibleService.getQuestions()
                            }
                        }
                    )
                }
            }
        }
    }

    if (editingNote != null) {
        val note = editingNote!!
        var titleEdit by remember { mutableStateOf(note.title) }
        var refEdit by remember { mutableStateOf(note.verseRef) }
        var contentEdit by remember { mutableStateOf(note.content) }
        var blocksEdit by remember { mutableStateOf(note.blocks) }
        var isParsing by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = { Text("View & Edit Study Note", color = GoldText) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    OutlinedTextField(
                        value = titleEdit,
                        onValueChange = { titleEdit = it },
                        label = { Text("Title / Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = refEdit,
                        onValueChange = { refEdit = it },
                        label = { Text("Scripture Reference") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isParsing = true
                                    blocksEdit = noteParserService.parseAndExpand(refEdit)
                                    isParsing = false
                                }
                            },
                            enabled = !isParsing
                        ) {
                            Text(if (isParsing) "Parsing..." else "Re-parse Scripture", fontSize = 12.sp, color = GoldAccent)
                        }
                    }

                    if (blocksEdit.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Attached Scripture Passage:", style = MaterialTheme.typography.labelSmall, color = GoldText)
                                blocksEdit.filter { it.isScripture }.forEach { block ->
                                    Text(block.referenceHeader, style = MaterialTheme.typography.labelMedium, color = GoldAccent, modifier = Modifier.padding(top = 4.dp))
                                    block.verses.forEach { v ->
                                        Text("${v.verseNum}. ${v.text}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = contentEdit,
                        onValueChange = { contentEdit = it },
                        label = { Text("Personal Notes & Commentary") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val updatedNote = note.copy(
                                title = titleEdit,
                                verseRef = refEdit,
                                content = contentEdit,
                                blocks = blocksEdit,
                                updatedAt = 0L
                            )
                            bibleService.saveNote(updatedNote)
                            notes = bibleService.getNotes()
                            editingNote = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Text("SAVE CHANGES")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNote = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (editingQuestion != null) {
        val question = editingQuestion!!
        var qTextEdit by remember { mutableStateOf(question.questionText) }
        var qRefEdit by remember { mutableStateOf(question.verseRef) }
        var qAnswerEdit by remember { mutableStateOf(question.answerNotes) }
        var qResolvedEdit by remember { mutableStateOf(question.isResolved) }

        AlertDialog(
            onDismissRequest = { editingQuestion = null },
            title = { Text("Question Detail & Answer Notes", color = GoldText) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = qRefEdit,
                        onValueChange = { qRefEdit = it },
                        label = { Text("Reference") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = qTextEdit,
                        onValueChange = { qTextEdit = it },
                        label = { Text("Question to Ask Pastor") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = qAnswerEdit,
                        onValueChange = { qAnswerEdit = it },
                        label = { Text("Answer / Notes from Pastor/Teacher") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = qResolvedEdit, onCheckedChange = { qResolvedEdit = it })
                        Text("Mark as Answered / Resolved", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val updatedQ = question.copy(
                                verseRef = qRefEdit,
                                questionText = qTextEdit,
                                answerNotes = qAnswerEdit,
                                isResolved = qResolvedEdit
                            )
                            bibleService.saveQuestion(updatedQ)
                            questions = bibleService.getQuestions()
                            editingQuestion = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingQuestion = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun SavedNoteCard(note: SavedNote, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (note.title.isBlank()) "Study Note" else note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldText,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
            if (note.verseRef.isNotBlank()) {
                Text(note.verseRef, style = MaterialTheme.typography.labelSmall, color = GoldAccent)
            }
            if (note.blocks.any { it.isScripture }) {
                Text(
                    "Scripture Attached (${note.blocks.filter { it.isScripture }.sumOf { it.verses.size }} verses)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (note.content.isNotBlank()) {
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SavedQuestionCard(
    question: SavedQuestion,
    onClick: () -> Unit,
    onToggleResolved: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (question.isResolved) Color(0xFF2E7D32) else GoldAccent,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (question.isResolved) "ANSWERED" else "PENDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
            if (question.verseRef.isNotBlank()) {
                Text(question.verseRef, style = MaterialTheme.typography.labelSmall, color = GoldText, modifier = Modifier.padding(top = 4.dp))
            }
            if (question.answerNotes.isNotBlank()) {
                Text("Answer: " + question.answerNotes, style = MaterialTheme.typography.bodyMedium, color = LightGray, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
