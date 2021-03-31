package com.example.waveplayer.fragments

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.example.waveplayer.databinding.FragmentPaneSongBinding

class FragmentPaneSong : Fragment() {
    private var binding: FragmentPaneSongBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var onDestinationChangedListenerSongPane: OnDestinationChangedListener? = null
    private var onLayoutChangeListenerSongPane: View.OnLayoutChangeListener? = null
    private var onClickListenerSongPane: View.OnClickListener? = null
    private var runnableSongPaneArtUpdater: Runnable? = null
    private var observerIsPlaying: Observer<Boolean?>? = null
    private var observerCurrentSong: Observer<AudioUri?>? = null
    private var songArtWidth = 0
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        viewModelActivityMain = ViewModelProvider(activityMain).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
        binding = FragmentPaneSongBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    private fun setUpObservers() {
        observerCurrentSong = Observer<AudioUri?> { currentAudioUri: AudioUri? -> updateSongUI(currentAudioUri) }
        viewModelActivityMain.getCurrentSong().observe(viewLifecycleOwner, observerCurrentSong)
        observerIsPlaying = Observer { isPlaying: Boolean? -> setUpPlayButton(isPlaying) }
        viewModelActivityMain.getIsPlaying().observe(viewLifecycleOwner, observerIsPlaying)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        setUpBroadcastReceiver()
        setUpRunnableSongArtUpdater()
        setUpOnLayoutChangeListener(view)
        setUpOnDestinationChangedListener()
        linkSongPaneButtons()
        updateSongUI(activityMain.getCurrentAudioUri())
    }

