package com.cobplayer.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.media3.common.util.UnstableApi
import com.cobplayer.app.data.SecureStore
import com.cobplayer.app.data.SyncConfigStore.lastSyncTime
import com.cobplayer.app.data.SyncConfigStore.syncPassword
import com.cobplayer.app.data.SyncConfigStore.syncUrl
import com.cobplayer.app.data.SyncConfigStore.syncUser
import com.cobplayer.app.data.SyncSnapshot
import com.cobplayer.app.databinding.ActivitySyncBinding
import com.cobplayer.app.net.WebDavClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@UnstableApi
class SyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySyncBinding
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rowConfigure.setOnClickListener { showConfigureDialog() }
        binding.btnTest.setOnClickListener { testConnection() }
        binding.btnPush.setOnClickListener { push() }
        binding.btnPull.setOnClickListener { pull() }

        refreshStatus()
    }

    private fun refreshStatus() {
        val url = this.syncUrl
        binding.statusUrl.text = url.ifBlank { "(not configured)" }
        binding.statusLastSync.text = if (this.lastSyncTime > 0)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(this.lastSyncTime))
        else "(never)"
        val configured = url.isNotBlank() && this.syncUser.isNotBlank()
        binding.btnTest.isEnabled = configured
        binding.btnPush.isEnabled = configured
        binding.btnPull.isEnabled = configured
    }

    private fun showConfigureDialog() {
        val view = layoutInflater.inflate(com.cobplayer.app.R.layout.dialog_sync_config, null)
        val url = view.findViewById<AppCompatEditText>(com.cobplayer.app.R.id.in_url)
        val user = view.findViewById<AppCompatEditText>(com.cobplayer.app.R.id.in_user)
        val pass = view.findViewById<AppCompatEditText>(com.cobplayer.app.R.id.in_pass)
        url.setText(this.syncUrl)
        user.setText(this.syncUser)
        pass.setText(this.syncPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle("WebDAV configuration")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                this.syncUrl = url.text?.toString()?.trim().orEmpty()
                this.syncUser = user.text?.toString()?.trim().orEmpty()
                this.syncPassword = pass.text?.toString().orEmpty()
                refreshStatus()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildConfig(): WebDavClient.Config? {
        val url = this.syncUrl
        if (url.isBlank()) return null
        return WebDavClient.Config(
            baseUrl = url,
            username = this.syncUser,
            password = this.syncPassword
        )
    }

    private fun testConnection() {
        val cfg = buildConfig() ?: return
        binding.btnTest.isEnabled = false
        binding.btnTest.text = "Testing…"
        executor.execute {
            val (ok, msg) = WebDavClient.test(cfg)
            main.post {
                binding.btnTest.isEnabled = true
                binding.btnTest.text = "Test connection"
                toast(if (ok) "Connected" else "Failed: $msg")
            }
        }
    }

    private fun push() {
        val cfg = buildConfig() ?: return
        binding.btnPush.isEnabled = false
        binding.btnPush.text = "Uploading…"
        val json = SyncSnapshot.export(this)
        executor.execute {
            val ok = WebDavClient.upload(cfg, json)
            main.post {
                binding.btnPush.isEnabled = true
                binding.btnPush.text = "Push snapshot"
                if (ok) {
                    this.lastSyncTime = System.currentTimeMillis()
                    refreshStatus()
                    toast("Uploaded")
                } else toast("Upload failed")
            }
        }
    }

    private fun pull() {
        val cfg = buildConfig() ?: return
        binding.btnPull.isEnabled = false
        binding.btnPull.text = "Downloading…"
        executor.execute {
            val json = WebDavClient.download(cfg)
            val merged = if (json != null) SyncSnapshot.import(this, json) else 0
            main.post {
                binding.btnPull.isEnabled = true
                binding.btnPull.text = "Pull snapshot"
                if (json != null) {
                    this.lastSyncTime = System.currentTimeMillis()
                    refreshStatus()
                    toast("Merged $merged entries")
                } else toast("Download failed")
            }
        }
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}
