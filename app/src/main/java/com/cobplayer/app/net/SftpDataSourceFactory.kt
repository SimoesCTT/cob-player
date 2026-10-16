package com.cobplayer.app.net

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource

@UnstableApi
class SftpDataSourceFactory(private val context: Context) : DataSource.Factory {
    override fun createDataSource(): DataSource = SftpDataSource(context)
}
