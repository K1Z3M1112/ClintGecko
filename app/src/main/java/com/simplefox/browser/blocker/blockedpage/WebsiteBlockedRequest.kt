package com.simplefox.browser.blocker.blockedpage

data class WebsiteBlockedRequest(
    val blockedUrl: String,
    val previousUrl: String,
    val tabId: String
)
