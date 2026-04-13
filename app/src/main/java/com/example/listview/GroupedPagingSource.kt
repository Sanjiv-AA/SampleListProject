package com.example.listview

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay

/**
 * A PagingSource that pages per-group. Each page returns a list of DisplayItem (Header and Child).
 * - For the first page of a group, the Header(DisplayItem.Header) is emitted followed by up to pageSize children.
 * - For next pages of the same group, only Child items are emitted.
 * - Only when all children of a group have been emitted do we move to the next group's first page (which includes its Header).
 *
 * Key is a PairInt encoded as Int: key = groupIndex * 1_000_000 + pageIndexWithinGroup (pageIndex starts at 0).
 */
class GroupedPagingSource(
    private val groupsProvider: () -> List<Pair<String, List<Item>>>,
    private val pageSize: Int
) : PagingSource<Int, DisplayItem>() {

    private fun encodeKey(groupIndex: Int, pageIndex: Int): Int = groupIndex * 1_000_000 + pageIndex
    private fun decodeKey(key: Int): Pair<Int, Int> = Pair(key / 1_000_000, key % 1_000_000)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DisplayItem> {
        try {
            val groups = groupsProvider()
            val key = params.key ?: encodeKey(0, 0)
            val (groupIndex, pageIndex) = decodeKey(key)

            // simulate a delay when loading subsequent pages (next set of 5 items)
//            if (pageIndex > 0) {
//                delay(5_000)
//            }

            if (groupIndex >= groups.size) {
                return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }

            val (groupName, itemsInGroup) = groups[groupIndex]
            // For pageIndex == 0, include header first
            val start = pageIndex * pageSize
            if (start >= itemsInGroup.size) {
                // no more children in this group -> move to next group
                val nextGroupIndex = groupIndex + 1
                return if (nextGroupIndex >= groups.size) {
                    LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
                } else {
                    // return next group's first page (will include its header)
                    val nextKey = encodeKey(nextGroupIndex, 0)
                    LoadResult.Page(emptyList(), prevKey = null, nextKey = nextKey)
                }
            }

            val end = minOf(start + pageSize, itemsInGroup.size)
            val childSlice = itemsInGroup.subList(start, end)

            val pageItems = mutableListOf<DisplayItem>()
            if (pageIndex == 0) {
                pageItems.add(DisplayItem.Header(groupName, itemsInGroup.size))
            }
            childSlice.forEach { itItem -> pageItems.add(DisplayItem.Child(itItem, groupName)) }

            // determine nextKey
            val nextKey = if (end < itemsInGroup.size) {
                // next page within same group
                encodeKey(groupIndex, pageIndex + 1)
            } else {
                // move to next group's first page if any
                val nextGroupIndex = groupIndex + 1
                if (nextGroupIndex < groups.size) encodeKey(nextGroupIndex, 0) else null
            }

            val prevKey = if (groupIndex == 0 && pageIndex == 0) null else {
                // compute previous key
                if (pageIndex > 0) encodeKey(groupIndex, pageIndex - 1)
                else {
                    val prevGroupIndex = groupIndex - 1
                    val prevGroupSize = groups[prevGroupIndex].second.size
                    val lastPageIndex = if (prevGroupSize == 0) 0 else ( (prevGroupSize - 1) / pageSize )
                    encodeKey(prevGroupIndex, lastPageIndex)
                }
            }

            return LoadResult.Page(pageItems, prevKey = prevKey, nextKey = nextKey)
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, DisplayItem>): Int? {
        // Simple fallback: return null so refresh starts from beginning
        return null
    }
}
