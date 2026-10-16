package com.cobplayer.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cobplayer.app.R
import com.cobplayer.app.data.ThumbnailLoader
import com.cobplayer.app.databinding.ItemContinueWatchingBinding
import java.util.Locale

data class ContinueEntry(
    val uri: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long
)

class ContinueWatchingAdapter(
    private val onClick: (ContinueEntry) -> Unit
) : RecyclerView.Adapter<ContinueWatchingAdapter.VH>() {

    private val items = mutableListOf<ContinueEntry>()

    fun submit(newItems: List<ContinueEntry>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemContinueWatchingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemContinueWatchingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ContinueEntry) {
            b.title.text = item.title
            val pct = if (item.durationMs > 0)
                (item.positionMs * 100 / item.durationMs).toInt() else 0
            b.progress.text = "$pct% · ${formatMs(item.positionMs)} / ${formatMs(item.durationMs)}"
            b.progressBar.progress = pct

            ThumbnailLoader.load(b.root.context, android.net.Uri.parse(item.uri)) { bmp ->
                if (bmp != null) b.thumb.setImageBitmap(bmp)
                else b.thumb.setImageResource(R.drawable.ic_cob_play)
            }

            b.root.setOnClickListener { onClick(item) }
        }
    }

    private fun formatMs(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
               else String.format(Locale.US, "%d:%02d", m, sec)
    }
}
