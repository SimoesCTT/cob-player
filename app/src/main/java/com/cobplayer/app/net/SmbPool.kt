package com.cobplayer.app.net

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare

/**
 * Caches one SMB connection per host so successive reads don't reconnect.
 * Simplistic — a single connection per host, released on close or idle.
 */
object SmbPool {

    private data class Pooled(
        val client: SMBClient,
        val connection: Connection,
        val session: Session,
        val share: DiskShare,
        val lastUsed: Long
    )

    private val pool = mutableMapOf<String, Pooled>()
    private const val IDLE_MS = 60_000L

    @Synchronized
    fun acquire(
        host: String, shareName: String,
        user: String, password: String
    ): Triple<DiskShare, Session, Connection> {
        evictIdle()
        val key = "$host/$shareName/$user"
        pool[key]?.let {
            pool[key] = it.copy(lastUsed = System.currentTimeMillis())
            return Triple(it.share, it.session, it.connection)
        }

        val client = SMBClient()
        val connection = client.connect(host)
        val ac = if (user.isBlank()) AuthenticationContext.anonymous()
                 else AuthenticationContext(user, password.toCharArray(), null)
        val session = connection.authenticate(ac)
        val share = session.connectShare(shareName) as DiskShare
        pool[key] = Pooled(client, connection, session, share, System.currentTimeMillis())
        return Triple(share, session, connection)
    }

    @Synchronized
    fun release(host: String, shareName: String, user: String) {
        val key = "$host/$shareName/$user"
        pool[key]?.let {
            // Don't close here — keep for reuse. Mark stale by removing from map
            pool.remove(key)
        }
    }

    @Synchronized
    fun evictAll() {
        pool.values.forEach {
            try { it.share.close() } catch (_: Exception) {}
            try { it.session.close() } catch (_: Exception) {}
            try { it.connection.close() } catch (_: Exception) {}
            try { it.client.close() } catch (_: Exception) {}
        }
        pool.clear()
    }

    private fun evictIdle() {
        val now = System.currentTimeMillis()
        val stale = pool.filterValues { now - it.lastUsed > IDLE_MS }
        stale.forEach { (k, v) ->
            try { v.share.close() } catch (_: Exception) {}
            try { v.session.close() } catch (_: Exception) {}
            try { v.connection.close() } catch (_: Exception) {}
            try { v.client.close() } catch (_: Exception) {}
            pool.remove(k)
        }
    }
}
