package com.redmadrobot.gallery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.redmadrobot.gallery.R
import com.redmadrobot.gallery.entity.Media
import com.redmadrobot.gallery.util.argument
import kotlinx.android.synthetic.main.fragment_gallery.*
import kotlin.math.min

class GalleryFragment : DialogFragment(), ExoPlayerCallbacks {

    companion object {
        private const val STATE_LAST_CHECKED_ITEM_INDEX = "state_last_checked_item_index"
        private const val ARG_LIST_OF_MEDIA = "arg_list_of_media"
        private const val ARG_INITIALLY_CHECKED_ITEM_INDEX = "arg_initially_checked_item_index"

        lateinit var mainLayout: ConstraintLayout
        var activityGallery: FragmentActivity? = null
        var isStarted = MutableLiveData<Boolean>().apply { value = false }
        var isReleased = MutableLiveData<Boolean>().apply { value = false }
        var seekTo = MutableLiveData<Long>().apply { value = 0 }

        fun create(
                list: ArrayList<Media>,
                initiallyCheckedItemIndex: Int
        ): GalleryFragment =
                GalleryFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(ARG_LIST_OF_MEDIA, list)
                        putInt(ARG_INITIALLY_CHECKED_ITEM_INDEX, initiallyCheckedItemIndex)
                    }
                }
    }

    private val listOfMedia: ArrayList<Media> by argument(ARG_LIST_OF_MEDIA)
    private val initiallyCheckedItemIndex: Int by argument(ARG_INITIALLY_CHECKED_ITEM_INDEX)
    private var lastCheckedItemIndex: Int = 0
    private lateinit var mediaViewController: MediaViewController

    private val dismissPathLength by lazy { resources.getDimensionPixelSize(R.dimen.dismiss_path_length) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullScreen)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? =
            inflater.inflate(R.layout.fragment_gallery, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainLayout = main as ConstraintLayout
        activityGallery = activity
        dragLayout.setOnDragListener { dy ->
            backgroundColorView.alpha = 1 - min(Math.abs(dy / (3 * dismissPathLength)), 1f)
            viewPager.translationY = -dy
        }
        dragLayout.setOnReleaseDragListener { dy ->
            if (Math.abs(dy) > dismissPathLength) {
                viewPager.isVisible = false
                dismiss()
            } else {
                backgroundColorView.alpha = 1f
                viewPager.translationY = 0f
            }
        }

        lastCheckedItemIndex = savedInstanceState?.getInt(STATE_LAST_CHECKED_ITEM_INDEX)
                ?: initiallyCheckedItemIndex

        mediaViewController = MediaViewController(
                viewPager = viewPager,
                exoPlayerCallbacks = this,
                onCurrentItemChangeListener = { index ->
                    lastCheckedItemIndex = index
                },
                onPlayerControllerVisibilityListener = {},
                onImageZoomListener = { isZoomed -> dragLayout.draggingIsEnabled = !isZoomed }
        ).apply {
            bind(listOfMedia)
            setCurrentItemIndex(lastCheckedItemIndex)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaViewController.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_LAST_CHECKED_ITEM_INDEX, lastCheckedItemIndex)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        applyWindowFullscreenStyle()
        super.onResume()
    }

    private fun applyWindowFullscreenStyle() {
        dialog?.window?.apply {
            attributes = attributes?.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
            }
        }
    }

    override fun isStarted(isStarted: Boolean) {
        GalleryFragment.isStarted.value = isStarted
    }

    override fun isReleased(isReleased: Boolean, timeWhenReleased: Long) {
        GalleryFragment.isReleased.value = isReleased
        if (isReleased) seekTo.value = timeWhenReleased
    }
}
