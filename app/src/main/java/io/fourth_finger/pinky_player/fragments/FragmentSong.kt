package io.fourth_finger.pinky_player.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import io.fourth_finger.pinky_player.KeyboardUtil
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.TextUtil.Companion.formatMillis
import io.fourth_finger.pinky_player.ToastUtil
import io.fourth_finger.pinky_player.activity_main.ActivityMain
import io.fourth_finger.pinky_player.activity_main.MenuActionIndex
import io.fourth_finger.pinky_player.activity_main.ViewModelActivityMain
import io.fourth_finger.pinky_player.databinding.FragmentSongBinding
import io.fourth_finger.playlist_data_source.AudioUri
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FragmentSong : Fragment() {

    private var _binding: FragmentSongBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain> {
        ViewModelActivityMain.Factory
    }

    private val viewModelFragmentSong by viewModels<ViewModelFragmentSong> {
        ViewModelFragmentSong.Factory
    }

    // For updating the SeekBar
    private var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    private fun setUpMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater
            ) {
                menuInflater.inflate(
                    R.menu.menu_toolbar,
                    menu
                )
                for (menuActionIndex in MenuActionIndex.entries) {
                    val menuItem = menu.getItem(menuActionIndex.ordinal)
                    when (menuActionIndex) {
                        MenuActionIndex.MENU_ACTION_ADD_TO_PLAYLIST_INDEX -> {
                            menuItem.isVisible = true
                        }

                        MenuActionIndex.MENU_ACTION_QUEUE_INDEX -> {
                            menuItem.isVisible = true
                        }

                        MenuActionIndex.MENU_ACTION_SEARCH_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_ADD_TO_QUEUE_INDEX -> {
                            menuItem.isVisible = true
                        }

                        MenuActionIndex.MENU_ACTION_LOWER_PROBABILITIES_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_RESET_PROBABILITIES_INDEX -> {
                            menuItem.isVisible = false
                        }
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtil.hideKeyboard(view)
        setUpMenu()
        viewModelFragmentSong.currentAudioUri.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModelActivityMain.setSongToAddToQueue(it.id)
                binding.textViewSongName.text = it.title
                setUpSeekBar(it.getDurationMS(requireActivity().applicationContext).toInt())
                setUpSeekBarUpdater()
                updateSongArt(it)
                setUpButtons(it)
            }
        }
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.now_playing))
        viewModelActivityMain.showFab(false)
        viewModelFragmentSong.isPlaying.observe(viewLifecycleOwner) { b: Boolean? ->
            if (b != null) {
                updateSongPlayButton(b)
            }
        }
        viewModelActivityMain.fragmentSongVisible.observe(viewLifecycleOwner) {
            if (it == true) {
                setUpButton(binding.buttonThumbUp, R.drawable.thumb_up_alt_black_24dp)
                setUpButton(binding.buttonThumbDown, R.drawable.thumb_down_alt_black_24dp)
                setUpButton(binding.imageButtonPrev, R.drawable.skip_previous_black_24dp)
                setUpButton(binding.imageButtonNext, R.drawable.skip_next_black_24dp)
                // TODO make what happens inside these happen in response to LiveData
                setUpShuffle()
                setUpLoop()
            }
        }
        setUpButtons(null)
    }

    private fun setUpSeekBar(maxMillis: Int) {
        val stringEndTime = formatMillis(maxMillis)
        val stringCurrentTime = formatMillis(viewModelFragmentSong.getCurrentTime())
        binding.editTextCurrentTime.text = stringCurrentTime
        binding.editTextEndTime.text = stringEndTime
        val seekBar: SeekBar = binding.seekBar
        seekBar.max = maxMillis
        seekBar.progress = viewModelFragmentSong.getCurrentTime()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                viewModelFragmentSong.seekTo(
                    requireContext(),
                    seekBar.progress
                )
            }
        })
    }

    private fun setUpSeekBarUpdater() {
        val seekBar: SeekBar = binding.seekBar
        val textViewCurrent: TextView = binding.editTextCurrentTime
        shutDownSeekBarUpdater()
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        val runnableSeekBarUpdater = Runnable {
            seekBar.post {
                val currentMilliseconds: Int = viewModelFragmentSong.getCurrentTime()
                if (seekBar.progress != currentMilliseconds) {
                    seekBar.progress = currentMilliseconds
                    val currentTime = formatMillis(currentMilliseconds)
                    textViewCurrent.text = currentTime
                }
            }
        }
        scheduledExecutorService.scheduleWithFixedDelay(
            runnableSeekBarUpdater,
            0L,
            1L,
            TimeUnit.SECONDS
        )
    }

    private fun shutDownSeekBarUpdater() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow()
            scheduledExecutorService = null
        }
    }

    private fun updateSongArt(audioUri: AudioUri) {
        val imageViewSongArt: ImageView = binding.imageViewSongArt
        val runnableSongArtUpdater = RunnableSongArtUpdater(
            requireActivity().resources,
            binding.imageViewSongArt,
            audioUri
        )
        imageViewSongArt.post(runnableSongArtUpdater)
    }

    private class RunnableSongArtUpdater(
        val resources: Resources,
        val imageViewSongArt: ImageView,
        val currentAudioUri: AudioUri
    ) : Runnable {

        override fun run() {
            var songArtHeight = imageViewSongArt.height
            var songArtWidth = imageViewSongArt.width
            if (songArtWidth > songArtHeight) {
                songArtWidth = songArtHeight
            } else {
                songArtHeight = songArtWidth
            }
            if (songArtHeight > 0) {
                val bitmap: Bitmap? =
                    io.fourth_finger.playlist_data_source.BitmapUtil.getThumbnailBitmap(
                        imageViewSongArt.context,
                        currentAudioUri,
                        songArtWidth,
                    )
                if (bitmap == null) {
                    val drawable: Drawable? = ResourcesCompat.getDrawable(
                        resources,
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
//                        val paint = Paint()
//                        paint.color = ContextCompat.getColor(
//                            imageViewSongArt.context,
//                            R.color.colorPrimary
//                        )
//                        canvas.drawRect(0f, 0f, songArtWidth.toFloat(), songArtHeight.toFloat(), paint)
                        drawable.draw(canvas)
                        imageViewSongArt.setImageBitmap(bitmapDrawable)
                    }
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
    private fun setUpButtons(currentAudioUri: AudioUri?) {
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
                        currentAudioUri?.let {
                            viewModelFragmentSong.thumbDownClicked(
                                requireContext(),
                                it
                            )
                        }
                    }

                    R.id.button_thumb_up -> {
                        currentAudioUri?.let {
                            viewModelFragmentSong.thumbUpClicked(
                                requireContext(),
                                it
                            )
                        }
                    }

                    R.id.imageButtonShuffle -> {
                        viewModelFragmentSong.shuffleClicked()
                        val imageButton: ImageButton = clickedView as ImageButton
                        if (viewModelFragmentSong.isShuffling()) {
                            imageButton.setImageResource(R.drawable.ic_shuffle_black_24dp)
                        } else {
                            imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp)
                        }
                    }

                    R.id.imageButtonPrev -> {
                        viewModelFragmentSong.prevClicked(requireContext())
                    }

                    R.id.imageButtonPlayPause -> {
                        viewModelFragmentSong.playPauseClicked(requireContext())
                    }

                    R.id.imageButtonNext -> {
                        currentAudioUri?.let {
                            viewModelFragmentSong.nextClicked(
                                requireContext(),
                                it
                            )
                        }
                    }

                    R.id.imageButtonRepeat -> {
                        viewModelFragmentSong.repeatClicked()
                        val imageButton: ImageButton = clickedView as ImageButton
                        when {
                            viewModelFragmentSong.isLoopingOne() -> {
                                imageButton.setImageResource(R.drawable.repeat_one_black_24dp)
                            }

                            viewModelFragmentSong.isLooping() -> {
                                imageButton.setImageResource(R.drawable.repeat_black_24dp)
                            }

                            else -> {
                                imageButton.setImageResource(R.drawable.repeat_white_24dp)
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
        if (viewModelFragmentSong.isShuffling()) {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp)
        } else {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp)
        }
        when {
            viewModelFragmentSong.isLoopingOne() -> {
                buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp)
            }

            viewModelFragmentSong.isLooping() -> {
                buttonLoop.setImageResource(R.drawable.repeat_black_24dp)
            }

            else -> {
                buttonLoop.setImageResource(R.drawable.repeat_white_24dp)
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
            if (width > 0) {
                imageView.setImageBitmap(it.toBitmap(width, width))
            }
        }
    }

    private fun setUpShuffle() {
        val imageView: ImageView = binding.imageButtonShuffle
        if (viewModelFragmentSong.isShuffling()) {
            setUpButton(imageView, R.drawable.ic_shuffle_black_24dp)
        } else {
            setUpButton(imageView, R.drawable.ic_shuffle_white_24dp)
        }
    }

    private fun setUpLoop() {
        val imageView: ImageView = binding.imageButtonRepeat
        when {
            viewModelFragmentSong.isLoopingOne() -> {
                setUpButton(imageView, R.drawable.repeat_one_black_24dp)
            }

            viewModelFragmentSong.isLooping() -> {
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
                R.string.action_service_connected
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        shutDownSeekBarUpdater()
    }

}