package com.cobplayer.app.net

import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare

data class RemoteEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val path: String,
    val smbUrl: String
)

class SmbClientWrapper {

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var shareName: String = ""
    private var hostForUrl: String = ""

    fun connect(host: String, user: String, password: String, basePath: String) {
        disconnect()
        hostForUrl = host
        client = SMBClient()
        connection = client!!.connect(host.trimStart('\\').trimEnd('\\'))
        val ac = if (user.isBlank()) AuthenticationContext.anonymous()
                 else AuthenticationContext(user, password.toCharArray(), null)
        session = connection!!.authenticate(ac)
        shareName = basePath.trim('/').substringBefore('/').ifBlank { "C$" }
        share = session!!.connectShare(shareName) as DiskShare
    }

    fun list(path: String): List<RemoteEntry> {
        val s = share ?: return emptyList()
        val smbPath = path.trim('/').replace('/', '\\')
        val out = mutableListOf<RemoteEntry>()

        s.list(smbPath).forEach { entry: FileIdBothDirectoryInformation ->
            val name = entry.fileName
            if (name == "." || name == "..") return@forEach
            val isDir = (entry.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
            val childPath = if (smbPath.isBlank()) name else "$smbPath\\$name"
            val urlPath = childPath.replace('\\', '/')
            out.add(RemoteEntry(
                name = name,
                isDirectory = isDir,
                size = entry.endOfFile,
                path = childPath,
                smbUrl = "smb://$hostForUrl/$shareName/$urlPath"
            ))
        }
        return out.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun disconnect() {
        try { share?.close() } catch (_: Exception) {}
        try { session?.close() } catch (_: Exception) {}
        try { connection?.close() } catch (_: Exception) {}
        try { client?.close() } catch (_: Exception) {}
        share = null; session = null; connection = null; client = null
    }
}
