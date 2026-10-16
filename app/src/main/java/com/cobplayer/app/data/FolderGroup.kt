package com.cobplayer.app.data

data class FolderGroup(
    val bucketId: Long,
    val name: String,
    val uri: String,
    val count: Int,
    val coverUri: String?
)
