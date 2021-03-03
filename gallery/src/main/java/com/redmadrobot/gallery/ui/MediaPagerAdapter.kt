package com.redmadrobot.gallery.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.valueIterator
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.redmadrobot.gallery.R
import com.redmadrobot.gallery.entity.Media
import com.redmadrobot.gallery.entity.MediaType
import com.redmadrobot.gallery.ui.custom.ExoPlayerView
import java.util.*

internal class MediaPagerAdapter(
        private val listOfMedia: List<Media>,
        context: Context,
        exoPlayerCallbacks: ExoPlayerCallbacks,
        onPlayerControllerVisibilityListener: (Boolean) -> Unit,
        onImageZoomListener: (isZoomed: Boolean) -> Unit
) : PagerAdapter() {

    private val mediaPagePool = MediaPagePool(
            context,
            ExoPlayerFactory(context, exoPlayerCallbacks),
            onPlayerControllerVisibilityListener,
            onImageZoomListener
    )
    private val mediaPagesInUse = SparseArray<MediaPage>()

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val media = listOfMedia[position]
        val mediaPage = when (media.type) {
            MediaType.VIDEO -> mediaPagePool.getVideoPage().apply { this.media = media }
            MediaType.IMAGE -> mediaPagePool.getImagePage().apply { this.media = media }
        }
        container.addView(mediaPage.view)
        mediaPagesInUse.put(position, mediaPage)
        return mediaPage
    }

    override fun destroyItem(container: ViewGroup, position: Int, key: Any) {
        val mediaPage = (key as MediaPage)
        container.removeView(mediaPage.view)
        mediaPagesInUse.remove(position)
        mediaPagePool.releaseMediaPage(mediaPage)
    }

    override fun isViewFromObject(view: View, key: Any): Boolean = ((key as MediaPage).view == view)

    override fun getCount(): Int = listOfMedia.size

    private var lastPrimaryItem = -1 // Initially invalid.

    override fun setPrimaryItem(container: ViewGroup, position: Int, key: Any) {
        // It's crucial to make this method idempotent.
        // It is called multiple times when the player controller is visible.
        // This causes the pause button to not work.
        if (position != lastPrimaryItem) {
            lastPrimaryItem = position
            (key as? VideoPage)?.startOrResume()
            for (mediaPage in mediaPagesInUse.valueIterator()) {
                if (mediaPage is ImagePage && mediaPage !== key) {
                    mediaPage.resetScale()
                }
            }
        }
    }

    fun resumeVideo(position: Int) {
        if (lastPrimaryItem == position) {
            (mediaPagesInUse[position] as? VideoPage)?.startOrResume()
        }
    }

    fun pauseVideoAndHideController() {
        for (mediaPage in mediaPagesInUse.valueIterator()) {
            (mediaPage as? VideoPage)?.apply {
                pause()
                hideController()
            }
        }
    }

    fun clear() {
        for (mediaPage in mediaPagesInUse.valueIterator()) {
            (mediaPage as? VideoPage)?.releasePlayer()
        }
        mediaPagesInUse.clear()
        mediaPagePool.clear()
    }
}

/**
 * Abstraction over gallery page. Subclasses are responsible for view creation, media binding/unbinding and media
 * control.
 */
private sealed class MediaPage {
    abstract val view: View
    abstract var media: Media?
}

private class VideoPage(
        val context: Context,
        private val exoPlayerWrapper: ExoPlayerWrapper,
        onPlayerControllerVisibilityListener: (Boolean) -> Unit
) : MediaPage() {

    private var mExoPlayerFullscreen = false
    private var mFullScreenButton: FrameLayout
    private var mainLayout: ConstraintLayout
    private var videoWidth = 0
    private var videoHeight = 0
    private var mFullScreenIcon: ImageView
    val retriever = MediaMetadataRetriever()

    override val view: PlayerView = (LayoutInflater.from(context).inflate(R.layout.player_view, null) as ExoPlayerView).apply {
        exoPlayerWrapper.attachTo(this)
        controllerAutoShow = false
        controllerHideOnTouch = false

        hideController()
        setControllerVisibilityListener { visibility ->
            onPlayerControllerVisibilityListener((visibility == View.VISIBLE))
        }
        mFullScreenIcon = findViewById(R.id.exo_fullscreen_icon)
        mFullScreenButton = findViewById(R.id.exo_fullscreen_button)

        mainLayout = GalleryFragment.mainLayout

        mFullScreenButton.setOnClickListener {
            if (!mExoPlayerFullscreen) {
                openFullscreenDialog()
            } else {
                closeFullscreenDialog()
            }
        }
    }

    override var media: Media? = null
        set(value) {
            field = value
            value?.let {
                try {
                    retriever.setDataSource(Uri.parse(value.url).toString(), hashMapOf<String, String>())
                    videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
                    videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
                    setFullscreenButtonVisibility(videoWidth > videoHeight)
                } catch (e: Exception) {
                    setFullscreenButtonVisibility(true)
                }
            }
            when (value) {
                null -> exoPlayerWrapper.pause()
                else -> exoPlayerWrapper.setMediaSource(value.url)
            }
        }

    fun setFullscreenButtonVisibility(visible: Boolean) {
        mFullScreenIcon.visibility = if (visible) View.VISIBLE else View.GONE
        mFullScreenButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun startOrResume() = exoPlayerWrapper.startOrResume()

    fun pause() = exoPlayerWrapper.pause()

    fun hideController() = view.hideController()

    fun releasePlayer() = exoPlayerWrapper.release()

    private fun openFullscreenDialog() {
        doLandScape()
        mFullScreenIcon.setImageDrawable(
                ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_fullscreen_skrink
                )
        )
        mExoPlayerFullscreen = true
    }

    private fun closeFullscreenDialog() {
        doPortrait()
        mFullScreenIcon.setImageDrawable(
                ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_fullscreen_expand
                )
        )
        mExoPlayerFullscreen = false
    }

    fun doLandScape() {
        val w = mainLayout.width
        val h = mainLayout.height

        mainLayout.apply {
            rotation = 90.0f
            translationX = (w - h) / 2.toFloat()
            translationY = (h - w) / 2.toFloat()
        }

        val lp = mainLayout.layoutParams as ViewGroup.LayoutParams
        lp.height = w
        lp.width = h
        mainLayout.requestLayout()
    }

    private fun doPortrait() {
        val w = mainLayout.width
        val h = mainLayout.height

        mainLayout.apply {
            rotation = 0f
            translationX = 0f
            translationY = 0f
        }

        val lp = mainLayout.layoutParams as ViewGroup.LayoutParams
        lp.height = w
        lp.width = h
        mainLayout.requestLayout()
    }
}

