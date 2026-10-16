package com.cobplayer.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cobplayer.app.data.SettingsStore
import com.cobplayer.app.data.SettingsStore.showThumbnails
import com.cobplayer.app.data.ThumbnailLoader
import com.cobplayer.app.databinding.ItemMediaBinding
import java.util.Locale

class MediaAdapter(
    private val onClick: (MediaEntry) -> Unit,
    private val onLongClick: (MediaEntry) -> Unit
) : RecyclerView.Adapter<MediaAdapter.VH>() {

    private val items = mutableListOf<MediaEntry>()

    fun submit(newItems: List<MediaEntry>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemMediaBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MediaEntry) {
            b.title.text = item.title
            b.subtitle.text = if (item.durationMs > 0)
                "${formatDuration(item.durationMs)} · ${formatSize(item.sizeBytes)}" else ""

            val ctx = b.root.context
            b.thumb.setImageDrawable(null)
            if (ctx.showThumbnails && !item.isPlaylist && !item.isAudio) {
                b.thumb.visibility = android.view.View.VISIBLE
                ThumbnailLoader.load(ctx, android.net.Uri.parse(item.uri)) { bmp ->
                    if (bmp != null) b.thumb.setImageBitmap(bmp)
                }
            } else if (item.isAudio) {
                b.thumb.visibility = android.view.View.VISIBLE
                b.thumb.setImageResource(com.cobplayer.app.R.drawable.ic_cob_audio_track)
            } else if (item.isPlaylist) {
                b.thumb.visibility = android.view.View.VISIBLE
                b.thumb.setImageResource(com.cobplayer.app.R.drawable.ic_cob_playlist)
            } else {
                b.thumb.visibility = android.view.View.GONE
            }

            b.root.setOnClickListener { onClick(item) }
            b.root.setOnLongClickListener { onLongClick(item); true }
        }
    }

    private fun formatDuration(ms: Long): String {
        val t = ms / 1000
        return if (t >= 3600) String.format(Locale.US, "%d:%02d:%02d", t / 3600, (t % 3600) / 60, t % 60)
        else String.format(Locale.US, "%d:%02d", t / 60, t % 60)
    }

    private fun formatSize(b: Long): String {
        val mb = b / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format(Locale.US, "%.1f GB", mb / 1024)
               else String.format(Locale.US, "%.0f MB", mb)
    }
}
