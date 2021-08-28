package com.fourthFinger.pinkyPlayer.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fourthFinger.pinkyPlayer.KeyboardUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentSongBinding
import com.fourthFinger.pinkyPlayer.media_controller.*
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FragmentSong : Fragment() {

    private var _binding: FragmentSongBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>()

    private val mediaPlayerModel = MediaPlayerModel.getInstance()

    private lateinit var mediaData: MediaData
    private lateinit var mediaController: MediaController

    private var currentAudioUri: AudioUri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaController = MediaController.getInstance(requireActivity().applicationContext)
        mediaData = MediaData.getInstance(requireActivity().applicationContext)

    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (action != null) {
                if (action == resources.getString(
                                R.string.broadcast_receiver_action_service_connected)) {
                    setUpButtons()
                }
                if (action == resources.getString(
                                R.string.broadcast_receiver_action_on_create_options_menu)) {
                    setUpToolbar()
                }
            }
        }
    }

    private val onLayoutChangeListenerFragmentSong = View.OnLayoutChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
        val activityMain = requireActivity() as ActivityMain
        if (activityMain.fragmentSongVisible()) {
            setUpButton(binding.buttonThumbUp, R.drawable.thumb_up_alt_black_24dp)
            setUpButton(binding.buttonThumbDown, R.drawable.thumb_down_alt_black_24dp)
            setUpButton(binding.imageButtonPrev, R.drawable.skip_previous_black_24dp)
            setUpButton(binding.imageButtonNext, R.drawable.skip_next_black_24dp)
            setUpShuffle()
            //setUpPlay();
            setUpLoop()
        }
    }

    private val runnableSongArtUpdater = Runnable {
        val imageViewSongArt: ImageView = binding.imageViewSongArt
        var songArtHeight = imageViewSongArt.height
        var songArtWidth = imageViewSongArt.width
        if (songArtWidth > songArtHeight) {
            songArtWidth = songArtHeight
        } else {
            songArtHeight = songArtWidth
        }
        if (songArtHeight > 0 && songArtWidth > 0) {
            currentAudioUri?.let {
                val bitmap: Bitmap? = BitmapLoader.getThumbnail(
                        it.getUri(),
                        songArtWidth,
                        songArtHeight,
                        requireActivity().applicationContext
                )
                if (bitmap == null) {
                    val drawable: Drawable? = ResourcesCompat.getDrawable(
                            imageViewSongArt.resources,
                            R.drawable.music_note_black_48dp,
                            null
                    )
                    if (drawable != null) {
                        drawable.setBounds(0, 0, songArtWidth, songArtHeight)
                        val bitmapDrawable: Bitmap = Bitmap.createBitmap(
                                songArtWidth,
                                songArtHeight,
                                Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmapDrawable)
                        val paint = Paint()
                        paint.color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                        canvas.drawRect(0f, 0f, songArtWidth.toFloat(), songArtHeight.toFloat(), paint)
                        drawable.draw(canvas)
                        imageViewSongArt.setImageBitmap(bitmapDrawable)
                    }
                } else {
                    imageViewSongArt.setImageBitmap(bitmap)
                }
            }
        }
    }

    // For updating the SeekBar
    private var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        KeyboardUtil.hideKeyboard(view)
        mediaPlayerModel.currentAudioUri.observe(viewLifecycleOwner) {
            if (it != null) {
                currentAudioUri = it
                viewModelActivityMain.setSongToAddToQueue(it.id)
                viewModelActivityMain.setPlaylistToAddToQueue(null)
                val textViewSongName: TextView = binding.textViewSongName
                textViewSongName.text = it.title
                val maxMillis: Int = it.getDuration(activityMain.applicationContext)
                val stringEndTime = formatMillis(maxMillis)
                val stringCurrentTime = formatMillis(mediaPlayerModel.getCurrentTime())
                val textViewCurrent: TextView = binding.editTextCurrentTime
                textViewCurrent.text = stringCurrentTime
                val textViewEnd: TextView = binding.editTextEndTime
                textViewEnd.text = stringEndTime
                val seekBar: SeekBar = binding.seekBar
                seekBar.max = maxMillis
                seekBar.progress = mediaPlayerModel.getCurrentTime()
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        mediaController.seekTo(seekBar.progress)
                    }
                })
                setUpSeekBarUpdater()
                updateSongArt()
            }
        }
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.now_playing))
        viewModelActivityMain.showFab(false)
        mediaPlayerModel.isPlaying.observe(viewLifecycleOwner) { b: Boolean? ->
            if (b != null) {
                updateSongPlayButton(b)
            }
        }
        setUpButtons()
    }

    private fun updateSongArt() {
        val imageViewSongArt: ImageView = binding.imageViewSongArt
        imageViewSongArt.post(runnableSongArtUpdater)
    }

    private fun formatMillis(millis: Int): String {
        return String.format(LocaleListCompat.getDefault()[0],
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis.toLong()),
                TimeUnit.MILLISECONDS.toMinutes(millis.toLong()) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis.toLong())),
                TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis.toLong())))
    }

    private fun updateSongPlayButton(isPlaying: Boolean) {
        val imageButtonPlayPause: ImageButton = binding.imageButtonPlayPause
        if (isPlaying) {
            imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                    resources, R.drawable.pause_black_24dp, null))
        } else {
            imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                    resources, R.drawable.play_arrow_black_24dp, null))
        }
    }


    private fun setUpSeekBarUpdater() {
        val seekBar: SeekBar = binding.seekBar
        val textViewCurrent: TextView = binding.editTextCurrentTime
        shutDownSeekBarUpdater()
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        val mediaPlayerWUri: MediaPlayerWUri? = mediaPlayerModel.getCurrentMediaPlayerWUri()
        if (mediaPlayerWUri != null) {
            val runnableSeekBarUpdater = Runnable {
                seekBar.post {
                    if (mediaPlayerWUri.isPrepared()) {
                        val currentMilliseconds: Int = mediaPlayerWUri.getCurrentPosition()
                        if (seekBar.progress != currentMilliseconds) {
                            seekBar.progress = currentMilliseconds
                            val currentTime = formatMillis(currentMilliseconds)
                            textViewCurrent.text = currentTime
                        }
                    }
                }
            }
            scheduledExecutorService.scheduleAtFixedRate(
                    runnableSeekBarUpdater,
                    0L,
                    1L,
                    TimeUnit.SECONDS
            )
        }
    }

    private fun shutDownSeekBarUpdater() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown()
            scheduledExecutorService = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpButtons() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val view = requireView()
        val buttonBad: ImageButton = binding.buttonThumbDown
        val buttonGood: ImageButton = binding.buttonThumbUp
        val buttonShuffle: ImageButton = binding.imageButtonShuffle
        val buttonPrev: ImageButton = binding.imageButtonPrev
        val buttonPause: ImageButton = binding.imageButtonPlayPause
        val buttonNext: ImageButton = binding.imageButtonNext
        val buttonLoop: ImageButton = binding.imageButtonRepeat
        val onClickListenerFragmentSong = View.OnClickListener { clickedView: View ->
            synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
                when (clickedView.id) {
                    R.id.button_thumb_down -> {
                        val song: Song? = currentAudioUri?.id?.let {
                            viewModelPlaylists.getSong(it)
                        }
                        if (song != null) {
                            mediaController.getCurrentPlaylist()?.globalBad(song)
                            activityMain.saveFile()
                        }
                    }
                    R.id.button_thumb_up -> {
                        val song: Song? = currentAudioUri?.id?.let {
                            viewModelPlaylists.getSong(it)
                        }
                        if (song != null) {
                            mediaController.getCurrentPlaylist()?.globalGood(song)
                            activityMain.saveFile()
                        }
                    }
                    R.id.imageButtonShuffle -> {
                        val imageButton: ImageButton = clickedView as ImageButton
                        if (mediaController.isShuffling()) {
                            mediaController.setShuffling(false)
                            imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp)
                        } else {
                            mediaController.setShuffling(true)
                            imageButton.setImageResource(R.drawable.ic_shuffle_black_24dp)
                        }
                    }
                    R.id.imageButtonPrev -> {
                        mediaController.playPrevious()
                    }
                    R.id.imageButtonPlayPause -> {
                        mediaController.pauseOrPlay()
                    }
                    R.id.imageButtonNext -> {
                        mediaController.playNext()
                    }
                    R.id.imageButtonRepeat -> {
                        val imageButton: ImageButton = clickedView as ImageButton
                        if (mediaController.isLoopingOne()) {
                            mediaController.setLoopingOne(false)
                            imageButton.setImageResource(R.drawable.repeat_white_24dp)
                        } else if (mediaController.isLooping()) {
                            mediaController.setLooping(false)
                            mediaController.setLoopingOne(true)
                            imageButton.setImageResource(R.drawable.repeat_one_black_24dp)
                        } else {
                            mediaController.setLooping(true)
                            mediaController.setLoopingOne(false)
                            imageButton.setImageResource(R.drawable.repeat_black_24dp)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
        buttonBad.setOnClickListener(onClickListenerFragmentSong)
        buttonGood.setOnClickListener(onClickListenerFragmentSong)
        buttonShuffle.setOnClickListener(onClickListenerFragmentSong)
        buttonPrev.setOnClickListener(onClickListenerFragmentSong)
        buttonPause.setOnClickListener(onClickListenerFragmentSong)
        buttonNext.setOnClickListener(onClickListenerFragmentSong)
        buttonLoop.setOnClickListener(onClickListenerFragmentSong)
        buttonNext.isLongClickable = true
        buttonNext.setOnLongClickListener {
            // TODO change color of button
            currentAudioUri?.id?.let { id ->
                viewModelPlaylists.getSong(id)?.let {
                    mediaController.getCurrentPlaylist()?.globalBad(it)
                }
            }
            true
        }
        val onTouchListenerFragmentSongButtons = View.OnTouchListener { view1: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    view1.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorOnSecondary))
                    return@OnTouchListener false
                }
                MotionEvent.ACTION_UP -> {
                    view1.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                    view1.performClick()
                    return@OnTouchListener true
                }
            }
            false
        }
        buttonBad.setOnTouchListener(onTouchListenerFragmentSongButtons)
        buttonGood.setOnTouchListener(onTouchListenerFragmentSongButtons)
        buttonShuffle.setOnTouchListener(onTouchListenerFragmentSongButtons)
        buttonPrev.setOnTouchListener(onTouchListenerFragmentSongButtons)
        buttonPause.setOnTouchListener(onTouchListenerFragmentSongButtons)
        buttonNext.setOnTouchListener(onTouchListenerFragmentSongButtons)
        buttonLoop.setOnTouchListener(onTouchListenerFragmentSongButtons)
        if (mediaController.isShuffling()) {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp)
        } else {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp)
        }
        if (mediaController.isLoopingOne()) {
            buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp)
        } else if (mediaController.isLooping()) {
            buttonLoop.setImageResource(R.drawable.repeat_black_24dp)
        } else {
            buttonLoop.setImageResource(R.drawable.repeat_white_24dp)
        }
        view.addOnLayoutChangeListener(onLayoutChangeListenerFragmentSong)
    }

    private fun setUpButton(imageView: ImageView, drawableID: Int) {
        val width = imageView.measuredWidth
        val drawable: Drawable? = ResourcesCompat.getDrawable(
                imageView.resources,
                drawableID,
                null
        )
        if (drawable != null) {
            drawable.setBounds(0, 0, width, width)
            val bitmap: Bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.draw(canvas)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun setUpShuffle() {
        val imageView: ImageView = binding.imageButtonShuffle
        if (mediaController.isShuffling()) {
            setUpButton(imageView, R.drawable.ic_shuffle_black_24dp)
        } else {
            setUpButton(imageView, R.drawable.ic_shuffle_white_24dp)
        }
    }

    private fun setUpPlay(isPlaying: Boolean) {
        val imageView: ImageView = binding.imageButtonPlayPause
        if (isPlaying) {
            setUpButton(imageView, R.drawable.pause_black_24dp)
        } else {
            setUpButton(imageView, R.drawable.play_arrow_black_24dp)
        }
    }

    private fun setUpLoop() {
        val imageView: ImageView = binding.imageButtonRepeat
        if (mediaController.isLoopingOne()) {
            setUpButton(imageView, R.drawable.repeat_one_black_24dp)
        } else if (mediaController.isLooping()) {
            setUpButton(imageView, R.drawable.repeat_black_24dp)
        } else {
            setUpButton(imageView, R.drawable.repeat_white_24dp)
        }
    }

    private fun setUpBroadcastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(requireActivity().resources.getString(
                R.string.broadcast_receiver_action_service_connected))
        filterComplete.addAction(requireActivity().resources.getString(
                R.string.broadcast_receiver_action_on_create_options_menu))
        activityMain.registerReceiver(broadcastReceiver, filterComplete)
    }

    private fun setUpToolbar() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val toolbar: Toolbar = activityMain.findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).isVisible = true
            menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        setUpToolbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        setUpBroadcastReceiver()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        view?.removeOnLayoutChangeListener(onLayoutChangeListenerFragmentSong)
        shutDownSeekBarUpdater()
        mediaPlayerModel.isPlaying.removeObservers(this)
        mediaPlayerModel.currentAudioUri.removeObservers(this)
    }

}