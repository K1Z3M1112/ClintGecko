package com.simplefox.browser.tabs

import com.simplefox.browser.media.TabAudioState

data class TabPreview(
    val id: String,
    val title: String,
    val url: String,
    val isIncognito: Boolean,
    val profileId: String = "default",
    val audio: TabAudioState? = null,
    val groupId: String? = null
)
