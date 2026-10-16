package com.cobplayer.app.net

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.cobplayer.app.data.NetworkServerStore
import com.cobplayer.app.data.Protocol
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.InputStream

@UnstableApi
class SftpDataSource(
    private val context: android.content.Context
) : BaseDataSource(true) {

    private var session: Session? = null
    private var channel: ChannelSftp? = null
    private var stream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var opened: Boolean = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        uri = dataSpec.uri

        val host = dataSpec.uri.host ?: throw SmbDataSource.SmbException("No host")
        val port = if (dataSpec.uri.port > 0) dataSpec.uri.port else 22
        val path = dataSpec.uri.path ?: "/"

        val server = NetworkServerStore.load(context)
            .firstOrNull { it.protocol == Protocol.SFTP &&
                (it.host == host || it.host == host.substringBefore(':')) }
            ?: throw SmbDataSource.SmbException("No saved SFTP server for $host")

        val jsch = JSch()
        session = jsch.getSession(server.user, host, port)
        session!!.setPassword(server.password)
        session!!.setConfig("StrictHostKeyChecking", "no")
        session!!.connect(10_000)
        channel = session!!.openChannel("sftp") as ChannelSftp
        channel!!.connect()

        stream = channel!!.get(path)

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length
        else C.LENGTH_UNSET.toLong()

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val read = stream?.read(buffer, offset, length) ?: return C.RESULT_END_OF_INPUT
        if (read == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (!opened) return
        opened = false
        try { stream?.close() } catch (_: Exception) {}
        try { channel?.disconnect() } catch (_: Exception) {}
        try { session?.disconnect() } catch (_: Exception) {}
        stream = null; channel = null; session = null
        transferEnded()
    }
}
