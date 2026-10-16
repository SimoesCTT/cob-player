package com.cobplayer.app.ui.browser

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cobplayer.app.R
import com.cobplayer.app.databinding.ItemFileBinding
import java.io.File
import java.util.Locale

class FileAdapter(
    private val onFolder: (File) -> Unit,
    private val onFile: (File) -> Unit,
    private val onShare: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    private val items = mutableListOf<File>()

    fun submit(newItems: List<File>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemFileBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(f: File) {
            b.fileName.text = f.name
            b.fileIcon.setImageResource(
                if (f.isDirectory) R.drawable.ic_cob_folder
                else R.drawable.ic_cob_play
            )
            b.fileSize.text = if (f.isDirectory) "" else humanSize(f.length())
            b.root.setOnClickListener {
                if (f.isDirectory) onFolder(f) else onFile(f)
            }
            b.root.setOnLongClickListener {
                if (!f.isDirectory) onShare(f)
                true
            }
        }
    }

    private fun humanSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return when {
            mb >= 1024 -> String.format(Locale.US, "%.1f GB", mb / 1024)
            mb >= 1 -> String.format(Locale.US, "%.0f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        }
    }
}
