package com.cobplayer.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.cobplayer.app.R
import com.cobplayer.app.data.PlaylistStore
import com.cobplayer.app.data.RecentStore
import com.cobplayer.app.data.ResumeStore
import com.cobplayer.app.data.SettingsStore.gridView
import com.cobplayer.app.data.SettingsStore.groupByFolder
import com.cobplayer.app.data.FolderGroup
import com.cobplayer.app.data.SettingsStore.showHiddenFiles
import com.cobplayer.app.data.SettingsStore.sortMode
import com.cobplayer.app.databinding.ActivityMainBinding
import com.cobplayer.app.ui.browser.FolderBrowserActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

@UnstableApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var listAdapter: MediaAdapter
    private lateinit var gridAdapter: MediaGridAdapter
    private lateinit var continueAdapter: ContinueWatchingAdapter
    private var currentView: String = VIEW_VIDEOS
    private var searchQuery: String = ""
    private var usingGrid: Boolean = false

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { openPlayer(it, it.lastPathSegment ?: "Selected file") } }

    private val exportM3uLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri -> uri?.let { doExportM3u(it) } }

    private var exportPlaylistId: String? = null

    private val m3uLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importM3u(it) } }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { loadMedia() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listAdapter = MediaAdapter(
            onClick = { openPlayer(Uri.parse(it.uri), it.title) },
            onLongClick = { showItemActions(it) }
        )
        gridAdapter = MediaGridAdapter(
            onClick = { openPlayer(Uri.parse(it.uri), it.title) },
            onLongClick = { showItemActions(it) }
        )
        continueAdapter = ContinueWatchingAdapter { entry ->
            startActivity(Intent(this, PlayerActivity::class.java)
                .setData(Uri.parse(entry.uri))
                .putExtra("title", entry.title))
        }

        binding.continueRow.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.continueRow.adapter = continueAdapter

        usingGrid = this.gridView
        applyLayoutManager()

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.actionSearch.setOnClickListener { showSearchDialog() }
        binding.actionGrid.setOnClickListener {
            usingGrid = !usingGrid
            this.gridView = usingGrid
            applyLayoutManager()
            loadMedia()
            updateGridIcon()
        }
        updateGridIcon()

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_videos -> { currentView = VIEW_VIDEOS; binding.screenTitle.text = getString(R.string.tab_videos); loadMedia() }
                R.id.nav_audio -> { currentView = VIEW_AUDIO; binding.screenTitle.text = getString(R.string.tab_audio); loadMedia() }
                R.id.nav_folders -> startActivity(Intent(this, FolderBrowserActivity::class.java))
                R.id.nav_playlists -> { currentView = VIEW_PLAYLISTS; binding.screenTitle.text = getString(R.string.nav_playlists); loadMedia() }
                R.id.nav_shows -> { currentView = VIEW_SHOWS; binding.screenTitle.text = getString(R.string.tab_shows); loadMedia() }
                R.id.nav_recent -> { currentView = VIEW_RECENT; binding.screenTitle.text = getString(R.string.nav_recent); loadMedia() }
                R.id.nav_sync -> startActivity(Intent(this, SyncActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_network -> startActivity(Intent(this, NetworkStreamsActivity::class.java))
                R.id.nav_servers -> startActivity(Intent(this, NetworkServersActivity::class.java))
                R.id.nav_import_m3u -> m3uLauncher.launch(arrayOf("*/*"))
                R.id.nav_open_file -> pickFileLauncher.launch(arrayOf("video/*", "audio/*"))
                R.id.nav_corn_store -> startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://simoesctt.github.io/corn-store-index/#/")))
            }
            binding.drawerLayout.closeDrawers()
            true
        }

        binding.fabOpen.setOnClickListener {
            pickFileLauncher.launch(arrayOf("video/*", "audio/*"))
        }

        requestMediaPermissions()
        binding.navView.setCheckedItem(R.id.nav_videos)
    }

    override fun onResume() {
        super.onResume()
        loadMedia()
        loadContinueWatching()
    }

    private fun loadContinueWatching() {
        if (currentView != VIEW_VIDEOS && currentView != VIEW_RECENT) {
            binding.continueLabel.visibility = View.GONE
            binding.continueRow.visibility = View.GONE
            return
        }
        val resumeMap = ResumeStore.all(this)
        if (resumeMap.isEmpty()) {
            binding.continueLabel.visibility = View.GONE
            binding.continueRow.visibility = View.GONE
            return
        }

        val entries = mutableListOf<ContinueEntry>()
        val recent = RecentStore.load(this)
        recent.forEach { r ->
            val pos = resumeMap[r.uri] ?: return@forEach
            entries.add(ContinueEntry(r.uri, r.title, pos, 0L))
        }

        if (entries.isEmpty()) {
            binding.continueLabel.visibility = View.GONE
            binding.continueRow.visibility = View.GONE
            return
        }

        binding.continueLabel.visibility = View.VISIBLE
        binding.continueRow.visibility = View.VISIBLE
        continueAdapter.submit(entries.take(10))
    }

    private fun applyLayoutManager() {
        if (usingGrid) {
            binding.mediaList.layoutManager = GridLayoutManager(this, 2)
            binding.mediaList.adapter = gridAdapter
        } else {
            binding.mediaList.layoutManager = LinearLayoutManager(this)
            binding.mediaList.adapter = listAdapter
        }
    }

    private fun updateGridIcon() {
        binding.actionGrid.setImageResource(
            if (usingGrid) R.drawable.ic_cob_list else R.drawable.ic_cob_grid
        )
    }

    private fun submit(items: List<MediaEntry>) {
        if (usingGrid) gridAdapter.submit(items) else listAdapter.submit(items)
    }

    private fun requestMediaPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.READ_MEDIA_VIDEO
            perms += Manifest.permission.READ_MEDIA_AUDIO
        } else perms += Manifest.permission.READ_EXTERNAL_STORAGE
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) loadMedia()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun loadMedia() {
        val base: List<MediaEntry> = when (currentView) {
            VIEW_VIDEOS -> if (this.groupByFolder) queryVideoFolders() else queryVideos()
            VIEW_AUDIO -> if (this.groupByFolder) queryAudioFolders() else queryAudio()
            VIEW_PLAYLISTS -> PlaylistStore.load(this).map {
                MediaEntry("playlist:${it.id}", "${it.name} (${it.uris.size})",
                    0L, 0L, false, isPlaylist = true)
            }
            VIEW_SHOWS -> queryShows()
            VIEW_RECENT -> RecentStore.load(this).map {
                MediaEntry(it.uri, it.title, 0L, 0L, false)
            }
            else -> emptyList()
        }

        val filtered = if (searchQuery.isBlank()) base
            else base.filter { it.title.contains(searchQuery, ignoreCase = true) }

        val sorted = sortList(filtered, this.sortMode)
        submit(sorted)

        binding.emptyState.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyState.text = when (currentView) {
            VIEW_PLAYLISTS -> getString(R.string.playlist_empty)
            else -> getString(R.string.library_empty)
        }
    }

    private fun sortList(list: List<MediaEntry>, mode: String): List<MediaEntry> = when (mode) {
        "name_asc" -> list.sortedBy { it.title.lowercase() }
        "name_desc" -> list.sortedByDescending { it.title.lowercase() }
        "size_desc" -> list.sortedByDescending { it.sizeBytes }
        "duration_desc" -> list.sortedByDescending { it.durationMs }
        "date_asc" -> list.reversed()
        else -> list
    }

    private fun queryVideos(): List<MediaEntry> {
        val items = mutableListOf<MediaEntry>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                items += MediaEntry(
                    Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString()).toString(),
                    c.getString(nameCol) ?: "Unknown",
                    c.getLong(durCol), c.getLong(sizeCol), false
                )
            }
        }
        return items
    }

    private fun queryAudio(): List<MediaEntry> {
        val items = mutableListOf<MediaEntry>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ARTIST
        )
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "Unknown"
                val artist = c.getString(artistCol)
                items += MediaEntry(
                    Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()).toString(),
                    if (artist.isNullOrBlank() || artist == "<unknown>") name else "$name — $artist",
                    c.getLong(durCol), c.getLong(sizeCol), true
                )
            }
        }
        return items
    }

    private fun queryVideoFolders(): List<MediaEntry> {
        val out = mutableListOf<MediaEntry>()
        val projection = arrayOf(
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media._ID
        )
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val rowIdIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val groups = mutableMapOf<Long, Pair<String, MutableList<Long>>>()
            while (c.moveToNext()) {
                val bucketId = c.getLong(idIdx)
                val name = c.getString(nameIdx) ?: "Unknown"
                val rowId = c.getLong(rowIdIdx)
                groups.getOrPut(bucketId) { name to mutableListOf() }.second.add(rowId)
            }
            groups.forEach { (bucketId, pair) ->
                val (name, ids) = pair
                out += MediaEntry(
                    uri = "folder:$bucketId",
                    title = "$name (${ids.size})",
                    durationMs = 0L,
                    sizeBytes = 0L,
                    isAudio = false,
                    isPlaylist = true
                )
            }
        }
        return out.sortedBy { it.title.lowercase() }
    }

    private fun queryAudioFolders(): List<MediaEntry> {
        val out = mutableListOf<MediaEntry>()
        val projection = arrayOf(
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
        )
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            val groups = mutableMapOf<Long, Pair<String, Int>>()
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val name = c.getString(nameIdx) ?: "Unknown"
                val existing = groups[id]
                groups[id] = name to ((existing?.second ?: 0) + 1)
            }
            groups.forEach { (bucketId, pair) ->
                val (name, count) = pair
                out += MediaEntry(
                    uri = "folder_audio:$bucketId",
                    title = "$name ($count)",
                    durationMs = 0L,
                    sizeBytes = 0L,
                    isAudio = true,
                    isPlaylist = true
                )
            }
        }
        return out.sortedBy { it.title.lowercase() }
    }

    private fun openFolder(bucketId: Long, isAudio: Boolean) {
        val ids = mutableListOf<Long>()
        val collectionUri = if (isAudio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val idColumn = if (isAudio) MediaStore.Audio.Media._ID else MediaStore.Video.Media._ID
        val bucketColumn = if (isAudio) MediaStore.Audio.Media.BUCKET_ID else MediaStore.Video.Media.BUCKET_ID

        contentResolver.query(
            collectionUri, arrayOf(idColumn),
            "$bucketColumn = ?", arrayOf(bucketId.toString()), null
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(idColumn)
            while (c.moveToNext()) ids.add(c.getLong(idIdx))
        }

        if (ids.isEmpty()) return
        val playlistUris = ArrayList<String>()
        val playlistTitles = ArrayList<String>()
        ids.forEach { id ->
            val itemUri = Uri.withAppendedPath(collectionUri, id.toString()).toString()
            playlistUris.add(itemUri)
            playlistTitles.add(itemUri.substringAfterLast('/'))
        }
        startActivity(Intent(this, PlayerActivity::class.java)
            .setData(Uri.parse(playlistUris[0]))
            .putExtra("title", playlistTitles[0])
            .putStringArrayListExtra("queue_uris", playlistUris)
            .putStringArrayListExtra("queue_titles", playlistTitles))
    }

    private fun queryShows(): List<MediaEntry> {
        val videos = queryVideos()
        val groups = mutableMapOf<String, MutableList<MediaEntry>>()
        videos.forEach { v ->
            val parsed = com.cobplayer.app.data.TvEpisodeParser.parse(v.title)
            val key = parsed?.show ?: return@forEach
            groups.getOrPut(key) { mutableListOf() }.add(v)
        }
        return groups.map { (show, items) ->
            MediaEntry(
                uri = "show:$show",
                title = "$show (${items.size})",
                durationMs = 0L,
                sizeBytes = 0L,
                isAudio = false,
                isPlaylist = true
            )
        }.sortedBy { it.title.lowercase() }
    }

    private fun openShow(showName: String) {
        val videos = queryVideos().filter {
            val p = com.cobplayer.app.data.TvEpisodeParser.parse(it.title)
            p?.show == showName
        }.sortedWith(compareBy(
            { com.cobplayer.app.data.TvEpisodeParser.parse(it.title)?.season ?: 0 },
            { com.cobplayer.app.data.TvEpisodeParser.parse(it.title)?.episode ?: 0 }
        ))
        if (videos.isEmpty()) return
        startActivity(Intent(this, PlayerActivity::class.java)
            .setData(Uri.parse(videos[0].uri))
            .putExtra("title", videos[0].title)
            .putStringArrayListExtra("queue_uris", ArrayList(videos.map { it.uri }))
            .putStringArrayListExtra("queue_titles", ArrayList(videos.map { it.title })))
    }

    private fun showSearchDialog() {
        val input = AppCompatEditText(this).apply {
            hint = getString(R.string.action_search)
            setText(searchQuery)
            setSelection(text?.length ?: 0)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_search)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                searchQuery = input.text?.toString()?.trim().orEmpty()
                loadMedia()
            }
            .setNeutralButton(R.string.cancel) { _, _ ->
                searchQuery = ""
                loadMedia()
            }.show()
    }

    private fun openPlayer(uri: Uri, title: String) {
        val uriStr = uri.toString()
        if (uriStr.startsWith("show:")) {
            openShow(uriStr.removePrefix("show:")); return
        }
        if (uriStr.startsWith("folder:")) {
            openFolder(uriStr.removePrefix("folder:").toLong(), false); return
        }
        if (uriStr.startsWith("folder_audio:")) {
            openFolder(uriStr.removePrefix("folder_audio:").toLong(), true); return
        }
        if (currentView == VIEW_PLAYLISTS && uriStr.startsWith("playlist:")) {
            startActivity(Intent(this, PlaylistDetailActivity::class.java)
                .putExtra("playlist_id", uri.toString().removePrefix("playlist:")))
            return
        }
        RecentStore.push(this, uri, title)
        startActivity(Intent(this, PlayerActivity::class.java).setData(uri).putExtra("title", title))
    }

    private fun showItemActions(entry: MediaEntry) {
        if (entry.isPlaylist) {
            val id = entry.uri.removePrefix("playlist:")
            val options = arrayOf(
                "Rename", "Export .m3u", "Delete playlist"
            )
            MaterialAlertDialogBuilder(this)
                .setTitle(entry.title)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showRenamePlaylistDialog(id, entry.title.substringBefore(" (").trim())
                        1 -> { exportPlaylistId = id; exportM3uLauncher.launch("${entry.title.substringBefore(" (").trim()}.m3u") }
                        2 -> { PlaylistStore.delete(this, id); loadMedia() }
                    }
                }.show()
            return
        }
        val options = arrayOf(
            getString(R.string.playlist_add),
            getString(R.string.share),
            getString(R.string.entry_delete)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(entry.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddToPlaylistDialog(entry)
                    1 -> shareItem(entry)
                    2 -> deleteEntry(entry)
                }
            }.show()
    }

    private fun showRenamePlaylistDialog(id: String, currentName: String) {
        val input = AppCompatEditText(this).apply {
            setText(currentName)
            setSelection(text?.length ?: 0)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Rename playlist")
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                if (newName.isNotEmpty()) {
                    PlaylistStore.rename(this, id, newName)
                    loadMedia()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteEntry(entry: MediaEntry) {
        if (entry.uri.startsWith("playlist:")) {
            PlaylistStore.delete(this, entry.uri.removePrefix("playlist:"))
        } else {
            try {
                val file = File(Uri.parse(entry.uri).path ?: return)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        loadMedia()
    }

    private fun shareItem(entry: MediaEntry) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (entry.isAudio) "audio/*" else "video/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(entry.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun showAddToPlaylistDialog(entry: MediaEntry) {
        val playlists = PlaylistStore.load(this)
        val names = playlists.map { "${it.name} (${it.uris.size})" }
            .toMutableList()
            .apply { add(0, "+ ${getString(R.string.playlist_create)}") }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.playlist_add)
            .setItems(names.toTypedArray()) { _, which ->
                if (which == 0) showCreatePlaylistDialog(entry)
                else PlaylistStore.addItem(this, playlists[which - 1].id,
                    Uri.parse(entry.uri), entry.title)
            }.show()
    }

    private fun showCreatePlaylistDialog(entry: MediaEntry?) {
        val input = AppCompatEditText(this).apply { hint = getString(R.string.playlist_name_hint) }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.playlist_create)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    val pl = PlaylistStore.create(this, name)
                    entry?.let { PlaylistStore.addItem(this, pl.id, Uri.parse(it.uri), it.title) }
                    if (currentView == VIEW_PLAYLISTS) loadMedia()
                }
            }
            .setNegativeButton(R.string.cancel, null).show()
    }

    private fun doExportM3u(uri: Uri) {
        val id = exportPlaylistId ?: return
        val pl = PlaylistStore.load(this).find { it.id == id } ?: return
        try {
            com.cobplayer.app.data.M3u.write(this, uri, pl.name, pl.uris, pl.titles)
            android.widget.Toast.makeText(this, "Exported ${pl.uris.size} tracks",
                android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Export failed: ${e.message}",
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun importM3u(uri: Uri) {
        val entries = try { com.cobplayer.app.data.M3u.parse(this, uri) }
                      catch (e: Exception) { emptyList() }
        if (entries.isEmpty()) {
            android.widget.Toast.makeText(this, "No entries found",
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val name = uri.lastPathSegment?.substringBeforeLast('.') ?: "Imported"
        val playlist = PlaylistStore.create(this, name)
        entries.forEach { (path, title) ->
            PlaylistStore.addItem(this, playlist.id, Uri.parse(path), title)
        }
        currentView = VIEW_PLAYLISTS
        binding.screenTitle.text = getString(R.string.nav_playlists)
        loadMedia()
        android.widget.Toast.makeText(this,
            "Imported ${entries.size} tracks", android.widget.Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val VIEW_VIDEOS = "videos"
        private const val VIEW_AUDIO = "audio"
        private const val VIEW_PLAYLISTS = "playlists"
        private const val VIEW_RECENT = "recent"
        private const val VIEW_SHOWS = "shows"
    }
}

data class MediaEntry(
    val uri: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val isAudio: Boolean,
    val isPlaylist: Boolean = false
)
