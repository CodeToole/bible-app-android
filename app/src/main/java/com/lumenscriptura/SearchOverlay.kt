package com.lumenscriptura

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenscriptura.ui.theme.GoldText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchModal(
    bibleService: IBibleService,
    onResultClick: (Book, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Verse>>(emptyList()) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Search Bible", style = MaterialTheme.typography.titleLarge, color = GoldText, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    scope.launch {
                        results = bibleService.search(it)
                    }
                },
                placeholder = { Text("Search keywords or references...") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldText,
                    unfocusedBorderColor = Color.Gray
                )
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results) { verse ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                scope.launch {
                                    val book = bibleService.getBookByName(verse.bookName ?: "")
                                    if (book != null) {
                                        onResultClick(book, verse.chapter)
                                    }
                                }
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "${verse.bookName} ${verse.chapter}:${verse.verseNum}",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldText
                        )
                        Text(
                            text = verse.text,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}
