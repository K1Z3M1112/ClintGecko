package com.simplefox.browser.bookmarks

data class BookmarkFolder(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val createdAt: Long = 0L
)
