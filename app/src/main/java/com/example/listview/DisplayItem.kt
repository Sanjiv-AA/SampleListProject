package com.example.listview

sealed class DisplayItem {
    data class Header(val group: String, val count: Int) : DisplayItem()
    data class Child(val item: Item, val group: String) : DisplayItem()
}

