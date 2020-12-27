package com.redmadrobot.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.redmadrobot.gallery.entity.Media
import com.redmadrobot.gallery.entity.MediaType
import com.redmadrobot.gallery.ui.GalleryFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        recyclerView.apply {
            adapter = MediaAdapter().apply {
                items = listOfMedia
                onItemClick = { position ->
                    GalleryFragment
                            .create(listOfMedia, position)
                            .show(supportFragmentManager, "fragment_tag_gallery")
                }
            }
            setHasFixedSize(true)
        }
    }

    private val listOfMedia = ArrayList(listOf(
            Media(
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                    MediaType.VIDEO,
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            Media(
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
                    MediaType.VIDEO,
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            ),
            Media(
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg",
                    MediaType.VIDEO,
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            ),
            Media(
                    "https://images.pexels.com/photos/459225/pexels-photo-459225.jpeg",
                    MediaType.IMAGE,
                    "https://images.pexels.com/photos/459225/pexels-photo-459225.jpeg"
            ),
            Media(
                    "https://images.pexels.com/photos/257840/pexels-photo-257840.jpeg",
                    MediaType.IMAGE,
                    "https://images.pexels.com/photos/257840/pexels-photo-257840.jpeg"
            ),
            Media(
                    "https://images.pexels.com/photos/39811/pexels-photo-39811.jpeg",
                    MediaType.IMAGE,
                    "https://images.pexels.com/photos/39811/pexels-photo-39811.jpeg"
            ),
            Media(
                    "https://images.pexels.com/photos/1039302/pexels-photo-1039302.jpeg",
                    MediaType.IMAGE,
                    "https://images.pexels.com/photos/1039302/pexels-photo-1039302.jpeg"
            ),
            Media(
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerEscapes.jpg",
                    MediaType.VIDEO,
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
            ),
            Media(
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
                    MediaType.VIDEO,
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            ),
            Media(
                    "https://images.pexels.com/photos/4827/nature-forest-trees-fog.jpeg",
                    MediaType.IMAGE,
                    "https://images.pexels.com/photos/4827/nature-forest-trees-fog.jpeg"
            ),
            Media(
                    "https://images.pexels.com/photos/248771/pexels-photo-248771.jpeg",
                    MediaType.IMAGE,
                    "https://images.pexels.com/photos/248771/pexels-photo-248771.jpeg"
            ),
            Media(
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                    MediaType.VIDEO,
                    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            Media(
                    "https://images.pexels.com/photos/248797/pexels-photo-248797.jpeg",
                    MediaType.IMAGE,
                    "https://images.pexels.com/photos/248797/pexels-photo-248797.jpeg"
            )
    ))
}
