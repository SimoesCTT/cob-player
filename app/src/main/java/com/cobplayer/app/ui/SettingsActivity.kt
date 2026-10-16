package com.cobplayer.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.cobplayer.app.R
import com.cobplayer.app.data.SettingsStore.seekSeconds
import com.cobplayer.app.data.SettingsStore.showHiddenFiles
import com.cobplayer.app.data.SettingsStore.groupByFolder
import com.cobplayer.app.data.SettingsStore.showThumbnails
import com.cobplayer.app.data.SettingsStore.sleepTimerDefaultMin
import com.cobplayer.app.data.SettingsStore.sortMode
import com.cobplayer.app.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@UnstableApi
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchThumbs.isChecked = this.showThumbnails
        binding.switchHidden.isChecked = this.showHiddenFiles
        binding.switchGroup.isChecked = this.groupByFolder
        binding.rowSortSubtitle.text = sortLabel(this.sortMode)
        binding.rowSeekSubtitle.text = "${this.seekSeconds}s"
        binding.rowSleepSubtitle.text = "${this.sleepTimerDefaultMin} min"

        binding.switchThumbs.setOnCheckedChangeListener { _, v -> this.showThumbnails = v }
        binding.switchHidden.setOnCheckedChangeListener { _, v -> this.showHiddenFiles = v }
        binding.switchGroup.setOnCheckedChangeListener { _, v -> this.groupByFolder = v }

        binding.rowSort.setOnClickListener { pickSort() }
        binding.rowSeek.setOnClickListener { pickSeek() }
        binding.rowSleep.setOnClickListener { pickSleep() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun pickSort() {
        val modes = arrayOf("date_desc", "date_asc", "name_asc", "name_desc", "size_desc", "duration_desc")
        val labels = arrayOf("Newest first", "Oldest first", "Name A→Z", "Name Z→A", "Largest first", "Longest first")
        val current = modes.indexOf(this.sortMode).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(labels, current) { d, which ->
                this.sortMode = modes[which]
                binding.rowSortSubtitle.text = labels[which]
                d.dismiss()
            }.show()
    }

    private fun pickSeek() {
        val opts = intArrayOf(5, 10, 15, 30)
        val labels = opts.map { "${it}s" }.toTypedArray()
        val current = opts.indexOf(this.seekSeconds).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.seek_duration)
            .setSingleChoiceItems(labels, current) { d, which ->
                this.seekSeconds = opts[which]
                binding.rowSeekSubtitle.text = labels[which]
                d.dismiss()
            }.show()
    }

    private fun pickSleep() {
        val opts = intArrayOf(5, 10, 15, 30, 45, 60, 90)
        val labels = opts.map { "$it min" }.toTypedArray()
        val current = opts.indexOf(this.sleepTimerDefaultMin).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sleep_timer_default)
            .setSingleChoiceItems(labels, current) { d, which ->
                this.sleepTimerDefaultMin = opts[which]
                binding.rowSleepSubtitle.text = labels[which]
                d.dismiss()
            }.show()
    }

    private fun sortLabel(mode: String): String = when (mode) {
        "date_asc" -> "Oldest first"
        "name_asc" -> "Name A→Z"
        "name_desc" -> "Name Z→A"
        "size_desc" -> "Largest first"
        "duration_desc" -> "Longest first"
        else -> "Newest first"
    }
}
