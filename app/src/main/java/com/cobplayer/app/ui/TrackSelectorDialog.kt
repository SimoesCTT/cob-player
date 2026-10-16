package com.cobplayer.app.ui

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.TrackSelectionParameters
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@UnstableApi
object TrackSelectorDialog {

    fun show(context: Context, player: Player, strings: TrackStrings) {
        val tracks = player.currentTracks
        val audioTracks = flatten(tracks, C.TRACK_TYPE_AUDIO)
        val textTracks = flatten(tracks, C.TRACK_TYPE_TEXT)

        val labels = mutableListOf<String>()
        val actions = mutableListOf<Pair<String, () -> Unit>>()

        // Audio section
        if (audioTracks.isNotEmpty()) {
            labels += "── ${strings.audioHeader} ──"
            actions += "" to {}
            audioTracks.forEach { entry ->
                val isSelected = entry.isSelected
                val mark = if (isSelected) "✓ " else "   "
                labels += "$mark${entry.label}"
                actions += entry.label to {
                    val override = TrackSelectionOverride(entry.group, listOf(entry.index))
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(override)
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                        .build()
                }
            }
        }

        // Subtitle section
        if (textTracks.isNotEmpty()) {
            labels += "── ${strings.subtitleHeader} ──"
            actions += "" to {}
            textTracks.forEach { entry ->
                val isSelected = entry.isSelected
                val mark = if (isSelected) "✓ " else "   "
                labels += "$mark${entry.label}"
                actions += entry.label to {
                    val override = TrackSelectionOverride(entry.group, listOf(entry.index))
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(override)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .build()
                }
            }
            labels += "   ${strings.subtitleOff}"
            actions += strings.subtitleOff to {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            }
        } else {
            labels += "── ${strings.subtitleHeader} ──"
            actions += "" to {}
            labels += "   ${strings.subtitleNone}"
            actions += "" to {}
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(strings.title)
            .setItems(labels.toTypedArray()) { _, which ->
                actions.getOrNull(which)?.second?.invoke()
            }
            .setNegativeButton(strings.close, null)
            .show()
    }

    private data class Entry(
        val group: TrackGroup,
        val index: Int,
        val label: String,
        val isSelected: Boolean
    )

    private fun flatten(tracks: Tracks, type: Int): List<Entry> {
        val out = mutableListOf<Entry>()
        tracks.groups.forEach { group ->
            if (group.type != type) return@forEach
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = format.label
                    ?: format.language
                    ?: format.codecs
                    ?: "Track ${out.size + 1}"
                out += Entry(group.mediaTrackGroup, i, label, group.isTrackSelected(i))
            }
        }
        return out
    }

    data class TrackStrings(
        val title: String,
        val audioHeader: String,
        val subtitleHeader: String,
        val subtitleOff: String,
        val subtitleNone: String,
        val close: String
    )
}
