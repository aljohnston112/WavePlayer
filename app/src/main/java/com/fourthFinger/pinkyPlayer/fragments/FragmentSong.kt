package com.fourthFinger.pinkyPlayer.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.fourthFinger.pinkyPlayer.BitmapUtil
import com.fourthFinger.pinkyPlayer.KeyboardUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.TextUtil.Companion.formatMillis
import com.fourthFinger.pinkyPlayer.ToastUtil
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentSongBinding
import com.fourthFinger.pinkyPlayer.media_controller.*
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FragmentSong : Fragment() {

    private var _binding: FragmentSongBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>()
    private val viewModelFragmentSong by viewModels<ViewModelFragmentSong>()

    private val mediaPlayerModel = MediaPlayerSession.getInstance()
    private val mediaSession: MediaSession =
        MediaSession.getInstance(requireActivity().applicationContext)

    private var currentAudioUri: AudioUri? = null

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
        KeyboardUtil.hideKeyboard(view)
        mediaPlayerModel.currentAudioUri.observe(viewLifecycleOwner) {
            if (it != null) {
                // TODO why does this need to be cached?
                currentAudioUri = it
                viewModelPlaylists.newSong(it.id)
                binding.textViewSongName.text = it.title
                setUpSeekBar(it.getDuration(requireActivity().applicationContext))
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

    private fun setUpSeekBar(maxMillis: Int) {
        val stringEndTime = formatMillis(maxMillis)
        val stringCurrentTime = formatMillis(mediaPlayerModel.getCurrentTime())
        binding.editTextCurrentTime.text = stringCurrentTime
        binding.editTextEndTime.text = stringEndTime
        val seekBar: SeekBar = binding.seekBar
        seekBar.max = maxMillis
        seekBar.progress = mediaPlayerModel.getCurrentTime()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mediaSession.seekTo(requireActivity().applicationContext, seekBar.progress)
            }
        })
    }

    private fun setUpSeekBarUpdater() {
        val seekBar: SeekBar = binding.seekBar
        val textViewCurrent: TextView = binding.editTextCurrentTime
        shutDownSeekBarUpdater()
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        mediaPlayerModel.getCurrentMediaPlayerWUri()?.let {
            val runnableSeekBarUpdater = Runnable {
                seekBar.post {
                    if (it.isPrepared()) {
                        val currentMilliseconds: Int = it.getCurrentPosition()
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

    private fun updateSongArt() {
        val imageViewSongArt: ImageView = binding.imageViewSongArt
        imageViewSongArt.post(runnableSongArtUpdater)
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
            currentAudioUri?.let { it ->
                val bitmap: Bitmap? = BitmapUtil.getThumbnailBitmap(
                    it,
                    songArtWidth,
                    requireActivity().applicationContext
                )
                if (bitmap == null) {
                    val drawable: Drawable? = ResourcesCompat.getDrawable(
                        imageViewSongArt.resources,
                        R.drawable.music_note_black_48dp,
                        null
                    )
                    // TODO Make sure this works
                    drawable?.let { d ->
                        imageViewSongArt.setImageBitmap(d.toBitmap(songArtWidth, songArtWidth))
                    }
                    /*
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
                     */
                } else {
                    imageViewSongArt.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun updateSongPlayButton(isPlaying: Boolean) {
        val imageButtonPlayPause: ImageButton = binding.imageButtonPlayPause
        if (isPlaying) {
            imageButtonPlayPause.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.pause_black_24dp, null
                )
            )
        } else {
            imageButtonPlayPause.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.play_arrow_black_24dp, null
                )
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpButtons() {
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
                        viewModelFragmentSong.thumbDownClicked(currentAudioUri)
                    }
                    R.id.button_thumb_up -> {
                        viewModelFragmentSong.thumbUpClicked(currentAudioUri)
                    }
                    R.id.imageButtonShuffle -> {
                        viewModelFragmentSong.shuffleClicked()
                        val imageButton: ImageButton = clickedView as ImageButton
                        if (mediaSession.isShuffling()) {
                            imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp)
                        } else {
                            imageButton.setImageResource(R.drawable.ic_shuffle_black_24dp)
                        }
                    }
                    R.id.imageButtonPrev -> {
                        viewModelFragmentSong.prevClicked()
                    }
                    R.id.imageButtonPlayPause -> {
                        viewModelFragmentSong.playPauseClicked()
                    }
                    R.id.imageButtonNext -> {
                        viewModelFragmentSong.nextClicked()
                    }
                    R.id.imageButtonRepeat -> {
                        viewModelFragmentSong.repeatClicked()
                        val imageButton: ImageButton = clickedView as ImageButton
                        when {
                            mediaSession.isLoopingOne() -> {
                                imageButton.setImageResource(R.drawable.repeat_white_24dp)
                            }
                            mediaSession.isLooping() -> {
                                imageButton.setImageResource(R.drawable.repeat_one_black_24dp)
                            }
                            else -> {
                                imageButton.setImageResource(R.drawable.repeat_black_24dp)
                            }
                        }
                    }
                    else -> {
                        ToastUtil.showToast(requireActivity().applicationContext, R.string.RR)
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
            // Change color, start a runnable to change it back
            viewModelFragmentSong.nextLongClicked(currentAudioUri)
            it.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.colorPrimaryVariant
                )
            )
            true
        }
        val onTouchListenerFragmentSongButtons =
            View.OnTouchListener { view1: View, motionEvent: MotionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view1.setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorOnSecondary
                            )
                        )
                        return@OnTouchListener false
                    }
                    MotionEvent.ACTION_UP -> {
                        view1.setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimary
                            )
                        )
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
        if (mediaSession.isShuffling()) {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp)
        } else {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp)
        }
        when {
            mediaSession.isLoopingOne() -> {
                buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp)
            }
            mediaSession.isLooping() -> {
                buttonLoop.setImageResource(R.drawable.repeat_black_24dp)
            }
            else -> {
                buttonLoop.setImageResource(R.drawable.repeat_white_24dp)
            }
        }
        requireView().addOnLayoutChangeListener(onLayoutChangeListenerFragmentSong)
    }

    private val onLayoutChangeListenerFragmentSong =
        View.OnLayoutChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
            viewModelActivityMain.fragmentSongVisible.observe(viewLifecycleOwner) {
                if (it == true) {
                    setUpButton(binding.buttonThumbUp, R.drawable.thumb_up_alt_black_24dp)
                    setUpButton(binding.buttonThumbDown, R.drawable.thumb_down_alt_black_24dp)
                    setUpButton(binding.imageButtonPrev, R.drawable.skip_previous_black_24dp)
                    setUpButton(binding.imageButtonNext, R.drawable.skip_next_black_24dp)
                    // TODO make these happen in response to LiveData
                    setUpShuffle()
                    setUpLoop()
                }
            }
        }

    private fun setUpButton(imageView: ImageView, drawableID: Int) {
        val width = imageView.measuredWidth
        ResourcesCompat.getDrawable(
            imageView.resources,
            drawableID,
            null
        )?.let {
            // TODO Make sure this works
            imageView.setImageBitmap(it.toBitmap(width, width))
            /*
            it.setBounds(0, 0, width, width)
            val bitmap: Bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            it.draw(canvas)
            imageView.setImageBitmap(bitmap)
             */
        }
    }

    private fun setUpShuffle() {
        val imageView: ImageView = binding.imageButtonShuffle
        if (mediaSession.isShuffling()) {
            setUpButton(imageView, R.drawable.ic_shuffle_black_24dp)
        } else {
            setUpButton(imageView, R.drawable.ic_shuffle_white_24dp)
        }
    }

    private fun setUpLoop() {
        val imageView: ImageView = binding.imageButtonRepeat
        when {
            mediaSession.isLoopingOne() -> {
                setUpButton(imageView, R.drawable.repeat_one_black_24dp)
            }
            mediaSession.isLooping() -> {
                setUpButton(imageView, R.drawable.repeat_black_24dp)
            }
            else -> {
                setUpButton(imageView, R.drawable.repeat_white_24dp)
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
        requireActivity().registerReceiver(broadcastReceiver, filterComplete)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (action != null) {
                if (action == resources.getString(
                        R.string.broadcast_receiver_action_service_connected
                    )
                ) {
                    // TODO make sure this isn't needed
                    // setUpButtons()
                }
            }
        }
    }

    private fun setUpToolbar() {
        requireActivity().findViewById<Toolbar>(R.id.toolbar).menu?.let {
            it.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).isVisible = true
            it.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE_INDEX).isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO delete?
        setUpToolbar()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
            super.onPrepareOptionsMenu(menu)
            setUpToolbar()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        view?.removeOnLayoutChangeListener(onLayoutChangeListenerFragmentSong)
        shutDownSeekBarUpdater()
    }

}