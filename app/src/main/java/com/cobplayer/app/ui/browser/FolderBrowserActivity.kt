package com.cobplayer.app.ui.browser

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cobplayer.app.R
import com.cobplayer.app.data.StorageRoots
import com.cobplayer.app.databinding.ActivityFolderBrowserBinding
import com.cobplayer.app.ui.PlayerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class FolderBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderBrowserBinding
    private lateinit var adapter: FileAdapter
    private var currentDir: File? = null
    private val backStack = ArrayDeque<File>()

    private val fullAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FileAdapter(
            onFolder = { f ->
                currentDir?.let { backStack.addLast(it) }
                openDirectory(f)
            },
            onFile = { f ->
                startActivity(Intent(this, PlayerActivity::class.java)
                    .setData(Uri.fromFile(f))
                    .putExtra("title", f.name))
            },
            onShare = { f ->
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = guessMime(f.name)
                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f))
                }
                startActivity(Intent.createChooser(share, getString(R.string.share)))
            }
        )

        binding.fileList.layoutManager = LinearLayoutManager(this)
        binding.fileList.adapter = adapter
        binding.btnBack.setOnClickListener { goUp() }
        binding.btnPickRoot.setOnClickListener { showRootsMenu() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (currentDir == null) refresh()
    }

    private fun refresh() {
        if (StorageRoots.hasBroadAccess()) {
            val root = Environment.getExternalStorageDirectory()
            if (root != null && root.exists()) {
                openDirectory(root)
                return
            }
        }
        showFallbackRoots()
    }

    private fun showFallbackRoots() {
        binding.pathText.text = getString(R.string.browse_title)
        val roots = StorageRoots.candidates(this)
        if (roots.isEmpty()) {
            adapter.submit(emptyList())
            return
        }
        // If only one root, just open it
        if (roots.size == 1) {
            openDirectory(roots.first())
            return
        }
        adapter.submit(roots)
        binding.emptyStateMessage.visibility =
            android.view.View.VISIBLE
        binding.emptyStateMessage.text = getString(R.string.browse_grant_hint)
    }

    private fun showRootsMenu() {
        val roots = StorageRoots.candidates(this)
        val labels = roots.map { it.absolutePath }.toTypedArray()
        val extra = if (!StorageRoots.hasBroadAccess() &&
                      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            arrayOf(getString(R.string.browse_grant_full)) else emptyArray()

        val all = labels + extra

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.browse_pick_root)
            .setItems(all) { _, which ->
                if (which < roots.size) {
                    backStack.clear()
                    openDirectory(roots[which])
                } else {
                    requestFullAccess()
                }
            }
            .show()
    }

    private fun requestFullAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:$packageName"))
            try {
                fullAccessLauncher.launch(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backStack.isNotEmpty()) goUp() else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun goUp() {
        if (backStack.isEmpty()) return
        openDirectory(backStack.removeLast())
    }

    private fun openDirectory(dir: File?) {
        if (dir == null || !dir.exists()) return
        binding.emptyStateMessage.visibility = android.view.View.GONE
        currentDir = dir
        binding.pathText.text = dir.absolutePath

        val children = (dir.listFiles() ?: emptyArray())
            .filter { !it.name.startsWith(".") && (it.isDirectory || isMedia(it)) }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        adapter.submit(children)
    }

    private fun isMedia(f: File): Boolean {
        val n = f.name.lowercase()
        return VIDEO_EXTS.any { n.endsWith(it) } || AUDIO_EXTS.any { n.endsWith(it) }
    }

    private fun guessMime(name: String): String {
        val n = name.lowercase()
        return when {
            VIDEO_EXTS.any { n.endsWith(it) } -> "video/*"
            AUDIO_EXTS.any { n.endsWith(it) } -> "audio/*"
            else -> "*/*"
        }
    }

    companion object {
        private val VIDEO_EXTS = listOf(
            ".mp4", ".mkv", ".webm", ".avi", ".mov", ".flv", ".ts", ".m2ts", ".wmv", ".3gp"
        )
        private val AUDIO_EXTS = listOf(
            ".mp3", ".flac", ".ogg", ".m4a", ".aac", ".wav", ".opus", ".wma", ".ape"
        )
    }
}
