package com.lumenscriptura

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenscriptura.ui.theme.*
import kotlinx.coroutines.launch

enum class Tab {
    SCRIPTURE, HIGHLIGHTS, STUDY_NOTES, HISTORY
}

@Composable
fun App(bibleService: BibleService) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        bibleService.load(
            onSuccess = {
                scope.launch {
                    books = bibleService.getAllBooks()
                    isLoading = false
                }
            },
            onError = {
                errorMessage = it
                isLoading = false
            }
        )
    }

    BibleStudyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                MainContent(bibleService, books)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(bibleService: BibleService, books: List<Book>) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf(Tab.SCRIPTURE) }
    var currentBook by rememberSaveable { mutableStateOf(books.firstOrNull()?.longName ?: "Genesis") }
    var currentChapter by rememberSaveable { mutableIntStateOf(1) }
    var targetScrollVerse by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var savedNotes by rememberSaveable { mutableStateOf<List<SavedStudyNote>>(emptyList()) }

    val selectedBook = remember(currentBook) {
        books.find { it.longName == currentBook } ?: books.firstOrNull() ?: Book(10, "GEN", "Genesis", 50)
    }

    fun navigateToScripture(bookName: String, chapter: Int, verseNumber: Int? = null) {
        val book = books.find { it.longName.equals(bookName, ignoreCase = true) || it.shortName.equals(bookName, ignoreCase = true) }
        if (book != null) {
            currentBook = book.longName
            currentChapter = chapter
            targetScrollVerse = verseNumber
            activeTab = Tab.SCRIPTURE
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(320.dp)
            ) {
                DrawerContent(
                    books = books,
                    selectedBook = selectedBook,
                    onBookSelected = { book ->
                        currentBook = book.longName
                        currentChapter = 1
                        targetScrollVerse = null
                        activeTab = Tab.SCRIPTURE
                        coroutineScope.launch { drawerState.close() }
                    },
                    onChapterSelected = { chapter ->
                        currentChapter = chapter
                        targetScrollVerse = null
                        activeTab = Tab.SCRIPTURE
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (currentChapter > 1) {
                                        currentChapter--
                                        targetScrollVerse = null
                                    } else {
                                        val prevBookIndex = books.indexOf(selectedBook) - 1
                                        if (prevBookIndex >= 0) {
                                            currentBook = books[prevBookIndex].longName
                                            currentChapter = books[prevBookIndex].totalChapters
                                            targetScrollVerse = null
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                                }
                                Text(
                                    text = "${selectedBook.longName.uppercase()} $currentChapter",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = {
                                    if (currentChapter < selectedBook.totalChapters) {
                                        currentChapter++
                                        targetScrollVerse = null
                                    } else {
                                        val nextBookIndex = books.indexOf(selectedBook) + 1
                                        if (nextBookIndex < books.size) {
                                            currentBook = books[nextBookIndex].longName
                                            currentChapter = 1
                                            targetScrollVerse = null
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("C", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    ScrollableTabRow(
                        selectedTabIndex = activeTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        divider = {}
                    ) {
                        Tab.entries.forEach { screen ->
                            Tab(
                                selected = activeTab == screen,
                                onClick = { 
                                    activeTab = screen 
                                    if (screen != Tab.SCRIPTURE) targetScrollVerse = null
                                },
                                text = { Text(screen.name.uppercase(), fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = activeTab == Tab.SCRIPTURE,
                        onClick = { activeTab = Tab.SCRIPTURE }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks") },
                        label = { Text("Bookmarks") },
                        selected = activeTab == Tab.HIGHLIGHTS,
                        onClick = { activeTab = Tab.HIGHLIGHTS }
                    )
                    NavigationBarItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                            }
                        },
                        label = { Text("Quick Add") },
                        selected = false,
                        onClick = { 
                            activeTab = Tab.STUDY_NOTES
                            targetScrollVerse = null
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.NoteAlt, contentDescription = "Notes") },
                        label = { Text("Notes") },
                        selected = activeTab == Tab.STUDY_NOTES,
                        onClick = { activeTab = Tab.STUDY_NOTES }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        selected = activeTab == Tab.HISTORY,
                        onClick = { activeTab = Tab.HISTORY }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (activeTab) {
                    Tab.SCRIPTURE -> ScriptureReader(bibleService, selectedBook, currentChapter, targetScrollVerse, onScrollComplete = { targetScrollVerse = null })
                    Tab.STUDY_NOTES -> StudyNotesScreen(
                        bibleService = bibleService,
                        savedNotes = savedNotes,
                        onSaveNote = { input, blocks ->
                            savedNotes = listOf(SavedStudyNote(inputReference = input, blocks = blocks)) + savedNotes
                        },
                        onDeleteNote = { note ->
                            savedNotes = savedNotes.filter { it.id != note.id }
                        }
                    )
                    Tab.HIGHLIGHTS -> HighlightsScreen(bibleService, onHighlightClick = { h -> navigateToScripture(h.bookName, h.chapter, h.verseNum) })
                    Tab.HISTORY -> HistoryScreen(bibleService, onHistoryClick = { item -> navigateToScripture(item.bookName, item.chapter) })
                }
            }
            
            if (showSearch) {
                SmartSearchModal(
                    bibleService = bibleService,
                    onResultClick = { book, chapter, verse ->
                        currentBook = book.longName
                        currentChapter = chapter
                        targetScrollVerse = verse
                        activeTab = Tab.SCRIPTURE
                        showSearch = false
                    },
                    onDismiss = { showSearch = false }
                )
            }
        }
    }
}

@Composable
fun DrawerContent(
    books: List<Book>,
    selectedBook: Book,
    onBookSelected: (Book) -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    var expandedOT by remember { mutableStateOf(true) }
    var expandedNT by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("BIBLE STUDY APP", style = MaterialTheme.typography.titleMedium, color = GoldText)
                Text("KING JAMES VERSION", style = MaterialTheme.typography.labelSmall)
                Text("WAITAMINUTE DIGITAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
        }

        item {
            DrawerMenuItem("Current Reader", Icons.Default.MenuBook, true)
            DrawerMenuItem("Highlights & Bookmarks", Icons.Default.BookmarkBorder, false)
            DrawerMenuItem("Auto-Expanded Notes", Icons.Default.AutoAwesome, false)
            DrawerMenuItem("Reading History", Icons.Default.History, false)
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
        }

        // Old Testament
        item {
            ExpandableSection("Old Testament (39 Books)", expandedOT) { expandedOT = !expandedOT }
        }
        if (expandedOT) {
            items(books.filter { it.bookNumber <= 460 }) { book ->
                BookItem(book, selectedBook == book, onBookSelected, onChapterSelected)
            }
        }

        // New Testament
        item {
            ExpandableSection("New Testament (27 Books)", expandedNT) { expandedNT = !expandedNT }
        }
        if (expandedNT) {
            items(books.filter { it.bookNumber > 460 }) { book ->
                BookItem(book, selectedBook == book, onBookSelected, onChapterSelected)
            }
        }
    }
}

@Composable
fun DrawerMenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) GoldText else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            label,
            color = if (selected) GoldText else White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ExpandableSection(label: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = GoldText)
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = GoldText
        )
    }
}

@Composable
fun BookItem(
    book: Book,
    isSelected: Boolean,
    onBookSelected: (Book) -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = book.longName,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBookSelected(book) }
                .padding(horizontal = 32.dp, vertical = 8.dp),
            color = if (isSelected) GoldText else White
        )
        if (isSelected) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items((1..book.totalChapters).toList()) { chapter ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .clickable { onChapterSelected(chapter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chapter.toString(), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightsScreen(bibleService: IBibleService, onHighlightClick: (Highlight) -> Unit) {
    var highlights by remember { mutableStateOf<List<Highlight>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        highlights = bibleService.getHighlights()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Highlights & Bookmarks", style = MaterialTheme.typography.titleLarge, color = GoldText, modifier = Modifier.padding(vertical = 16.dp))
        }
        items(highlights) { highlight ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onHighlightClick(highlight) },
                colors = CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(highlight.colorHex)).copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${highlight.bookName} ${highlight.chapter}:${highlight.verseNum}", style = MaterialTheme.typography.labelMedium, color = GoldText)
                    Text("Highlighted on ${java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(highlight.timestamp))}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(bibleService: IBibleService, onHistoryClick: (HistoryItem) -> Unit) {
    var history by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        history = bibleService.getHistory()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Reading History", style = MaterialTheme.typography.titleLarge, color = GoldText, modifier = Modifier.padding(vertical = 16.dp))
        }
        items(history) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHistoryClick(item) }
                    .padding(vertical = 12.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = GoldText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("${item.bookName} ${item.chapter}", style = MaterialTheme.typography.bodyLarge)
                    Text(java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(item.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
        }
    }
}
