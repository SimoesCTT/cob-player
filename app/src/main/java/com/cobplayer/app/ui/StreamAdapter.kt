package com.cobplayer.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cobplayer.app.data.StreamEntry
import com.cobplayer.app.databinding.ItemStreamBinding

class StreamAdapter(
    private val onOpen: (StreamEntry) -> Unit,
    private val onLongClick: (StreamEntry) -> Unit
) : RecyclerView.Adapter<StreamAdapter.VH>() {

    private val items = mutableListOf<StreamEntry>()

    fun submit(newItems: List<StreamEntry>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemStreamBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemStreamBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: StreamEntry) {
            b.label.text = e.label
            b.url.text = e.url
            b.root.setOnClickListener { onOpen(e) }
            b.root.setOnLongClickListener { onLongClick(e); true }
        }
    }
}
