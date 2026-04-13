package com.example.listview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ListViewModel : ViewModel() {

    private val totalCount = 2000

    // keep full items in memory (source of truth)
    private val fullItems: MutableList<Item> = MutableList(totalCount) { index ->
        val id = 1001001001L + index
        val labels = listOf("Return", "Reroute", "Forward")
        val names = listOf("Jacob", "Pushpa", "Nimi", "Aiswarya")
        Item(bagTagId = id, title = labels[index % labels.size], name = names[index % names.size])
    }

    // expose update start timestamp so UI can measure time
    private val _updateStart = MutableStateFlow<Long?>(null)
    val updateStart: StateFlow<Long?> = _updateStart.asStateFlow()

    init {
        scheduleNameUpdate()
    }

    fun pagingFlowForTab(selectedTab: String, sortOption: String = "Ascending"): Flow<PagingData<DisplayItem>> {
        // group items by name for the selected tab
        val groupsProvider = {
            val filtered = fullItems.filter { it.title == selectedTab }
            val sorted = when (sortOption) {
                "Ascending" -> filtered.sortedBy { it.bagTagId }
                "Descending" -> filtered.sortedByDescending { it.bagTagId }
                else -> filtered
            }
            sorted.groupBy { it.name ?: "Unknown" }.map { Pair(it.key, it.value) }
        }

        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { GroupedPagingSource(groupsProvider, 50) }
        ).flow.cachedIn(viewModelScope)
    }

    private fun scheduleNameUpdate() {
        viewModelScope.launch {
            delay(10_000)
            val start = System.currentTimeMillis()
            _updateStart.value = start

            val fromId = 1001001010L
            val toId = 1001001090L
            for (i in fullItems.indices) {
                val item = fullItems[i]
                if (item.bagTagId in fromId..toId) {
                    fullItems[i] = item.copy(name = "Sanjiv")
                }
            }

            // notify: the UI will recreate the paging flow when needed (by re-calling pagingFlowForTab or via refresh)
        }
    }
}
