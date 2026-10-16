package com.cobplayer.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cobplayer.app.R
import com.cobplayer.app.data.SettingsStore.showThumbnails
import com.cobplayer.app.data.ThumbnailLoader
import com.cobplayer.app.databinding.ItemMediaGridBinding
import java.util.Locale

class MediaGridAdapter(
    private val onClick: (MediaEntry) -> Unit,
    private val onLongClick: (MediaEntry) -> Unit
) : RecyclerView.Adapter<MediaGridAdapter.VH>() {

    private val items = mutableListOf<MediaEntry>()

    fun submit(newItems: List<MediaEntry>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMediaGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemMediaGridBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MediaEntry) {
            b.title.text = item.title
            b.subtitle.text = if (item.durationMs > 0) formatDuration(item.durationMs) else ""

            val ctx = b.root.context
            when {
                item.isAudio -> {
                    b.thumb.setImageResource(R.drawable.ic_cob_audio_track)
                    b.thumb.scaleType = android.widget.ImageView.ScaleType.CENTER
                }
                item.isPlaylist -> {
                    b.thumb.setImageResource(R.drawable.ic_cob_playlist)
                    b.thumb.scaleType = android.widget.ImageView.ScaleType.CENTER
                }
                ctx.showThumbnails -> {
                    b.thumb.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    b.thumb.setImageDrawable(null)
                    ThumbnailLoader.load(ctx, android.net.Uri.parse(item.uri)) { bmp ->
                        if (bmp != null) b.thumb.setImageBitmap(bmp)
                    }
                }
                else -> b.thumb.setImageDrawable(null)
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
}
