package io.fourth_finger.pinky_player.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.activity_main.ActivityMain
import io.fourth_finger.pinky_player.activity_main.ViewModelActivityMain
import io.fourth_finger.pinky_player.databinding.FragmentPaneSongBinding
import io.fourth_finger.playlist_data_source.BitmapUtil

class FragmentPaneSong : Fragment() {

    private var _binding: FragmentPaneSongBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>{
        ViewModelActivityMain.Factory
    }

    private val viewModelFragmentPaneSong by viewModels<ViewModelFragmentPaneSong>{
        ViewModelFragmentPaneSong.Factory
    }

    private var songArtWidth = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaneSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane)
        linkSongPaneButtons()
        setUpObservers()
    }

    private fun setUpObservers() {
        viewModelActivityMain.currentAudioUri.observe(viewLifecycleOwner) { currentAudioUri: io.fourth_finger.playlist_data_source.AudioUri? ->
            updateSongUI(currentAudioUri)
        }
        viewModelActivityMain.isPlaying.observe(viewLifecycleOwner) { isPlaying: Boolean? ->
            if (isPlaying != null) {
                setUpPlayButton(isPlaying)
            }
        }
    }

    private var onLayoutChangeListenerSongPane: OnLayoutChangeListener? =
        OnLayoutChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
                songArtWidth = binding.imageViewSongPaneSongArt.width
                setUpPrev()
                setUpPlayButton(viewModelActivityMain.isPlaying.value?:false)
                setUpNext()
        }

    private fun updateSongUI(currentAudioUri: io.fourth_finger.playlist_data_source.AudioUri?) {
        if (currentAudioUri != null) {
            binding.textViewSongPaneSongName.text = currentAudioUri.title
            binding.imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater)
        }
    }

    private var runnableSongPaneArtUpdater: Runnable? = Runnable {
        val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
        if (songArtWidth > 0) {
            val bitmapSongArt: Bitmap? = BitmapUtil.getThumbnailBitmap(
                viewModelActivityMain.currentAudioUri.value,
                songArtWidth,
                requireActivity().applicationContext
            )
            if (bitmapSongArt != null) {
                imageViewSongPaneSongArt.setImageBitmap(bitmapSongArt)
            } else {
                imageViewSongPaneSongArt.setImageBitmap(
                    BitmapUtil.getDefaultBitmap(
                        songArtWidth,
                        requireActivity().applicationContext
                    )
                )
            }
        }
    }

    private fun setUpPrev() {
        val imageView: ImageView = binding.imageButtonSongPanePrev
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            requireActivity().resources,
            R.drawable.skip_previous_black_24dp,
            requireActivity().theme
        )
        if (drawable != null && songArtWidth > 0) {
            imageView.setImageBitmap(drawable.toBitmap(songArtWidth, songArtWidth))
        }
    }

    private fun setUpPlayButton(isPlaying: Boolean) {
        val imageView: ImageView = binding.imageButtonSongPanePlayPause
        val drawable: Drawable? = if (isPlaying) {
            ResourcesCompat.getDrawable(
                resources, R.drawable.pause_black_24dp, requireActivity().theme
            )
        } else {
            ResourcesCompat.getDrawable(
                resources, R.drawable.play_arrow_black_24dp, requireActivity().theme
            )
        }

        if (drawable != null && songArtWidth > 0) {
            imageView.setImageBitmap(drawable.toBitmap(songArtWidth, songArtWidth))
        }
    }

    private fun setUpNext() {
        val imageView: ImageView = binding.imageButtonSongPaneNext
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            requireActivity().resources,
            R.drawable.skip_next_black_24dp,
            requireActivity().theme
        )
        if (drawable != null && songArtWidth > 0) {
            imageView.setImageBitmap(drawable.toBitmap(songArtWidth, songArtWidth))
        }
    }

    private fun linkSongPaneButtons() {
        binding.imageButtonSongPaneNext.setOnClickListener(onClickListenerSongPane)
        binding.imageButtonSongPanePlayPause.setOnClickListener(onClickListenerSongPane)
        binding.imageButtonSongPanePrev.setOnClickListener(onClickListenerSongPane)
        binding.textViewSongPaneSongName.setOnClickListener(onClickListenerSongPane)
        binding.imageViewSongPaneSongArt.setOnClickListener(onClickListenerSongPane)
    }

    private var onClickListenerSongPane: View.OnClickListener? = View.OnClickListener { v: View ->
        synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
            val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
            if (fragment != null) {
                viewModelFragmentPaneSong.clicked(
                    fragment,
                    v.id
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.imageButtonSongPaneNext.setOnClickListener(null)
        binding.imageButtonSongPanePlayPause.setOnClickListener(null)
        binding.imageButtonSongPanePrev.setOnClickListener(null)
        binding.textViewSongPaneSongName.setOnClickListener(null)
        binding.imageViewSongPaneSongArt.setOnClickListener(null)
        _binding = null
        removeListeners()
        runnableSongPaneArtUpdater = null
        onLayoutChangeListenerSongPane = null
        onClickListenerSongPane = null
    }

    private fun removeListeners() {
        requireView().removeOnLayoutChangeListener(onLayoutChangeListenerSongPane)
    }

}