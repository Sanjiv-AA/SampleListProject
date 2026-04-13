package com.example.listview

// UI state for the list screen
data class ListUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

