package com.example.listview

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState

@Composable
fun ListScreen(
    viewModel: ListViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(modifier = modifier.fillMaxSize()) {
        val sortOptions = remember { listOf("Ascending", "Descending") }
        var sortSelectedOption by remember { mutableStateOf(sortOptions[0]) }

        // Tabs to replace filter radio buttons
        val tabs = remember { listOf("Return", "Reroute", "Forward") }
        var selectedTabIndex by remember { mutableStateOf(0) }
        val selectedTab = tabs[selectedTabIndex]

        // collect the paging flow for the selected tab
        val pagingFlow = remember(selectedTab, sortSelectedOption) { viewModel.pagingFlowForTab(selectedTab, sortSelectedOption) }
        val pagingItems = pagingFlow.collectAsLazyPagingItems()

        val updateStart by viewModel.updateStart.collectAsState()

        val listState = rememberLazyListState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                "Sample List View",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, top = 50.dp, bottom = 24.dp)
            )

            // TabRow filter (shows only items whose title matches the selected tab)
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = index == selectedTabIndex, onClick = { selectedTabIndex = index }, text = { Text(title) })
                }
            }

            // Sort radio row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(sortOptions) { option ->
                    Row(
                        modifier = Modifier
                            .clickable { sortSelectedOption = option }
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = (option == sortSelectedOption),
                            onClick = { sortSelectedOption = option }
                        )
                        Text(text = option)
                    }
                }
            }

            // per-group expansion states
            val groupExpanded: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf<String, Boolean>() }

            // reset scroll & group expansion when tab or sort changes
            LaunchedEffect(selectedTab, sortSelectedOption) {
                listState.scrollToItem(0)
                groupExpanded.clear()
            }

            // Render the paged DisplayItem stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(pagingItems.itemCount) { index ->
                    val di = pagingItems[index]
                    when (di) {
                        is DisplayItem.Header -> {
                            val groupKey = di.group
                            // default expanded = true
                            groupExpanded.getOrPut(groupKey) { true }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF424242)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { groupExpanded[groupKey] = !(groupExpanded[groupKey] ?: true) }
                                    .animateContentSize(),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text(text = groupKey, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(text = "${di.count}", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        is DisplayItem.Child -> {
                            val groupKey = di.group
                            val isExpandedNow = groupExpanded[groupKey] ?: true
                            if (isExpandedNow) {
                                // optionally sort children if requested (we only have current page's children available)
                                ItemRow(di.item, backgroundColor = Color(0xFFEEEEEE))
                            }
                        }

                        else -> {
                            // null or placeholder
                            Box(modifier = Modifier.fillMaxWidth()) { }
                        }
                    }
                }

                // optional footer showing append loading
                item {
                    when (pagingItems.loadState.append) {
                        is LoadState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun ItemRow(item: Item, backgroundColor: Color = Color.White) {
    // per-item expanded state (remembered by item id) so only composed items keep state
    var expanded by remember(item.bagTagId) { mutableStateOf(false) }
    // stable random details per item
    val details = remember(item.bagTagId) { "Details: ${List(8) { ('a'..'z').random() }.joinToString("")}" }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (item.title == "Return") expanded = !expanded }
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(text = item.bagTagId.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            // show title and name for clarity
            item.title?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it)
            }
            item.name?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it)
            }

            if (item.title == "Return" && expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = details)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListScreenPreview() {
    val sampleItems = listOf(
        Item(1, "Preview A", "John"),
        Item(2, "Return", "Nimi")
    )

    // Show sample rows for preview
    Column(Modifier.padding(16.dp)) {
        ItemRow(sampleItems[0])
        Spacer(modifier = Modifier.height(8.dp))
        ItemRow(sampleItems[1])
    }
}
