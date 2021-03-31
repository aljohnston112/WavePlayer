package com.example.waveplayer.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.waveplayer.databinding.FragmentSongBinding
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FragmentSong : Fragment() {
    private var binding: FragmentSongBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var onSeekBarChangeListener: OnSeekBarChangeListener? = null
    private var onClickListenerFragmentSong: View.OnClickListener? = null
    private var onLongClickListener: OnLongClickListener? = null
    private var onTouchListenerFragmentSongButtons: OnTouchListener? = null
    private var onLayoutChangeListenerFragmentSongButtons: View.OnLayoutChangeListener? = null

    // For updating the SeekBar
    private var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var runnableSeekBarUpdater: Runnable? = null
    private var runnableSongArtUpdater: Runnable? = null
    private var observerCurrentSong: Observer<AudioUri?>? = null
    private var observerIsPlaying: Observer<Boolean?>? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewModelActivityMain = ViewModelProvider(requireActivity()).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
        binding = FragmentSongBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.hideKeyboard(view)
        viewModelActivityMain.setSongToAddToQueue(activityMain.getCurrentSong().id)
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.now_playing))
        viewModelActivityMain.showFab(false)
        onSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val activityMain: ActivityMain = requireActivity() as ActivityMain
                activityMain.seekTo(seekBar.getProgress())
            }
        }
        runnableSongArtUpdater = Runnable {
            val imageViewSongArt: ImageView = binding.imageViewSongArt
            var songArtHeight = imageViewSongArt.height
            var songArtWidth = imageViewSongArt.width
            if (songArtWidth > songArtHeight) {
                songArtWidth = songArtHeight
            } else {
                songArtHeight = songArtWidth
            }
            if (songArtHeight > 0 && songArtWidth > 0) {
                val bitmap: Bitmap = BitmapLoader.getThumbnail(activityMain.getCurrentUri(),
                        songArtWidth, songArtHeight, activityMain.getApplicationContext())
                if (bitmap == null) {
                    val drawable: Drawable = ResourcesCompat.getDrawable(imageViewSongArt.resources,
                            R.drawable.music_note_black_48dp, null)
                    if (drawable != null) {
                        drawable.setBounds(0, 0, songArtWidth, songArtHeight)
                        val bitmapDrawable: Bitmap = Bitmap.createBitmap(songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmapDrawable)
                        val paint = Paint()
                        paint.color = imageViewSongArt.resources.getColor(R.color.colorPrimary)
                        canvas.drawRect(0f, 0f, songArtWidth.toFloat(), songArtHeight.toFloat(), paint)
                        drawable.draw(canvas)
                        imageViewSongArt.setImageBitmap(bitmapDrawable)
                    }
                } else {
                    imageViewSongArt.setImageBitmap(bitmap)
                }
            }
        }
        observerCurrentSong = Observer<AudioUri?> { s: AudioUri? -> updateSongUI() }
        viewModelActivityMain.getCurrentSong().observe(viewLifecycleOwner, observerCurrentSong)
        observerIsPlaying = Observer { b: Boolean? -> updateSongPlayButton(b) }
        viewModelActivityMain.getIsPlaying().observe(viewLifecycleOwner, observerIsPlaying)
        setUpButtons()
        setUpBroadcastReceiver()
    }

    private fun updateSongUI() {
        // Log.v(TAG, "updateSongUI start");
        // Log.v(TAG, "updating SongUI");
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        updateSongArt()
        updateSongName()
        updateTextViewTimes()
        // updateSongPlayButton(activityMain.isPlaying());
        updateSeekBar()
        // Log.v(TAG, "updateSongUI end");
    }

    private fun updateSongArt() {
        // Log.v(TAG, "updateSongArt start");
        val imageViewSongArt: ImageView = binding.imageViewSongArt
        imageViewSongArt.post(runnableSongArtUpdater)
        // Log.v(TAG, "updateSongArt end");
    }

    private fun updateSongName() {
        // Log.v(TAG, "updateSongName start");
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val textViewSongName: TextView = binding.textViewSongName
        textViewSongName.setText(activityMain.getCurrentAudioUri().title)
        // Log.v(TAG, "updateSongName end");
    }

    private fun updateTextViewTimes() {
        // Log.v(TAG, "updateTextViewTimes start");
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val maxMillis: Int = activityMain.getCurrentAudioUri().getDuration(activityMain.getApplicationContext())
        val stringEndTime = formatMillis(maxMillis)
        val stringCurrentTime = formatMillis(activityMain.getCurrentTime())
        val textViewCurrent: TextView = binding.editTextCurrentTime
        textViewCurrent.setText(stringCurrentTime)
        val textViewEnd: TextView = binding.editTextEndTime
        textViewEnd.setText(stringEndTime)
        // Log.v(TAG, "updateTextViewTimes end");
    }

    private fun formatMillis(millis: Int): String? {
        // Log.v(TAG, "formatMillis start and end");
        return String.format(resources.configuration.locale,
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis.toLong()),
                TimeUnit.MILLISECONDS.toMinutes(millis.toLong()) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis.toLong())),
                TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis.toLong())))
    }

    private fun updateSongPlayButton(isPlaying: Boolean) {
        // Log.v(TAG, "updateSongPlayButton start");
        val imageButtonPlayPause: ImageButton = binding.imageButtonPlayPause
        // Log.v(TAG, "updating SongPlayButton");
        if (isPlaying) {
            imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                    resources, R.drawable.pause_black_24dp, null))
        } else {
            imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                    resources, R.drawable.play_arrow_black_24dp, null))
        }
        // Log.v(TAG, "updateSongPlayButton end");
    }

    private fun updateSeekBar() {
        // Log.v(TAG, "updateSeekBar start");
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val seekBar: SeekBar = binding.seekBar
        val audioUri: AudioUri = activityMain.getCurrentAudioUri()
        val maxMillis: Int
        maxMillis = if (audioUri != null) {
            audioUri.getDuration(activityMain.getApplicationContext())
        } else {
            9999
        }
        seekBar.setMax(maxMillis)
        seekBar.setProgress(activityMain.getCurrentTime())
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)
        setUpSeekBarUpdater()
        // Log.v(TAG, "updateSeekBar end");
    }

    private fun setUpSeekBarUpdater() {
        // Log.v(TAG, "setUpSeekBarUpdater start");
        val seekBar: SeekBar = binding.seekBar
        val textViewCurrent: TextView = binding.editTextCurrentTime
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        shutDownSeekBarUpdater()
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        val mediaPlayerWUri: MediaPlayerWUri = activityMain.getCurrentMediaPlayerWUri()
        if (mediaPlayerWUri != null) {
            runnableSeekBarUpdater = Runnable {
                seekBar.post(Runnable {
                    if (mediaPlayerWUri.isPrepared()) {
                        val currentMilliseconds: Int = mediaPlayerWUri.getCurrentPosition()
                        if (seekBar.getProgress() != currentMilliseconds) {
                            seekBar.setProgress(currentMilliseconds)
                            val currentTime = formatMillis(currentMilliseconds)
                            textViewCurrent.setText(currentTime)
                        }
                    }
                })
            }
            scheduledExecutorService.scheduleAtFixedRate(
                    runnableSeekBarUpdater, 0L, 1L, TimeUnit.SECONDS)
        }
        // Log.v(TAG, "setUpSeekBarUpdater end");
    }

    fun shutDownSeekBarUpdater() {
        // Log.v(TAG, "shutDownSeekBarUpdater start");
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown()
            scheduledExecutorService = null
        }
        runnableSeekBarUpdater = null
        // Log.v(TAG, "shutDownSeekBarUpdater end");
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpButtons() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val view = view
        val buttonBad: ImageButton = binding.buttonThumbDown
        val buttonGood: ImageButton = binding.buttonThumbUp
        val buttonShuffle: ImageButton = binding.imageButtonShuffle
        val buttonPrev: ImageButton = binding.imageButtonPrev
        val buttonPause: ImageButton = binding.imageButtonPlayPause
        val buttonNext: ImageButton = binding.imageButtonNext
        val buttonLoop: ImageButton = binding.imageButtonRepeat
        onClickListenerFragmentSong = View.OnClickListener { clickedView: View? ->
            synchronized(ActivityMain.Companion.MUSIC_CONTROL_LOCK) {
                if (clickedView.getId() == R.id.button_thumb_down) {
                    val song: Song = activityMain.getSong(activityMain.getCurrentAudioUri().id)
                    if (song != null) {
                        activityMain.getCurrentPlaylist().globalBad(
                                song, activityMain.getPercentChangeDown())
                        activityMain.saveFile()
                    }
                } else if (clickedView.getId() == R.id.button_thumb_up) {
                    val song: Song = activityMain.getSong(activityMain.getCurrentAudioUri().id)
                    if (song != null) {
                        activityMain.getCurrentPlaylist().globalGood(
                                song, activityMain.getPercentChangeUp())
                        activityMain.saveFile()
                    }
                } else if (clickedView.getId() == R.id.imageButtonShuffle) {
                    val imageButton: ImageButton? = clickedView as ImageButton?
                    if (activityMain.isShuffling()) {
                        activityMain.setShuffling(false)
                        imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp)
                    } else {
                        activityMain.setShuffling(true)
                        imageButton.setImageResource(R.drawable.ic_shuffle_black_24dp)
                    }
                } else if (clickedView.getId() == R.id.imageButtonPrev) {
                    activityMain.playPrevious()
                } else if (clickedView.getId() == R.id.imageButtonPlayPause) {
                    activityMain.pauseOrPlay()
                } else if (clickedView.getId() == R.id.imageButtonNext) {
                    activityMain.playNext()
                } else if (clickedView.getId() == R.id.imageButtonRepeat) {
                    val imageButton: ImageButton? = clickedView as ImageButton?
                    if (activityMain.isLoopingOne()) {
                        activityMain.setLoopingOne(false)
                        imageButton.setImageResource(R.drawable.repeat_white_24dp)
                    } else if (activityMain.isLooping()) {
                        activityMain.setLooping(false)
                        activityMain.setLoopingOne(true)
                        imageButton.setImageResource(R.drawable.repeat_one_black_24dp)
                    } else {
                        activityMain.setLooping(true)
                        activityMain.setLoopingOne(false)
                        imageButton.setImageResource(R.drawable.repeat_black_24dp)
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
        buttonNext.setLongClickable(true)
        onLongClickListener = OnLongClickListener { v: View? ->
            // TODO change color of button
            activityMain.getCurrentPlaylist().globalBad(
                    activityMain.getSong(activityMain.getCurrentAudioUri().id),
                    activityMain.getPercentChangeDown())
            true
        }
        buttonNext.setOnLongClickListener(onLongClickListener)
        onTouchListenerFragmentSongButtons = label@ OnTouchListener { view1: View?, motionEvent: MotionEvent? ->
            when (motionEvent.getAction()) {
                MotionEvent.ACTION_DOWN -> {
                    view1.setBackgroundColor(view1.getResources().getColor(R.color.colorOnSecondary))
                    return@label false
                }
                MotionEvent.ACTION_UP -> {
                    view1.setBackgroundColor(view1.getResources().getColor(R.color.colorPrimary))
                    view1.performClick()
                    return@label true
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
        if (activityMain.isShuffling()) {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp)
        } else {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp)
        }
        if (activityMain.isLoopingOne()) {
            buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp)
        } else if (activityMain.isLooping()) {
            buttonLoop.setImageResource(R.drawable.repeat_black_24dp)
        } else {
            buttonLoop.setImageResource(R.drawable.repeat_white_24dp)
        }
        onLayoutChangeListenerFragmentSongButtons = View.OnLayoutChangeListener { view12: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
            if (activityMain.fragmentSongVisible()) {
                setUpButton(binding.buttonThumbUp, R.drawable.thumb_up_alt_black_24dp)
                setUpButton(binding.buttonThumbDown, R.drawable.thumb_down_alt_black_24dp)
                setUpButton(binding.imageButtonPrev, R.drawable.skip_previous_black_24dp)
                setUpButton(binding.imageButtonNext, R.drawable.skip_next_black_24dp)
                setUpShuffle()
                // setUpPlay();
                setUpLoop()
            }
        }
        view.addOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons)
    }

    private fun setUpButton(imageView: ImageView?, drawableID: Int) {
        val width = imageView.getMeasuredWidth()
        val drawable: Drawable = ResourcesCompat.getDrawable(
                imageView.getResources(), drawableID, null)
        if (drawable != null) {
            drawable.setBounds(0, 0, width, width)
            val bitmap: Bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.draw(canvas)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun setUpShuffle() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val imageView: ImageView = binding.imageButtonShuffle
        if (activityMain.isShuffling()) {
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
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val imageView: ImageView = binding.imageButtonRepeat
        if (activityMain.isLoopingOne()) {
            setUpButton(imageView, R.drawable.repeat_one_black_24dp)
        } else if (activityMain.isLooping()) {
            setUpButton(imageView, R.drawable.repeat_black_24dp)
        } else {
            setUpButton(imageView, R.drawable.repeat_white_24dp)
        }
    }

    private fun setUpBroadcastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected))
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu))
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action: String = intent.getAction()
                if (action != null) {
                    if (action == resources.getString(
                                    R.string.broadcast_receiver_action_service_connected)) {
                        setUpButtons()
                        updateSongUI()
                    }
                    if (action == resources.getString(
                                    R.string.broadcast_receiver_action_on_create_options_menu)) {
                        setUpToolbar()
                    }
                }
            }
        }
        activityMain.registerReceiver(broadcastReceiver, filterComplete)
    }

    private fun setUpToolbar() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val toolbar: Toolbar = activityMain.findViewById<Toolbar?>(R.id.toolbar)
        if (toolbar != null) {
            val menu = toolbar.menu
            if (menu != null) {
                menu.getItem(ActivityMain.Companion.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).isVisible = true
                menu.getItem(ActivityMain.Companion.MENU_ACTION_ADD_TO_QUEUE).isVisible = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setUpToolbar()
        updateSongUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
        val view = view
        view.removeOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons)
        onLayoutChangeListenerFragmentSongButtons = null
        val buttonBad: ImageButton = binding.buttonThumbDown
        val buttonGood: ImageButton = binding.buttonThumbUp
        val buttonShuffle: ImageButton = binding.imageButtonShuffle
        val buttonPrev: ImageButton = binding.imageButtonPrev
        val buttonPause: ImageButton = binding.imageButtonPlayPause
        val buttonNext: ImageButton = binding.imageButtonNext
        val buttonLoop: ImageButton = binding.imageButtonRepeat
        buttonBad.setOnClickListener(null)
        buttonGood.setOnClickListener(null)
        buttonShuffle.setOnClickListener(null)
        buttonPrev.setOnClickListener(null)
        buttonPause.setOnClickListener(null)
        buttonNext.setOnClickListener(null)
        buttonLoop.setOnClickListener(null)
        buttonBad.setOnTouchListener(null)
        buttonGood.setOnTouchListener(null)
        buttonShuffle.setOnTouchListener(null)
        buttonPrev.setOnTouchListener(null)
        buttonPause.setOnTouchListener(null)
        buttonNext.setOnTouchListener(null)
        buttonLoop.setOnTouchListener(null)
        buttonNext.setOnLongClickListener(null)
        onClickListenerFragmentSong = null
        onTouchListenerFragmentSongButtons = null
        onLongClickListener = null
        shutDownSeekBarUpdater()
        val seekBar: SeekBar = binding.seekBar
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(null)
        }
        onSeekBarChangeListener = null
        runnableSeekBarUpdater = null
        runnableSongArtUpdater = null
        viewModelActivityMain.getCurrentSong().removeObservers(this)
        observerCurrentSong = null
        viewModelActivityMain.getIsPlaying().removeObservers(this)
        observerIsPlaying = null
        viewModelActivityMain = null
        binding = null
    }
}