package com.cobplayer.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.media3.common.util.UnstableApi
import com.cobplayer.app.R
import com.cobplayer.app.audio.EqHolder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class EqualizerSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_equalizer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eq = EqHolder.controller
        val switch = view.findViewById<SwitchCompat>(R.id.eq_switch)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.eq_preset_spinner)
        val bandsContainer = view.findViewById<android.widget.LinearLayout>(R.id.eq_bands_container)
        val unavailable = view.findViewById<TextView>(R.id.eq_unavailable)

        if (eq == null || !eq.isAvailable) {
            switch.isEnabled = false
            spinner.isEnabled = false
            unavailable.visibility = View.VISIBLE
            return
        }

        switch.isChecked = eq.enabled
        switch.setOnCheckedChangeListener { _, isChecked -> eq.enabled = isChecked }

        // Presets
        val presets = eq.presets()
        val presetNames = presets.map { it.first }
        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            presetNames
        )
        eq.currentPreset()?.let { current ->
            val idx = presets.indexOfFirst { it.second == current }
            if (idx >= 0) spinner.setSelection(idx)
        }
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                presets.getOrNull(pos)?.let { eq.usePreset(it.second) }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Bands
        val range = eq.bandRange() ?: run {
            unavailable.visibility = View.VISIBLE
            return
        }
        val minLevel = range.first.toInt()
        val maxLevel = range.second.toInt()
        val count = eq.bandCount()

        bandsContainer.removeAllViews()
        for (i in 0 until count) {
            val bandView = layoutInflater.inflate(R.layout.item_eq_band, bandsContainer, false)
            val freq = bandView.findViewById<TextView>(R.id.band_freq)
            val levelLabel = bandView.findViewById<TextView>(R.id.band_level)
            val seek = bandView.findViewById<SeekBar>(R.id.band_seek)

            val freqHz = eq.bandFreqHz(i)
            freq.text = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz"

            seek.max = maxLevel - minLevel
            val currentLevel = eq.bandLevel(i).toInt()
            seek.progress = currentLevel - minLevel
            levelLabel.text = "$currentLevel dB"

            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val level = (progress + minLevel).toShort()
                    eq.setBandLevel(i, level)
                    levelLabel.text = "$level dB"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            bandsContainer.addView(bandView)
        }
    }
}
