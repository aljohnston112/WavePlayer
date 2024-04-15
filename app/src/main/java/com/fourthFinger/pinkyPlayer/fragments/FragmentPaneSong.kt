package com.fourthFinger.pinkyPlayer.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.fourthFinger.pinkyPlayer.BitmapUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentPaneSongBinding
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri

class FragmentPaneSong : Fragment() {

    private var _binding: FragmentPaneSongBinding? = null
    private val binding get() = _binding!!

    private val viewModelFragmentPaneSong by viewModels<ViewModelFragmentPaneSong>{
        ViewModelFragmentPaneSong.Factory
    }
    private val viewModelFragmentSong by viewModels<ViewModelFragmentSong> {
        ViewModelFragmentSong.Factory
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
        viewModelFragmentSong.currentAudioUri.observe(viewLifecycleOwner) { currentAudioUri: AudioUri? ->
            updateSongUI(currentAudioUri)
        }
        viewModelFragmentSong.isPlaying.observe(viewLifecycleOwner) { isPlaying: Boolean? ->
            if (isPlaying != null) {
                setUpPlayButton(isPlaying)
            }
        }
    }

    private val onLayoutChangeListenerSongPane =
        { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
                songArtWidth = binding.imageViewSongPaneSongArt.width
                setUpPrev()
                setUpPlayButton(viewModelFragmentSong.isPlaying.value?:false)
                setUpNext()
        }

    private fun updateSongUI(currentAudioUri: AudioUri?) {
        if (currentAudioUri != null) {
            binding.textViewSongPaneSongName.text = currentAudioUri.title
            binding.imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater)
        }
    }

    private var runnableSongPaneArtUpdater = Runnable {
        val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
        if (songArtWidth > 0) {
            val bitmapSongArt: Bitmap? = BitmapUtil.getThumbnailBitmap(
                viewModelFragmentSong.currentAudioUri.value,
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

    private val onClickListenerSongPane = View.OnClickListener { v: View ->
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
        _binding = null
        removeListeners()
    }

    private fun removeListeners() {
        requireView().removeOnLayoutChangeListener(onLayoutChangeListenerSongPane)
    }

}