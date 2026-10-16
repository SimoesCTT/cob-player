package com.cobplayer.app.net

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.util.Vector

data class SftpEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val path: String,
    val url: String
)

class SftpClientWrapper {

    private var session: Session? = null
    private var channel: ChannelSftp? = null
    private var host: String = ""
    private var port: Int = 22

    fun connect(h: String, p: Int, user: String, password: String) {
        disconnect()
        host = h; port = p
        val jsch = JSch()
        session = jsch.getSession(user, h, p)
        session!!.setPassword(password)
        session!!.setConfig("StrictHostKeyChecking", "no")
        session!!.connect(10_000)
        channel = session!!.openChannel("sftp") as ChannelSftp
        channel!!.connect()
    }

    fun list(path: String): List<SftpEntry> {
        val ch = channel ?: return emptyList()
        val resolved = path.ifBlank { "." }
        val entries = mutableListOf<SftpEntry>()
        @Suppress("UNCHECKED_CAST")
        val vector = ch.ls(resolved) as Vector<Any>
        for (obj in vector) {
            val entry = obj as ChannelSftp.LsEntry
            val name = entry.filename
            if (name == "." || name == "..") continue
            val attrs = entry.attrs
            val child = if (resolved == ".") name else "$resolved/$name"
            entries.add(SftpEntry(
                name = name,
                isDirectory = attrs.isDir,
                size = attrs.size.toLong(),
                path = child,
                url = "sftp://$host:$port$child"
            ))
        }
        return entries.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun disconnect() {
        try { channel?.disconnect() } catch (_: Exception) {}
        try { session?.disconnect() } catch (_: Exception) {}
        channel = null; session = null
    }
}
