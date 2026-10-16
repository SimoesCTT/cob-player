package com.cobplayer.app.net

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener

@UnstableApi
class CobDataSourceFactory(
    private val context: Context,
    private val upstream: TransferListener? = null
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        val defaultHttp = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("CobPlayer/1.0")

        val default = DefaultDataSource.Factory(context, defaultHttp)
        val smbFactory = SmbDataSourceFactory(context)
        val sftpFactory = SftpDataSourceFactory(context)

        return object : DataSource {
            private var delegate: DataSource? = null

            override fun open(dataSpec: androidx.media3.datasource.DataSpec): Long {
                val scheme = dataSpec.uri.scheme?.lowercase()
                delegate = when (scheme) {
                    "smb" -> smbFactory.createDataSource()
                    "sftp" -> sftpFactory.createDataSource()
                    else -> default.createDataSource()
                }
                return delegate!!.open(dataSpec)
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                delegate?.read(buffer, offset, length) ?: -1

            override fun getUri(): Uri? = delegate?.getUri()

            override fun close() {
                delegate?.close()
                delegate = null
            }

            override fun addTransferListener(transferListener: TransferListener) {
                delegate?.addTransferListener(transferListener)
            }
        }
    }
}
