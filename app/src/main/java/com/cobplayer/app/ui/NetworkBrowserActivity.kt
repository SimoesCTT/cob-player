package com.cobplayer.app.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cobplayer.app.R
import com.cobplayer.app.data.NetworkServerStore
import com.cobplayer.app.databinding.ActivityNetworkBrowserBinding
import com.cobplayer.app.databinding.ItemNetworkFileBinding
import com.cobplayer.app.net.FtpClientWrapper
import com.cobplayer.app.net.SmbClientWrapper
import java.util.concurrent.Executors

@UnstableApi
class NetworkBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkBrowserBinding
    private lateinit var adapter: RemoteAdapter
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var smb: SmbClientWrapper? = null
    private var ftp: FtpClientWrapper? = null
    private var sftp: com.cobplayer.app.net.SftpClientWrapper? = null
    private var currentPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RemoteAdapter { entry ->
            if (entry.isDirectory) navigate(entry.path) else openStream(entry)
        }
        binding.fileList.layoutManager = LinearLayoutManager(this)
        binding.fileList.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }

        val id = intent.getStringExtra("server_id") ?: return finish()
        val server = NetworkServerStore.load(this).find { it.id == id } ?: return finish()
        currentPath = server.basePath.trim('/')

        binding.pathText.text = "Connecting…"
        executor.execute {
            try {
                when (server.protocol) {
                    com.cobplayer.app.data.Protocol.SMB -> {
                        smb = SmbClientWrapper().apply {
                            connect(server.host, server.user, server.password, server.basePath)
                        }
                    }
                    com.cobplayer.app.data.Protocol.FTP -> {
                        ftp = FtpClientWrapper().apply {
                            connect(server.host, server.port, server.user, server.password)
                        }
                    }
                    com.cobplayer.app.data.Protocol.SFTP -> {
                        sftp = com.cobplayer.app.net.SftpClientWrapper().apply {
                            connect(server.host, server.port, server.user, server.password)
                        }
                    }
                }
                main.post { navigate(currentPath) }
            } catch (e: Exception) {
                main.post {
                    binding.pathText.text = "Connection failed: ${e.message}"
                }
            }
        }
    }

    private fun navigate(path: String) {
        binding.pathText.text = "/$path"
        executor.execute {
            val entries = try {
                when {
                    smb != null -> smb!!.list(path).map {
                        RemoteItem(it.name, it.isDirectory, it.size, it.path, it.smbUrl, "smb")
                    }
                    ftp != null -> ftp!!.list("/$path").map {
                        RemoteItem(it.name, it.isDirectory, it.size,
                            it.path.trimStart('/'), it.url, "ftp")
                    }
                    sftp != null -> sftp!!.list("/$path").map {
                        RemoteItem(it.name, it.isDirectory, it.size,
                            it.path.trimStart('/'), it.url, "sftp")
                    }
                    else -> emptyList()
                }
            } catch (e: Exception) { emptyList() }
            main.post { adapter.submit(entries) }
        }
    }

    private fun openStream(item: RemoteItem) {
        val intent = android.content.Intent(this, PlayerActivity::class.java)
            .setData(Uri.parse(item.url))
            .putExtra("title", item.name)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.execute {
            smb?.disconnect()
            ftp?.disconnect()
            sftp?.disconnect()
        }
        executor.shutdown()
    }
}

data class RemoteItem(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val path: String,
    val url: String,
    val protocol: String
)

class RemoteAdapter(
    private val onClick: (RemoteItem) -> Unit
) : RecyclerView.Adapter<RemoteAdapter.VH>() {

    private val items = mutableListOf<RemoteItem>()
    fun submit(l: List<RemoteItem>) { items.clear(); items.addAll(l); notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNetworkFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemNetworkFileBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(i: RemoteItem) {
            b.name.text = i.name
            b.icon.setImageResource(
                if (i.isDirectory) R.drawable.ic_cob_folder
                else R.drawable.ic_cob_play
            )
            b.root.setOnClickListener { onClick(i) }
        }
    }
}
