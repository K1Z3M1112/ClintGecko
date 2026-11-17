package com.simplefox.browser.mediacapture

fun compatibleAudioTracks(video: DetectedMedia, all: List<DetectedMedia>): List<DetectedMedia> {
    if (video.kind != MediaKind.VIDEO || video.hasAudio != false) return emptyList()
    val group = video.groupUrl ?: return emptyList()
    val sameGroup = all.filter { it.kind == MediaKind.AUDIO && it.groupUrl == group }
    if (sameGroup.isEmpty()) return emptyList()
    val ref = video.audioGroupRef
    val scoped = if (ref != null) sameGroup.filter { it.mediaGroupId == ref } else sameGroup
    return scoped.ifEmpty { sameGroup }.sortedByDescending { it.isDefaultTrack }
}

fun pickPairedAudio(media: DetectedMedia, all: List<DetectedMedia>): DetectedMedia? =
    compatibleAudioTracks(media, all).firstOrNull()
