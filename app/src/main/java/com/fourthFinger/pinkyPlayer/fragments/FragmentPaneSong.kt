package com.fourthFinger.pinkyPlayer.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.fragment.app.activityViewModels
import com.fourthFinger.pinkyPlayer.BitmapUtil
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentPaneSongBinding
import com.fourthFinger.pinkyPlayer.media_controller.MediaModel
import com.fourthFinger.pinkyPlayer.media_controller.MediaModel.Companion.getInstance
import com.fourthFinger.pinkyPlayer.media_controller.MediaPlayerModel
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri

class FragmentPaneSong : Fragment() {

    private var _binding: FragmentPaneSongBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()

    private var mediaModel: MediaModel = getInstance(requireActivity().applicationContext)
    private val mediaPlayerModel = MediaPlayerModel.getInstance()

    private var songArtWidth = 0
    private var visible = false

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
    }

    private val onLayoutChangeListenerSongPane =
        { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
            if (visible) {
                if (songArtWidth == 0) {
                    songArtWidth = binding.imageViewSongPaneSongArt.width
                }
                // TODO is this needed?
                updateSongUI(mediaPlayerModel.currentAudioUri.value)
                setUpPrev()
                setUpNext()
            }
        }

    fun updateSongUI(currentAudioUri: AudioUri?) {
        if (currentAudioUri != null && visible && mediaModel.isSongInProgress()) {
            binding.textViewSongPaneSongName.text = currentAudioUri.title
            binding.imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater)
        }
    }

    private var runnableSongPaneArtUpdater = Runnable {
        val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
        val bitmapSongArt: Bitmap? = BitmapUtil.getBitmap(
            mediaPlayerModel.currentAudioUri.value,
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

    private fun setUpPrev() {
        val imageView: ImageView = binding.imageButtonSongPanePrev
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            requireActivity().resources,
            R.drawable.skip_previous_black_24dp,
            requireActivity().theme
        )
        if (drawable != null && songArtWidth > 0) {
            // TODO Make sure this works
            imageView.setImageBitmap(drawable.toBitmap(songArtWidth, songArtWidth))
            /*
                val bitmap: Bitmap = Bitmap.createBitmap(
                    songArtWidth,
                    songArtWidth,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, songArtWidth, songArtWidth)
                drawable.draw(canvas)
                imageView.setImageBitmap(bitmap)
             */
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
            /*
                drawable.setBounds(0, 0, songArtWidth, songArtWidth)
                val bitmap: Bitmap =
                    Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.draw(canvas)
                imageView.setImageBitmap(bitmap)
             */
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
            // TODO as above
            imageView.setImageBitmap(drawable.toBitmap(songArtWidth, songArtWidth))
            /*
            drawable.setBounds(0, 0, songArtWidth, songArtWidth)
            val bitmap: Bitmap =
                Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.draw(canvas)
            imageView.setImageBitmap(bitmap)
             */
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
            if (v.id == R.id.imageButtonSongPaneNext) {
                mediaModel.playNext(v.context)
            } else if (v.id == R.id.imageButtonSongPanePlayPause) {
                mediaModel.pauseOrPlay(v.context)
            } else if (v.id == R.id.imageButtonSongPanePrev) {
                mediaModel.playPrevious(v.context)
            } else if (v.id == R.id.textViewSongPaneSongName ||
                v.id == R.id.imageViewSongPaneSongArt
            ) {
                // TODO make sure this works. Might need the Activity Fragment Container
                NavUtil.navigateTo(this, R.id.fragmentSong)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setUpBroadcastReceiver()
    }

    private fun setUpBroadcastReceiver() {
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(
            requireActivity().resources.getString(
                R.string.broadcast_receiver_action_service_connected
            )
        )
        filterComplete.addAction(
            requireActivity().resources.getString(
                R.string.broadcast_receiver_action_loaded
            )
        )
        requireActivity().registerReceiver(broadcastReceiver, filterComplete)
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // TODO see if updateSongUi is really needed
            val action: String? = intent.action
            if (action != null) {
                if (action == resources.getString(
                        R.string.broadcast_receiver_action_service_connected
                    )
                ) {
                    updateSongUI(mediaPlayerModel.currentAudioUri.value)
                } else if (action == resources.getString(
                        R.string.broadcast_receiver_action_loaded
                    )
                ) {
                    // TODO can this not be done onViewCreated?
                    setUpObservers()
                }
            }
        }
    }

    private fun setUpObservers() {
        mediaPlayerModel.currentAudioUri.observe(viewLifecycleOwner) { currentAudioUri: AudioUri? ->
            // TODO make sure this works as intended
            updateSongUI(currentAudioUri)
        }
        mediaPlayerModel.isPlaying.observe(viewLifecycleOwner) { isPlaying: Boolean? ->
            if (isPlaying != null) {
                setUpPlayButton(isPlaying)
            }
        }
        viewModelActivityMain.songPaneVisible.observe(viewLifecycleOwner) {
            visible = it
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
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