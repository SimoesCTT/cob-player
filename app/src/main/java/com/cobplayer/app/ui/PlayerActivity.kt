package com.cobplayer.app.ui

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.CaptionStyleCompat
import com.cobplayer.app.R
import com.cobplayer.app.data.PlayerPrefs
import com.cobplayer.app.data.PlayerPrefs.aspectMode
import com.cobplayer.app.data.PlayerPrefs.repeatMode
import com.cobplayer.app.data.PlayerPrefs.shuffle
import com.cobplayer.app.data.PlayerPrefs.speed
import com.cobplayer.app.data.RecentStore
import com.cobplayer.app.data.ResumeStore
import com.cobplayer.app.data.SubtitleLoader
import com.cobplayer.app.data.SpeedStore
import com.cobplayer.app.databinding.ActivityPlayerBinding
import com.cobplayer.app.service.PlaybackService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var currentUri: Uri? = null
    private var currentTitle: String? = null
    private var pendingResumeMs: Long = 0
    private var isFullscreen = false
    private var positionSaver: Runnable? = null
    private var currentAspect = AspectRatioController.Mode.FIT
    private var startIndex: Int = 0
    private var videoRotationDegrees: Int = 0
    private var brightness: Float = 1.0f
    private var contrast: Float = 1.0f
    private var saturation: Float = 1.0f
    private var subtitleDelayMs: Long = 0
    private var audioDelayMs: Long = 0
    private var sleepTimer: SleepTimer? = null

    private val positionHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val pickSubtitleLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { attachExternalSubtitle(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyCobSubtitleStyle()
        wireFullscreenButton()
        wireShareButton()
        wireSubtitleButton()
        wireSettingsButton()

        val uris = intent.getStringArrayListExtra("queue_uris")
        val titles = intent.getStringArrayListExtra("queue_titles")
        startIndex = intent.getIntExtra("queue_start_index", 0)
        currentUri = intent?.data
            ?: Uri.parse("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
        currentTitle = intent?.getStringExtra("title") ?: currentUri?.lastPathSegment

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync().also { future ->
            future.addListener({
                val c = future.get()
                controller = c
                setupPlayer(c, uris, titles)
            }, MoreExecutors.directExecutor())
        }

        positionSaver = object : Runnable {
            override fun run() {
                savePosition()
                positionHandler.postDelayed(this, 5000)
            }
        }
        positionHandler.postDelayed(positionSaver!!, 5000)

        currentAspect = try {
            AspectRatioController.Mode.valueOf(this.aspectMode)
        } catch (e: Exception) {
            AspectRatioController.Mode.FIT
        }
    }

    private fun setupPlayer(c: MediaController, uris: ArrayList<String>?, titles: ArrayList<String>?) {
        binding.playerView.player = c
        AspectRatioController.apply(binding.playerView, currentAspect)
        val perFileSpeed = currentUri?.let { SpeedStore.load(this, it) } ?: this.speed
        c.setPlaybackSpeed(perFileSpeed)
        c.repeatMode = this.repeatMode
        c.shuffleModeEnabled = this.shuffle

        c.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && pendingResumeMs > 0) {
                    c.seekTo(pendingResumeMs)
                    pendingResumeMs = 0
                }
            }
            override fun onRepeatModeChanged(repeatMode: Int) { this@PlayerActivity.repeatMode = repeatMode }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { shuffle = shuffleModeEnabled }
        })

        if (!uris.isNullOrEmpty()) {
            val items = uris.mapIndexed { i, u ->
                MediaItem.Builder()
                    .setUri(Uri.parse(u))
                    .setMediaId(titles?.getOrNull(i) ?: "")
                    .build()
            }
            c.setMediaItems(items, startIndex, 0L)
            currentUri = Uri.parse(uris[startIndex])
        } else {
            c.setMediaItem(MediaItem.fromUri(currentUri!!))
            pendingResumeMs = ResumeStore.load(this, currentUri!!)
        }

        c.prepare()
        c.playWhenReady = true
        currentUri?.let { RecentStore.push(this, it, currentTitle ?: "Unknown") }

        // Auto-discover subtitle file matching video name
        currentUri?.let { uri ->
            com.cobplayer.app.data.AutoSubtitle.findFor(this, uri)?.let { subUri ->
                try {
                    val mime = com.cobplayer.app.data.SubtitleLoader.guessMime(subUri)
                    val current = c.currentMediaItem
                    if (current != null) {
                        val subConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                            .setMimeType(mime)
                            .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                            .setLanguage("und")
                            .build()
                        val newItem = current.buildUpon()
                            .setSubtitleConfigurations(listOf(subConfig))
                            .build()
                        c.replaceMediaItem(c.currentMediaItemIndex, newItem)
                        c.seekTo(c.currentMediaItemIndex, 0L)
                        android.util.Log.d("CobPlayer", "Auto-attached subtitle: ${subUri.lastPathSegment}")
                    }
                } catch (e: Exception) {
                    android.util.Log.d("CobPlayer", "Auto-subtitle failed: ${e.message}")
                }
            }
        }
    }

    private fun applyInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.playerView) { v, insets ->
            val bars = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    or androidx.core.view.WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }


    private fun applyCobSubtitleStyle() {
        val style = CaptionStyleCompat(
            Color.parseColor("#FFC107"),
            Color.parseColor("#A02D1B0E"),
            Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            Color.BLACK, null
        )
        binding.playerView.subtitleView?.setStyle(style)
        binding.playerView.subtitleView?.setApplyEmbeddedStyles(true)
    }

    private fun wireFullscreenButton() {
        binding.playerView.findViewById<View?>(androidx.media3.ui.R.id.exo_fullscreen)
            ?.setOnClickListener { toggleFullscreen() }
    }

    private fun wireShareButton() {
        binding.playerView.findViewById<View?>(R.id.btn_share)
            ?.setOnClickListener {
                currentUri?.let { uri ->
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(share, getString(R.string.share)))
                }
            }
    }

    private fun wireSubtitleButton() {
        binding.playerView.findViewById<View?>(androidx.media3.ui.R.id.exo_subtitle)
            ?.setOnClickListener { showSubtitleMenu() }
    }

    private fun wireSettingsButton() {
        val btn = binding.playerView.findViewById<View?>(R.id.btn_settings)
        android.util.Log.d("DEBUG_GEAR", "btn_settings found? ${btn != null}")
        android.util.Log.d("DEBUG_GEAR", "share found? ${binding.playerView.findViewById<View?>(R.id.btn_share) != null}")
        btn?.setOnClickListener { showSettingsMenu() }
    }

    private fun showSubtitleMenu() {
        val options = arrayOf(
            getString(R.string.subtitle_load_file),
            getString(R.string.subtitle_pick_track)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.subtitles)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickSubtitleLauncher.launch(arrayOf(
                        "application/x-subrip", "text/vtt", "text/x-ssa",
                        "application/ttml+xml", "text/*"
                    ))
                    1 -> showTrackSelector()
                }
            }
            .show()
    }

    private fun attachExternalSubtitle(uri: Uri) {
        val c = controller ?: return
        val mime = SubtitleLoader.guessMime(uri)
        val current = c.currentMediaItem ?: return
        val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mime)
            .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
            .setLanguage("und")
            .build()
        val newItem = current.buildUpon()
            .setSubtitleConfigurations(listOf(subConfig) + current.localConfiguration?.subtitleConfigurations.orEmpty())
            .build()
        val idx = c.currentMediaItemIndex
        val pos = c.currentPosition
        c.replaceMediaItem(idx, newItem)
        c.seekTo(idx, pos)
    }

    private fun showTrackSelector() {
        val c = controller ?: return
        TrackSelectorDialog.show(this, c, TrackSelectorDialog.TrackStrings(
            title = "Tracks",
            audioHeader = "Audio",
            subtitleHeader = "Subtitles",
            subtitleOff = "Off",
            subtitleNone = "No embedded subtitles",
            close = "Close"
        ))
    }

    private fun showSettingsMenu() {
        val c = controller ?: return
        val speedOptions = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f)
        val speedLabels = speedOptions.map { "${(it * 100).toInt()}%" }.toTypedArray()

        val options = arrayOf(
            getString(R.string.setting_speed, (c.playbackParameters.speed * 100).toInt()),
            getString(R.string.setting_aspect, currentAspect.label),
            getString(R.string.setting_repeat, repeatLabel(c.repeatMode)),
            getString(R.string.setting_shuffle, if (c.shuffleModeEnabled) "On" else "Off"),
            getString(R.string.setting_loop_ab),
            getString(R.string.setting_sleep),
            getString(R.string.setting_snapshot),
            getString(R.string.setting_equalizer),
            "Subtitle delay: ${subtitleDelayMs} ms",
            "Audio delay: ${audioDelayMs} ms"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.playback_settings)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.setting_speed_dialog)
                        .setItems(speedLabels) { _, i ->
                            val s = speedOptions[i]
                            c.setPlaybackSpeed(s)
                            this.speed = s
                            currentUri?.let { SpeedStore.save(this@PlayerActivity, it, s) }
                        }.show()
                    1 -> {
                        currentAspect = AspectRatioController.cycle(currentAspect)
                        AspectRatioController.apply(binding.playerView, currentAspect)
                        this.aspectMode = currentAspect.name
                    }
                    2 -> {
                        val next = (c.repeatMode + 1) % 3
                        c.repeatMode = next
                    }
                    3 -> c.shuffleModeEnabled = !c.shuffleModeEnabled
                    4 -> toggleAbLoop()
                    5 -> showSleepTimer()
                    6 -> snapshotFrame()
                    7 -> openEqualizer()
                    8 -> frameStep(forward = true)
                    9 -> frameStep(forward = false)
                    10 -> { videoRotationDegrees = (videoRotationDegrees + 90) % 360; applyRotation() }
                    11 -> { videoRotationDegrees = 0; applyRotation() }
                    12 -> {
                        binding.spectrum.visibility =
                            if (binding.spectrum.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    13 -> adjustSubtitleDelay()
                    14 -> adjustAudioDelay()
                }
            }.show()
    }

    private var abStart: Long = -1
    private var abEnd: Long = -1

    private fun toggleAbLoop() {
        val c = controller ?: return
        if (abStart < 0) {
            abStart = c.currentPosition
            toast("A set at ${formatMs(abStart)}")
        } else if (abEnd < 0) {
            abEnd = c.currentPosition
            if (abEnd > abStart) {
                toast("B set at ${formatMs(abEnd)} — looping")
                c.setMediaItem(c.currentMediaItem!!.buildUpon().setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(abStart)
                        .setEndPositionMs(abEnd)
                        .build()
                ).build())
            } else {
                abEnd = -1
                toast("B must be after A")
            }
        } else {
            abStart = -1; abEnd = -1
            c.setMediaItem(c.currentMediaItem!!.buildUpon().setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().build()
            ).build())
            toast("A-B loop cleared")
        }
    }

    private fun repeatLabel(mode: Int): String = when (mode) {
        Player.REPEAT_MODE_ONE -> "One"
        Player.REPEAT_MODE_ALL -> "All"
        else -> "Off"
    }

    private fun showSleepTimer() {
        val mins = intArrayOf(5, 10, 15, 30, 45, 60, 90)
        val labels = mins.map { "$it min" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.setting_sleep)
            .setItems(labels) { _, which ->
                val c = controller ?: return@setItems
                sleepTimer?.stop()
                sleepTimer = SleepTimer(c)
                sleepTimer?.start(mins[which] * 60_000L,
                    onTick = { /* optionally show toast */ },
                    onFire = { toast("Sleep timer fired") })
                toast("Sleep in ${mins[which]} min")
            }.show()
    }

    private fun openEqualizer() {
        EqualizerSheet().show(supportFragmentManager, "eq")
    }

    private fun snapshotFrame() {
        val view = binding.playerView.videoSurfaceView
        if (view == null) {
            toast("No video surface")
            return
        }
        Snapshotter.capture(view) { uri ->
            if (uri != null) toast("Saved: ${uri.lastPathSegment}")
            else toast("Snapshot failed")
        }
    }

    private fun applyRotation() {
        binding.playerView.videoSurfaceView?.let { view ->
            if (view is android.view.SurfaceView) {
                view.rotation = videoRotationDegrees.toFloat()
            } else if (view is android.view.TextureView) {
                view.rotation = videoRotationDegrees.toFloat()
            }
        }
    }

    private fun frameStep(forward: Boolean) {
        val c = controller ?: return
        if (!c.isPlaying) {
            val fps = 30.0
            val delta = (1000.0 / fps).toLong()
            val target = if (forward) c.currentPosition + delta else (c.currentPosition - delta).coerceAtLeast(0)
            c.seekTo(target)
        } else {
            c.pause()
        }
    }

    private fun adjustSubtitleDelay() {
        val options = arrayOf("-500 ms", "-100 ms", "Reset", "+100 ms", "+500 ms")
        val deltas = longArrayOf(-500, -100, 0, 100, 500)
        MaterialAlertDialogBuilder(this)
            .setTitle("Subtitle delay")
            .setItems(options) { _, which ->
                subtitleDelayMs += deltas[which]
                toast("Subtitle delay: ${subtitleDelayMs} ms")
            }
            .show()
    }

    private fun adjustAudioDelay() {
        val options = arrayOf("-500 ms", "-100 ms", "Reset", "+100 ms", "+500 ms")
        val deltas = longArrayOf(-500, -100, 0, 100, 500)
        MaterialAlertDialogBuilder(this)
            .setTitle("Audio delay")
            .setItems(options) { _, which ->
                audioDelayMs += deltas[which]
                toast("Audio delay: ${audioDelayMs} ms")
            }
            .show()
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        requestedOrientation = if (isFullscreen)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (isFullscreen) hideSystemBars() else showSystemBars()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    private fun showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            window.insetsController?.show(WindowInsets.Type.systemBars())
        else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun autoPushSnapshot() {
        val url = com.cobplayer.app.data.SyncConfigStore.run { this@PlayerActivity.syncUrl }
        if (url.isBlank()) return
        val user = com.cobplayer.app.data.SyncConfigStore.run { this@PlayerActivity.syncUser }
        val pass = com.cobplayer.app.data.SyncConfigStore.run { this@PlayerActivity.syncPassword }
        if (user.isBlank()) return
        val json = com.cobplayer.app.data.SyncSnapshot.export(this)
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            com.cobplayer.app.net.WebDavClient.upload(
                com.cobplayer.app.net.WebDavClient.Config(url, user, pass), json
            )
        }
    }

    private fun savePosition() {
        val c = controller ?: return
        val uri = currentUri ?: return
        if (c.duration > 0) ResumeStore.save(this, uri, c.currentPosition, c.duration)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && controller?.isPlaying == true) {
            enterPip()
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9)).build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val landscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        // Adjust controls visibility timing in landscape — controls hide faster
        binding.playerView.controllerAutoShow = !landscape
    }

    override fun onStop() {
        super.onStop()
        savePosition()
        autoPushSnapshot()
        if (!isInPictureInPictureMode) controller?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        positionSaver?.let { positionHandler.removeCallbacks(it) }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    private fun formatMs(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, sec)
               else String.format("%d:%02d", m, sec)
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}
