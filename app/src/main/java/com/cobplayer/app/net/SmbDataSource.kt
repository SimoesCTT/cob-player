package com.cobplayer.app.net

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.cobplayer.app.data.NetworkServerStore
import com.cobplayer.app.data.Protocol
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import java.io.InputStream
import java.util.EnumSet

@UnstableApi
class SmbDataSource(
    private val context: android.content.Context
) : BaseDataSource(/* isNetwork = */ true) {

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var file: File? = null
    private var stream: InputStream? = null

    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var opened: Boolean = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        uri = dataSpec.uri

        val host = dataSpec.uri.host ?: throw SmbException("No host in URI")
        val shareName = dataSpec.uri.pathSegments.firstOrNull()
            ?: throw SmbException("No share in URI")
        val filePath = dataSpec.uri.pathSegments.drop(1).joinToString("\\")

        val server = NetworkServerStore.load(context)
            .firstOrNull {
                it.protocol == Protocol.SMB &&
                (it.host == host || it.host == host.substringBefore(':'))
            } ?: throw SmbException("No saved server for host $host")

        val pooled = SmbPool.acquire(server.host, shareName, server.user, server.password)
        share = pooled.first
        session = pooled.second
        connection = pooled.third

        file = share!!.openFile(
            filePath,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null
        )

        val totalSize = file!!.fileInformation.standardInformation.endOfFile
        stream = file!!.inputStream

        if (dataSpec.position > 0) {
            var skipped = 0L
            while (skipped < dataSpec.position) {
                val n = stream!!.skip(dataSpec.position - skipped)
                if (n <= 0) break
                skipped += n
            }
        }

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong())
            dataSpec.length
        else (totalSize - dataSpec.position).coerceAtLeast(0)

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val read = stream?.read(buffer, offset, toRead) ?: return C.RESULT_END_OF_INPUT
        if (read == -1) return C.RESULT_END_OF_INPUT
        bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (!opened) return
        opened = false
        try { stream?.close() } catch (_: Exception) {}
        try { file?.close() } catch (_: Exception) {}
        try { stream?.close() } catch (_: Exception) {}
        try { file?.close() } catch (_: Exception) {}
        // share/session/connection stay in pool
        stream = null; file = null; share = null
        session = null; connection = null; client = null
        uri = null
        transferEnded()
    }

    class SmbException(message: String) : java.io.IOException(message)
}
