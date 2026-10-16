package com.cobplayer.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cobplayer.app.R
import com.cobplayer.app.data.NetworkServer
import com.cobplayer.app.data.NetworkServerStore
import com.cobplayer.app.data.Protocol
import com.cobplayer.app.databinding.ActivityNetworkServersBinding
import com.cobplayer.app.databinding.ItemNetworkServerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@UnstableApi
class NetworkServersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkServersBinding
    private lateinit var adapter: ServerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkServersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ServerAdapter(
            onOpen = { server ->
                startActivity(Intent(this, NetworkBrowserActivity::class.java)
                    .putExtra("server_id", server.id))
            },
            onDelete = { server ->
                NetworkServerStore.remove(this, server.id); refresh()
            }
        )

        binding.serverList.layoutManager = LinearLayoutManager(this)
        binding.serverList.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showAddDialog() }

        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val list = NetworkServerStore.load(this)
        adapter.submit(list)
        binding.emptyState.visibility =
            if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_server_edit, null)
        val spinner = view.findViewById<AppCompatSpinner>(R.id.proto_spinner)
        spinner.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("SMB", "FTP", "SFTP"))

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.server_add)
            .setView(view)
            .setPositiveButton(R.string.ok) { _, _ ->
                val label = view.findViewById<AppCompatEditText>(R.id.in_label).text.toString().trim()
                val host = view.findViewById<AppCompatEditText>(R.id.in_host).text.toString().trim()
                val portStr = view.findViewById<AppCompatEditText>(R.id.in_port).text.toString().trim()
                val user = view.findViewById<AppCompatEditText>(R.id.in_user).text.toString().trim()
                val pass = view.findViewById<AppCompatEditText>(R.id.in_pass).text.toString().trim()
                val path = view.findViewById<AppCompatEditText>(R.id.in_path).text.toString().trim()
                if (host.isBlank()) return@setPositiveButton
                val proto = when (spinner.selectedItemPosition) {
                    0 -> Protocol.SMB
                    1 -> Protocol.FTP
                    else -> Protocol.SFTP
                }
                val defaultPort = when (proto) { Protocol.SMB -> 445; Protocol.FTP -> 21; Protocol.SFTP -> 22 }
                NetworkServerStore.add(this, NetworkServer(
                    id = System.currentTimeMillis().toString(),
                    label = label.ifBlank { host },
                    protocol = proto,
                    host = host,
                    port = portStr.toIntOrNull() ?: defaultPort,
                    user = user,
                    password = pass,
                    basePath = path
                ))
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

class ServerAdapter(
    private val onOpen: (NetworkServer) -> Unit,
    private val onDelete: (NetworkServer) -> Unit
) : RecyclerView.Adapter<ServerAdapter.VH>() {

    private val items = mutableListOf<NetworkServer>()
    fun submit(l: List<NetworkServer>) { items.clear(); items.addAll(l); notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNetworkServerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemNetworkServerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: NetworkServer) {
            b.label.text = s.label
            b.sub.text = "${s.protocol}://${s.host}:${s.port}${s.basePath}"
            b.root.setOnClickListener { onOpen(s) }
            b.root.setOnLongClickListener { onDelete(s); true }
        }
    }
}
