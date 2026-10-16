package com.cobplayer.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.cobplayer.app.R
import com.cobplayer.app.data.StreamStore
import com.cobplayer.app.databinding.ActivityNetworkStreamsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@UnstableApi
class NetworkStreamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkStreamsBinding
    private lateinit var adapter: StreamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkStreamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = StreamAdapter(
            onOpen = { entry ->
                startActivity(Intent(this, PlayerActivity::class.java)
                    .setData(Uri.parse(entry.url))
                    .putExtra("title", entry.label))
            },
            onLongClick = { entry -> showActions(entry) }
        )

        binding.streamList.layoutManager = LinearLayoutManager(this)
        binding.streamList.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showAddDialog() }

        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val items = StreamStore.load(this)
        adapter.submit(items)
        binding.emptyState.visibility = if (items.isEmpty())
            android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun showAddDialog() {
        val input = AppCompatEditText(this).apply {
            hint = getString(R.string.stream_url_hint)
            setSingleLine()
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.stream_add)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val url = input.text?.toString()?.trim().orEmpty()
                if (url.isNotEmpty()) {
                    val label = Uri.parse(url).lastPathSegment ?: url
                    StreamStore.push(this, url, label)
                    refresh()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showActions(entry: com.cobplayer.app.data.StreamEntry) {
        val options = arrayOf(
            getString(R.string.stream_open),
            getString(R.string.stream_copy),
            getString(R.string.stream_remove)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(entry.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, PlayerActivity::class.java)
                        .setData(Uri.parse(entry.url)).putExtra("title", entry.label))
                    1 -> {
                        val cm = getSystemService(android.content.ClipboardManager::class.java)
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("url", entry.url))
                    }
                    2 -> { StreamStore.remove(this, entry.url); refresh() }
                }
            }.show()
    }
}