private class ImagePage(
        context: Context,
        private val onImageZoomListener: (isZoomed: Boolean) -> Unit
) : MediaPage() {

    override val view: PhotoView = PhotoView(context).apply {
        minimumScale = 1.0F
        maximumScale = 2.0F
        setOnScaleChangeListener { _, _, _ -> onImageZoomListener(scale > 1.05F) }
    }

    override var media: Media? = null
        set(value) {
            field = value
            when (value) {
                null -> Glide.with(view).clear(view)
                else -> Glide.with(view).load(value.thumbnailUrl).into(view)
            }
        }

    fun resetScale() {
        view.setScale(1.0F, false)
        onImageZoomListener(false)
    }
}

/**
 * This class is a base for page view (players and other objects) recycling mechanism.
 */
private class MediaPagePool(
        private val context: Context,
        private val playerFactory: ExoPlayerFactory,
        private val onPlayerControllerVisibilityListener: (Boolean) -> Unit,
        private val onImageZoomListener: (isZoomed: Boolean) -> Unit
) {

    private val videoPagePool: Queue<VideoPage> = LinkedList<VideoPage>()
    private val imagePagePool: Queue<ImagePage> = LinkedList<ImagePage>()

    fun getVideoPage(): VideoPage =
            videoPagePool.poll()
                    ?: VideoPage(
                            context,
                            playerFactory.createPlayer(),
                            onPlayerControllerVisibilityListener
                    )

    fun getImagePage(): ImagePage =
            imagePagePool.poll()
                    ?: ImagePage(context, onImageZoomListener)

    fun releaseMediaPage(mediaPage: MediaPage) = when (mediaPage) {
        is VideoPage -> {
            mediaPage.media = null
            videoPagePool.offer(mediaPage)
        }
        is ImagePage -> {
            mediaPage.media = null
            imagePagePool.offer(mediaPage)
        }
    }

    fun clear() {
        videoPagePool.forEach { it.releasePlayer() }
        videoPagePool.clear()
        imagePagePool.clear()
    }
}

/**
 * ExoPlayer instance and ExtractorMediaSource.Factory instance are bind with the same BandwidthMeter instance.
 * We need to keep a reference to ExtractorMediaSource.Factory instance to be able to change video URL.
 * Also this class gathered together all methods we need to work with the player.
 * Do not expose reference to the player to not allow abuse of its usage, hence to lower code entanglement.
 */
private class ExoPlayerWrapper(
        private val exoPlayer: ExoPlayer,
        private val mediaSourceFactory: ExtractorMediaSource.Factory,
        private val exoPlayerCallbacks: ExoPlayerCallbacks
) {

    fun attachTo(playerView: PlayerView) {
        playerView.player = exoPlayer
    }

    fun startOrResume() {
        exoPlayer.playWhenReady = true
        exoPlayerCallbacks.isStarted(true)
        exoPlayerCallbacks.isReleased(false, exoPlayer.duration)
    }

    fun pause() {
        exoPlayer.playWhenReady = false
        exoPlayerCallbacks.isStarted(false)
    }

    fun setMediaSource(url: String) {
        exoPlayer.prepare(mediaSourceFactory.createMediaSource(Uri.parse(url)))
    }

    fun release() {
        exoPlayer.release()
        exoPlayerCallbacks.isStarted(false)
        if (exoPlayer.currentPosition > 0) exoPlayerCallbacks.isReleased(true, exoPlayer.currentPosition)
    }
}

private class ExoPlayerFactory(private val context: Context, private val exoPlayerCallbacks: ExoPlayerCallbacks) {

    private val userAgent: String = Util.getUserAgent(context, "Gallery")

    fun createPlayer(): ExoPlayerWrapper {
        val bandwidthMeter = DefaultBandwidthMeter()
        return ExoPlayerWrapper(
                com.google.android.exoplayer2.ExoPlayerFactory.newSimpleInstance(
                        context,
                        DefaultTrackSelector(
                                AdaptiveTrackSelection.Factory(bandwidthMeter)
                        )
                ),
                ExtractorMediaSource.Factory(
                        DefaultDataSourceFactory(
                                context,
                                userAgent,
                                bandwidthMeter
                        )
                ),
                exoPlayerCallbacks
        )
    }
}

interface ExoPlayerCallbacks {
    fun isStarted(isStarted: Boolean)
    fun isReleased(isReleased: Boolean, timeWhenReleased: Long)
}