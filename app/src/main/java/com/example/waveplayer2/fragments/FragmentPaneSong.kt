package com.example.waveplayer2.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.example.waveplayer2.R
import com.example.waveplayer2.activity_main.ActivityMain
import com.example.waveplayer2.activity_main.ViewModelActivityMain
import com.example.waveplayer2.databinding.FragmentPaneSongBinding
import com.example.waveplayer2.media_controller.BitmapLoader
import com.example.waveplayer2.media_controller.MediaController
import com.example.waveplayer2.media_controller.MediaController.Companion.getInstance
import com.example.waveplayer2.random_playlist.AudioUri

class FragmentPaneSong : Fragment() {

    private var _binding: FragmentPaneSongBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private lateinit var mediaController: MediaController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaController = getInstance(requireActivity().applicationContext)
    }

    private val onDestinationChangedListenerSongPane = { _: NavController?, destination: NavDestination?, _: Bundle? ->
        val activityMain = requireActivity() as ActivityMain
        if (destination?.id != R.id.fragmentSong) {
            if (mediaController.isSongInProgress()) {
                activityMain.fragmentSongVisible(false)
                activityMain.showSongPane()
            }
        } else {
            activityMain.fragmentSongVisible(true)
            activityMain.hideSongPane()
        }
    }

    private val onLayoutChangeListenerSongPane = { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
        if (requireActivity().findViewById<View>(R.id.fragmentSongPane).visibility == View.VISIBLE) {
            if (songArtWidth == 0) {
                val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
                songArtWidth = imageViewSongPaneSongArt.width
            }
            updateSongUI(mediaController.currentAudioUri.value)
            setUpPrev()
            setUpNext()
        }
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // TODO
            val action: String? = intent.action
            if (action != null) {
                if (action == resources.getString(
                                R.string.broadcast_receiver_action_service_connected)) {
                    updateSongUI(mediaController.currentAudioUri.value)
                } else if (action == resources.getString(
                                R.string.broadcast_receiver_action_loaded)) {
                    setUpObservers()
                }
            }
        }
    }

    private var runnableSongPaneArtUpdater: Runnable? = null

    private var songArtWidth = 0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaneSongBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    private fun setUpObservers() {
        mediaController.currentAudioUri.observe(viewLifecycleOwner) { currentAudioUri: AudioUri? ->
            updateSongUI(currentAudioUri)
        }
        mediaController.isPlaying.observe(viewLifecycleOwner) { isPlaying: Boolean? ->
            if (isPlaying != null) {
                setUpPlayButton(isPlaying)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpBroadcastReceiver()
        setUpRunnableSongArtUpdater()
        setUpOnLayoutChangeListener(view)
        setUpOnDestinationChangedListener()
        linkSongPaneButtons()
    }

    private fun setUpBroadcastReceiver() {
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(requireActivity().resources.getString(
                R.string.broadcast_receiver_action_service_connected))
        requireActivity().registerReceiver(broadcastReceiver, filterComplete)
    }

    private fun setUpRunnableSongArtUpdater() {
        runnableSongPaneArtUpdater = Runnable {
            val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
            val bitmapSongArt: Bitmap? = mediaController.currentAudioUri.value?.id?.let { id ->
                AudioUri.getUri(id).let {
                    BitmapLoader.getThumbnail(
                            it, songArtWidth,
                            songArtWidth,
                            requireActivity().applicationContext
                    )
                }
            }
            if (bitmapSongArt != null) {
                imageViewSongPaneSongArt.setImageBitmap(bitmapSongArt)
            } else {
                val defaultBitmap: Bitmap? = getDefaultBitmap(songArtWidth, songArtWidth)
                if (defaultBitmap != null) {
                    imageViewSongPaneSongArt.setImageBitmap(defaultBitmap)
                }
            }
        }
    }

    private fun getDefaultBitmap(songArtWidth: Int, songArtHeight: Int): Bitmap? {
        // TODO cache bitmap
        val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
        val drawableSongArt: Drawable? = ResourcesCompat.getDrawable(
                imageViewSongPaneSongArt.resources, R.drawable.music_note_black_48dp, null)
        if (drawableSongArt != null) {
            drawableSongArt.setBounds(0, 0, songArtWidth, songArtHeight)
            val bitmapSongArt: Bitmap = Bitmap.createBitmap(
                    songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmapSongArt)
            drawableSongArt.draw(canvas)
            return bitmapSongArt
        }
        return null
    }

    private fun setUpOnLayoutChangeListener(view: View) {
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane)
    }

    fun updateSongUI(currentAudioUri: AudioUri?) {
        if (
                currentAudioUri != null &&
                requireActivity().findViewById<View>(R.id.fragmentSongPane).visibility == View.VISIBLE &&
                mediaController.isSongInProgress()
        ) {
            updateSongPaneName(currentAudioUri.title)
            updateSongPaneArt()
        } else {
        }
    }

    private fun updateSongPaneName(title: String) {
        val textViewSongPaneSongName: TextView = binding.textViewSongPaneSongName
        textViewSongPaneSongName.text = title
    }

    private fun updateSongPaneArt() {
        val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
        imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater)
    }

    private fun setUpPrev() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val imageView: ImageView = binding.imageButtonSongPanePrev
        val drawable: Drawable? = ResourcesCompat.getDrawable(
                activityMain.resources, R.drawable.skip_previous_black_24dp, null)
        if (drawable != null && songArtWidth > 0) {
            drawable.setBounds(0, 0, songArtWidth, songArtWidth)
            val bitmap: Bitmap = Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.draw(canvas)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun setUpPlayButton(isPlaying: Boolean) {
        val imageView: ImageView = binding.imageButtonSongPanePlayPause
        val drawable: Drawable? = if (isPlaying) {
            ResourcesCompat.getDrawable(
                    resources, R.drawable.pause_black_24dp, null)
        } else {
            ResourcesCompat.getDrawable(
                    resources, R.drawable.play_arrow_black_24dp, null)
        }
        if (drawable != null && songArtWidth > 0) {
            drawable.setBounds(0, 0, songArtWidth, songArtWidth)
            val bitmap: Bitmap = Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.draw(canvas)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun setUpNext() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val imageView: ImageView = binding.imageButtonSongPaneNext
        val drawable: Drawable? = ResourcesCompat.getDrawable(
                activityMain.resources, R.drawable.skip_next_black_24dp, null)
        if (drawable != null && songArtWidth > 0) {
            drawable.setBounds(0, 0, songArtWidth, songArtWidth)
            val bitmap: Bitmap = Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.draw(canvas)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun setUpOnDestinationChangedListener() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val fragmentManager: FragmentManager = activityMain.supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(onDestinationChangedListenerSongPane)
        }
    }

    private fun linkSongPaneButtons() {
        val onClickListenerSongPane = View.OnClickListener { v: View ->
            synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
                if (v.id == R.id.imageButtonSongPaneNext) {
                    mediaController.playNext()
                } else if (v.id == R.id.imageButtonSongPanePlayPause) {
                    mediaController.pauseOrPlay()
                } else if (v.id == R.id.imageButtonSongPanePrev) {
                    mediaController.playPrevious()
                } else if (v.id == R.id.textViewSongPaneSongName ||
                        v.id == R.id.imageViewSongPaneSongArt) {
                    (requireActivity() as ActivityMain).navigateTo(R.id.fragmentSong)
                }
            }
        }
        binding.imageButtonSongPaneNext.setOnClickListener(onClickListenerSongPane)
        binding.imageButtonSongPanePlayPause.setOnClickListener(onClickListenerSongPane)
        binding.imageButtonSongPanePrev.setOnClickListener(onClickListenerSongPane)
        binding.textViewSongPaneSongName.setOnClickListener(onClickListenerSongPane)
        binding.imageViewSongPaneSongArt.setOnClickListener(onClickListenerSongPane)
    }

    private fun removeListeners() {
        val view = requireView()
        view.removeOnLayoutChangeListener(onLayoutChangeListenerSongPane)
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                    onDestinationChangedListenerSongPane)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeListeners()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }
}