    private fun setUpBroadcastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected))
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // TODO
                val action: String = intent.getAction()
                if (action != null) {
                    if (action == resources.getString(
                                    R.string.broadcast_receiver_action_service_connected)) {
                        updateSongUI(activityMain.getCurrentAudioUri())
                    } else if (action == resources.getString(
                                    R.string.broadcast_receiver_action_loaded)) {
                        setUpObservers()
                    }
                }
            }
        }
        activityMain.registerReceiver(broadcastReceiver, filterComplete)
    }

    private fun setUpRunnableSongArtUpdater() {
        runnableSongPaneArtUpdater = Runnable {
            val activityMain: ActivityMain = requireActivity() as ActivityMain
            val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
            val bitmapSongArt: Bitmap = BitmapLoader.getThumbnail(
                    activityMain.getCurrentUri(), songArtWidth, songArtWidth,
                    activityMain.getApplicationContext())
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
        val drawableSongArt: Drawable = ResourcesCompat.getDrawable(
                imageViewSongPaneSongArt.resources, R.drawable.music_note_black_48dp, null)
        if (drawableSongArt != null) {
            val bitmapSongArt: Bitmap
            drawableSongArt.setBounds(0, 0, songArtWidth, songArtHeight)
            bitmapSongArt = Bitmap.createBitmap(
                    songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmapSongArt)
            drawableSongArt.draw(canvas)
            return bitmapSongArt
        }
        return null
    }

    private fun setUpOnLayoutChangeListener(view: View?) {
        onLayoutChangeListenerSongPane = View.OnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
            val activityMain: ActivityMain = requireActivity() as ActivityMain
            if (activityMain.findViewById<View?>(R.id.fragmentSongPane).getVisibility() == View.VISIBLE &&
                    !activityMain.fragmentSongVisible()) {
                if (songArtWidth == 0) {
                    val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
                    songArtWidth = imageViewSongPaneSongArt.width
                }
                updateSongUI(activityMain.getCurrentAudioUri())
                setUpPrev()
                setUpNext()
            }
        }
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane)
    }

    fun updateSongUI(currentAudioUri: AudioUri?) {
        // Log.v(TAG, "updateSongPaneUI start");
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        if (currentAudioUri != null && !activityMain.fragmentSongVisible() && activityMain.songInProgress()) {
            // Log.v(TAG, "updating the song pane UI");
            updateSongPaneName(currentAudioUri.title)
            updateSongPaneArt()
        } else {
            // Log.v(TAG, "not updating the song pane UI");
        }
        // Log.v(TAG, "updateSongPaneUI end");
    }

    private fun updateSongPaneName(title: String?) {
        // Log.v(TAG, "updateSongPaneName start");
        val textViewSongPaneSongName: TextView = binding.textViewSongPaneSongName
        textViewSongPaneSongName.setText(title)
        // Log.v(TAG, "updateSongPaneName end");
    }

    private fun updateSongPaneArt() {
        // Log.v(TAG, "updateSongPaneArt start");
        val imageViewSongPaneSongArt: ImageView = binding.imageViewSongPaneSongArt
        imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater)
        // Log.v(TAG, "updateSongPaneArt end");
    }

    private fun setUpPrev() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val imageView: ImageView = binding.imageButtonSongPanePrev
        val drawable: Drawable = ResourcesCompat.getDrawable(
                activityMain.getResources(), R.drawable.skip_previous_black_24dp, null)
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
        val drawable: Drawable
        drawable = if (isPlaying) {
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
        val drawable: Drawable = ResourcesCompat.getDrawable(
                activityMain.getResources(), R.drawable.skip_next_black_24dp, null)
        if (drawable != null && songArtWidth > 0) {
            drawable.setBounds(0, 0, songArtWidth, songArtWidth)
            val bitmap: Bitmap = Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.draw(canvas)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun setUpOnDestinationChangedListener() {
        // Log.v(TAG, "setUpDestinationChangedListenerForPaneSong started");
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        onDestinationChangedListenerSongPane = OnDestinationChangedListener { controller: NavController?, destination: NavDestination?, arguments: Bundle? ->
            if (destination.getId() != R.id.fragmentSong) {
                if (activityMain.songInProgress()) {
                    activityMain.fragmentSongVisible(false)
                    activityMain.showSongPane()
                }
            } else {
                activityMain.fragmentSongVisible(true)
                activityMain.hideSongPane()
            }
        }
        val fragmentManager: FragmentManager = activityMain.getSupportFragmentManager()
        val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerSongPane)
        }
        // Log.v(TAG, "setUpDestinationChangedListenerForPaneSong ended");
    }

    private fun linkSongPaneButtons() {
        // Log.v(TAG, "linking song pane buttons");
        onClickListenerSongPane = View.OnClickListener { v: View? ->
            val activityMain: ActivityMain = requireActivity() as ActivityMain
            synchronized(ActivityMain.Companion.MUSIC_CONTROL_LOCK) {
                if (v.getId() == R.id.imageButtonSongPaneNext) {
                    activityMain.playNext()
                } else if (v.getId() == R.id.imageButtonSongPanePlayPause) {
                    activityMain.pauseOrPlay()
                } else if (v.getId() == R.id.imageButtonSongPanePrev) {
                    activityMain.playPrevious()
                } else if (v.getId() == R.id.textViewSongPaneSongName ||
                        v.getId() == R.id.imageViewSongPaneSongArt) {
                    activityMain.navigateTo(R.id.fragmentSong)
                }
            }
        }
        binding.imageButtonSongPaneNext.setOnClickListener(onClickListenerSongPane)
        binding.imageButtonSongPanePlayPause.setOnClickListener(onClickListenerSongPane)
        binding.imageButtonSongPanePrev.setOnClickListener(onClickListenerSongPane)
        binding.textViewSongPaneSongName.setOnClickListener(onClickListenerSongPane)
        binding.imageViewSongPaneSongArt.setOnClickListener(onClickListenerSongPane)
        // Log.v(TAG, "done linking song pane buttons");
    }

    fun removeListeners() {
        // Log.v(TAG, "removeListeners started");
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val view = requireView()
        view.removeOnLayoutChangeListener(onLayoutChangeListenerSongPane)
        onLayoutChangeListenerSongPane = null
        val fragmentManager: FragmentManager = activityMain.getSupportFragmentManager()
        val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                    onDestinationChangedListenerSongPane)
        }
        onDestinationChangedListenerSongPane = null
        binding.imageButtonSongPaneNext.setOnClickListener(null)
        binding.imageButtonSongPanePlayPause.setOnClickListener(null)
        binding.imageButtonSongPanePrev.setOnClickListener(null)
        binding.textViewSongPaneSongName.setOnClickListener(null)
        binding.imageViewSongPaneSongArt.setOnClickListener(null)
        onClickListenerSongPane = null
        // Log.v(TAG, "removeListeners ended");
    }

    override fun onDestroy() {
        super.onDestroy()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        removeListeners()
        activityMain.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
        runnableSongPaneArtUpdater = null
        viewModelActivityMain.getCurrentSong().removeObservers(this)
        observerCurrentSong = null
        viewModelActivityMain.getIsPlaying().removeObservers(this)
        observerIsPlaying = null
        viewModelActivityMain = null
        binding = null
    }
}