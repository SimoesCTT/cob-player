package com.cobplayer.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cobplayer.app.R
import com.cobplayer.app.data.PlaylistStore
import com.cobplayer.app.databinding.ActivityPlaylistDetailBinding
import com.cobplayer.app.databinding.ItemMediaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@UnstableApi
class PlaylistDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailBinding
    private lateinit var adapter: PlaylistItemsAdapter
    private var playlistId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playlistId = intent.getStringExtra("playlist_id") ?: return finish()

        adapter = PlaylistItemsAdapter(
            onPlay = { index -> playFrom(index) },
            onRemove = { index -> removeAt(index) }
        )
        binding.itemList.layoutManager = LinearLayoutManager(this)
        binding.itemList.adapter = adapter

        // Drag to reorder
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.move(vh.bindingAdapterPosition, target.bindingAdapterPosition)
                persistOrder()
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(binding.itemList)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPlayAll.setOnClickListener { playFrom(0) }

        load()
    }

    private fun load() {
        val pl = PlaylistStore.load(this).find { it.id == playlistId } ?: return finish()
        binding.title.text = pl.name
        adapter.submit(pl.uris, pl.titles)
    }

    private fun persistOrder() {
        val pl = PlaylistStore.load(this).find { it.id == playlistId } ?: return
        val newUris = adapter.uris()
        val newTitles = adapter.titles()
        pl.uris.clear(); pl.uris.addAll(newUris)
        pl.titles.clear(); pl.titles.addAll(newTitles)
        PlaylistStore.save(this, PlaylistStore.load(this).map {
            if (it.id == playlistId) pl else it
        })
    }

    private fun removeAt(index: Int) {
        val pl = PlaylistStore.load(this).find { it.id == playlistId } ?: return
        if (index < 0 || index >= pl.uris.size) return
        pl.uris.removeAt(index); pl.titles.removeAt(index)
        PlaylistStore.save(this, PlaylistStore.load(this).map {
            if (it.id == playlistId) pl else it
        })
        load()
    }

    private fun playFrom(index: Int) {
        val pl = PlaylistStore.load(this).find { it.id == playlistId } ?: return
        if (pl.uris.isEmpty() || index >= pl.uris.size) return
        val intent = android.content.Intent(this, PlayerActivity::class.java)
            .setData(Uri.parse(pl.uris[index]))
            .putExtra("title", pl.titles.getOrNull(index) ?: "")
            .putStringArrayListExtra("queue_uris", ArrayList(pl.uris))
            .putStringArrayListExtra("queue_titles", ArrayList(pl.titles))
            .putExtra("queue_start_index", index)
        startActivity(intent)
    }
}

class PlaylistItemsAdapter(
    private val onPlay: (Int) -> Unit,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<PlaylistItemsAdapter.VH>() {

    private val uris = mutableListOf<String>()
    private val titles = mutableListOf<String>()

    fun submit(u: List<String>, t: List<String>) {
        uris.clear(); uris.addAll(u)
        titles.clear(); titles.addAll(t)
        notifyDataSetChanged()
    }

    fun uris() = uris.toList()
    fun titles() = titles.toList()

    fun move(from: Int, to: Int) {
        if (from < to) {
            for (i in from until to) { swap(i, i + 1) }
        } else {
            for (i in from downTo to + 1) { swap(i, i - 1) }
        }
        notifyItemMoved(from, to)
    }

    private fun swap(a: Int, b: Int) {
        val u = uris[a]; uris[a] = uris[b]; uris[b] = u
        val t = titles[a]; titles[a] = titles[b]; titles[b] = t
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(position)
    override fun getItemCount() = uris.size

    inner class VH(private val b: ItemMediaBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(position: Int) {
            b.title.text = titles.getOrNull(position) ?: uris[position]
            b.subtitle.text = ""
            b.root.setOnClickListener { onPlay(position) }
            b.root.setOnLongClickListener {
                MaterialAlertDialogBuilder(b.root.context)
                    .setTitle("Remove from playlist?")
                    .setMessage(titles.getOrNull(position) ?: "")
                    .setPositiveButton(R.string.entry_delete) { _, _ -> onRemove(position) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                true
            }
        }
    }
}
