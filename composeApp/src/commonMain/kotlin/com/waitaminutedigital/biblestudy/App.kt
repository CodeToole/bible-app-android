package com.waitaminutedigital.biblestudy

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
import kotlinx.coroutines.launch

enum class Tab {
    SCRIPTURE, HIGHLIGHTS, STUDY_NOTES, HISTORY
}

@Composable
fun App() {
    val bibleService = remember { BibleService() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }

    LaunchedEffect(Unit) {
        println("App: Initializing BibleService.load()...")
        bibleService.load(
            onSuccess = {
                println("App: BibleService.load() succeeded.")
                try {
                    books = bibleService.getAllBooks()
                    println("App: Loaded ${books.size} books into state.")
                } catch (e: Throwable) {
                    println("App: Error fetching books: ${e.message}")
                    errorMessage = "Failed to fetch book index"
                } finally {
                    isLoading = false
                }
            },
            onError = { err ->
                println("App: BibleService.load() error: $err")
                errorMessage = err
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
                    CircularProgressIndicator(color = GoldAccent)
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
fun MainContent(bibleService: IBibleService, books: List<Book>) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val noteParserService = remember { NoteParserService(bibleService) }

    var activeTab by rememberSaveable { mutableStateOf(Tab.SCRIPTURE) }
    var currentBook by rememberSaveable { mutableStateOf(books.firstOrNull()?.longName ?: "Genesis") }
    var currentChapter by rememberSaveable { mutableIntStateOf(1) }
    var targetScrollVerse by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showChapterPickerDialog by remember { mutableStateOf(false) }

    // Quick Add Modal Dialog State
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickAddType by remember { mutableStateOf("NOTE") } // "NOTE" or "QUESTION"
    var quickAddRef by remember { mutableStateOf("") }
    var quickAddText by remember { mutableStateOf("") }

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

    fun nextChapter() {
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
    }

    fun previousChapter() {
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
                    },
                    onChapterSelected = { book, chapter ->
                        currentBook = book.longName
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
                                IconButton(onClick = { previousChapter() }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Chapter")
                                }
                                Text(
                                    text = "${selectedBook.longName.uppercase()} $currentChapter",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .clickable { showChapterPickerDialog = true }
                                        .padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { nextChapter() }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Chapter")
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
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { showProfileDialog = true },
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
                                Icon(Icons.Default.Add, contentDescription = "Quick Add", tint = Color.Black)
                            }
                        },
                        label = { Text("Quick Add") },
                        selected = false,
                        onClick = { 
                            quickAddRef = "${selectedBook.longName} $currentChapter"
                            quickAddText = ""
                            showQuickAddDialog = true
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
                    Tab.SCRIPTURE -> ScriptureReader(
                        bibleService = bibleService,
                        book = selectedBook,
                        chapter = currentChapter,
                        targetScrollVerse = targetScrollVerse,
                        onScrollComplete = { targetScrollVerse = null },
                        onNextChapter = { nextChapter() },
                        onPreviousChapter = { previousChapter() },
                        onQuickAddNote = { refBook, ch ->
                            quickAddRef = "$refBook $ch"
                            quickAddType = "NOTE"
                            showQuickAddDialog = true
                        },
                        onQuickAddQuestion = { refBook, ch ->
                            quickAddRef = "$refBook $ch"
                            quickAddType = "QUESTION"
                            showQuickAddDialog = true
                        }
                    )
                    Tab.STUDY_NOTES -> StudyNotesScreen(
                        bibleService = bibleService,
                        activeTabInitial = NotesTab.STUDY_NOTES
                    )
                    Tab.HIGHLIGHTS -> BookmarksAndHighlightsScreen(
                        bibleService = bibleService,
                        onNavigate = { bookName, ch, verseNum ->
                            navigateToScripture(bookName, ch, verseNum)
                        }
                    )
                    Tab.HISTORY -> HistoryScreen(
                        bibleService = bibleService,
                        onHistoryClick = { item -> navigateToScripture(item.book, item.chapter) }
                    )
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

            if (showProfileDialog) {
                AlertDialog(
                    onDismissRequest = { showProfileDialog = false },
                    title = { Text("Bible Study App", color = GoldText) },
                    text = {
                        Column {
                            Text("King James Version (KJV)", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("WaitaMinute Digital", style = MaterialTheme.typography.bodyMedium, color = GoldAccent)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Version: 1.0.3", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text("Platform: Cross-Platform (Android & Web Wasm)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text("Storage: Local Persistent Storage Active", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showProfileDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                        ) {
                            Text("OK")
                        }
                    }
                )
            }

            if (showChapterPickerDialog) {
                AlertDialog(
                    onDismissRequest = { showChapterPickerDialog = false },
                    title = { Text("Select Chapter in ${selectedBook.longName}", color = GoldText) },
                    text = {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.heightIn(max = 280.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items((1..selectedBook.totalChapters).toList()) { ch ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(
                                            if (ch == currentChapter) GoldAccent else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            currentChapter = ch
                                            targetScrollVerse = null
                                            showChapterPickerDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        ch.toString(),
                                        fontWeight = if (ch == currentChapter) FontWeight.Bold else FontWeight.Normal,
                                        color = if (ch == currentChapter) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showChapterPickerDialog = false }) {
                            Text("CLOSE")
                        }
                    }
                )
            }

            if (showQuickAddDialog) {
                AlertDialog(
                    onDismissRequest = { showQuickAddDialog = false },
                    title = { Text("Quick Add to ${quickAddRef}", color = GoldText) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                FilterChip(
                                    selected = quickAddType == "NOTE",
                                    onClick = { quickAddType = "NOTE" },
                                    label = { Text("Study Note") }
                                )
                                FilterChip(
                                    selected = quickAddType == "QUESTION",
                                    onClick = { quickAddType = "QUESTION" },
                                    label = { Text("Question for Study") }
                                )
                            }
                            OutlinedTextField(
                                value = quickAddRef,
                                onValueChange = { quickAddRef = it },
                                label = { Text("Verse / Passage Reference") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = quickAddText,
                                onValueChange = { quickAddText = it },
                                label = { Text(if (quickAddType == "NOTE") "Note Content" else "Question for Pastor / Study") },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (quickAddText.isNotBlank()) {
                                    coroutineScope.launch {
                                        if (quickAddType == "NOTE") {
                                            val blocks = noteParserService.parseAndExpand(quickAddRef)
                                            bibleService.saveNote(
                                                SavedNote(
                                                    title = "Quick Note ($quickAddRef)",
                                                    verseRef = quickAddRef,
                                                    content = quickAddText,
                                                    blocks = blocks
                                                )
                                            )
                                        } else {
                                            bibleService.saveQuestion(
                                                SavedQuestion(
                                                    verseRef = quickAddRef,
                                                    questionText = quickAddText
                                                )
                                            )
                                        }
                                        showQuickAddDialog = false
                                        activeTab = Tab.STUDY_NOTES
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                        ) {
                            Text("SAVE")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showQuickAddDialog = false }) {
                            Text("CANCEL")
                        }
                    }
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
    onChapterSelected: (Book, Int) -> Unit
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

        item {
            ExpandableSection("Old Testament (39 Books)", expandedOT) { expandedOT = !expandedOT }
        }
        if (expandedOT) {
            items(books.filter { it.bookNumber <= 460 }) { book ->
                BookItem(book, selectedBook == book, onBookSelected, onChapterSelected)
            }
        }

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
    onChapterSelected: (Book, Int) -> Unit
) {
    var showChapterGrid by remember { mutableStateOf(isSelected) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showChapterGrid = !showChapterGrid
                    onBookSelected(book)
                }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = book.longName,
                modifier = Modifier.weight(1f),
                color = if (isSelected || showChapterGrid) GoldText else White,
                fontWeight = if (isSelected || showChapterGrid) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "${book.totalChapters} Ch",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon(
                if (showChapterGrid) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Chapters",
                tint = GoldText,
                modifier = Modifier.size(18.dp)
            )
        }

        if (showChapterGrid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .padding(horizontal = 32.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items((1..book.totalChapters).toList()) { chapter ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .clickable { onChapterSelected(book, chapter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chapter.toString(), fontSize = 12.sp, color = GoldText)
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarksAndHighlightsScreen(
    bibleService: IBibleService,
    onNavigate: (String, Int, Int) -> Unit
) {
    var highlights by remember { mutableStateOf<List<VerseHighlight>>(emptyList()) }
    var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }
    var activeFilter by remember { mutableStateOf("ALL") } // "ALL", "BOOKMARKS", "HIGHLIGHTS"
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        highlights = bibleService.getHighlights()
        bookmarks = bibleService.getBookmarks()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Bookmarks & Verse Highlights", style = MaterialTheme.typography.titleLarge, color = GoldText, modifier = Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                FilterChip(
                    selected = activeFilter == "ALL",
                    onClick = { activeFilter = "ALL" },
                    label = { Text("ALL (${bookmarks.size + highlights.size})") }
                )
                FilterChip(
                    selected = activeFilter == "BOOKMARKS",
                    onClick = { activeFilter = "BOOKMARKS" },
                    label = { Text("BOOKMARKS (${bookmarks.size})") }
                )
                FilterChip(
                    selected = activeFilter == "HIGHLIGHTS",
                    onClick = { activeFilter = "HIGHLIGHTS" },
                    label = { Text("HIGHLIGHTS (${highlights.size})") }
                )
            }
        }

        if (activeFilter == "ALL" || activeFilter == "BOOKMARKS") {
            items(bookmarks) { b ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigate(b.book, b.chapter, b.verseNumber) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = GoldAccent, modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${b.book} ${b.chapter}:${b.verseNumber}", style = MaterialTheme.typography.titleSmall, color = GoldText)
                            Text("Tap to jump to chapter", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                bibleService.removeBookmark(b.book, b.chapter, b.verseNumber)
                                bookmarks = bibleService.getBookmarks()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Bookmark", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        if (activeFilter == "ALL" || activeFilter == "HIGHLIGHTS") {
            items(highlights) { highlight ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigate(highlight.book, highlight.chapter, highlight.verseNumber) },
                    colors = CardDefaults.cardColors(containerColor = parseHexColor(highlight.colorHex).copy(alpha = 0.25f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatColorFill, contentDescription = null, tint = parseHexColor(highlight.colorHex), modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${highlight.book} ${highlight.chapter}:${highlight.verseNumber}", style = MaterialTheme.typography.titleSmall, color = GoldText)
                            Text("Tap to jump to verse", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                bibleService.removeHighlight(highlight.book, highlight.chapter, highlight.verseNumber)
                                highlights = bibleService.getHighlights()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Highlight", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(bibleService: IBibleService, onHistoryClick: (ReadingHistory) -> Unit) {
    var history by remember { mutableStateOf<List<ReadingHistory>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        history = bibleService.getHistory()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Reading History", style = MaterialTheme.typography.titleLarge, color = GoldText, modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = {
                        scope.launch {
                            bibleService.clearHistory()
                            history = bibleService.getHistory()
                        }
                    }) {
                        Text("CLEAR ALL", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("${item.book} Chapter ${item.chapter}", style = MaterialTheme.typography.bodyLarge)
                    Text("Tap to resume reading", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                IconButton(onClick = {
                    scope.launch {
                        bibleService.removeHistoryItem(item.book, item.chapter)
                        history = bibleService.getHistory()
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
        }
    }
}
