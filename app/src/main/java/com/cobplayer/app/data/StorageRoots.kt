package com.cobplayer.app.data

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

object StorageRoots {

    /** Physical directories we try to open directly. */
    fun candidates(context: Context): List<File> {
        val out = mutableListOf<File>()

        // Primary external storage — /storage/emulated/0
        val primary = Environment.getExternalStorageDirectory()
        if (primary != null && primary.exists()) out.add(primary)

        // Well-known subfolders as quick access
        val subs = listOf("Download", "Movies", "Music", "DCIM", "Videos", "Documents", "Podcasts")
        subs.forEach { name ->
            val f = File(primary, name)
            if (f.exists() && f.isDirectory) out.add(f)
        }

        // App-specific external dirs (always accessible without permission)
        context.getExternalFilesDir(null)?.let { if (it.exists()) out.add(it) }

        // SD card roots
        val storageDirs = File("/storage").listFiles()
        storageDirs?.forEach { dir ->
            if (dir.isDirectory && dir.name != "emulated" && dir.name != "self") {
                out.add(dir)
            }
        }

        return out.distinctBy { it.absolutePath }
    }

    /** Whether we can read arbitrary files on external storage. */
    fun hasBroadAccess(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return Environment.getExternalStorageDirectory()?.canRead() == true
    }
}
