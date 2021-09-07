package com.fourthFinger.pinkyPlayer.activity_main

import android.content.*
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.databinding.ActivityMainBinding
import com.fourthFinger.pinkyPlayer.media_controller.*
import com.fourthFinger.pinkyPlayer.random_playlist.SongQueue
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.util.*

class ActivityMain : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModelActivityMain by viewModels<ViewModelActivityMain>()

    private val mediaPlayerModel = MediaPlayerModel.getInstance()

    private lateinit var mediaController: MediaController
    private lateinit var mediaData: MediaData
    private val songQueue = SongQueue.getInstance()

    private var fragmentSongVisible = false
    private lateinit var onDestinationChangedListenerToolbar: NavController.OnDestinationChangedListener

    private var serviceMain: ServiceMain? = null
    private val connectionServiceMain = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder: ServiceMain.ServiceMainBinder = service as ServiceMain.ServiceMainBinder
            serviceMain = binder.getService()
            sendBroadcastServiceConnected()
            setUpAfterServiceConnection()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceDisconnected()
        }
    }

    private var broadcastReceiverServiceMain = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                (resources.getString(R.string.broadcast_receiver_action_play_pause)) -> {

                }
                (resources.getString(R.string.broadcast_receiver_action_loaded)) -> {
                    loaded(true)
                    navigateTo(R.id.FragmentTitle)
                }
                (resources.getString(R.string.broadcast_receiver_action_new_song)) -> {

                }
            }
        }
    }

    // region lifecycle
    // region onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.setLifecycleOwner { lifecycle }
        binding.viewModelActivityMain = viewModelActivityMain
        setContentView(binding.root)
        mediaController = MediaController.getInstance(applicationContext)
        mediaData = MediaData.getInstance()
        setUpViewModelActivityMainObservers()
    }

    private fun setUpViewModelActivityMainObservers() {
        val fab: ExtendedFloatingActionButton = binding.fab
        val toolbar = binding.toolbar
        viewModelActivityMain.showFab.observe(this) { showFAB: Boolean ->
            when (showFAB) {
                true -> fab.show()
                false -> fab.hide()
            }
        }
        viewModelActivityMain.fabText.observe(this) { fabText: Int? ->
            if (fabText != null) {
                fab.setText(fabText)
            }
        }
        viewModelActivityMain.fabImageID.observe(this) { drawableID: Int? ->
            if (drawableID != null) {
                fab.icon = ResourcesCompat.getDrawable(resources, drawableID, theme)
            }
        }
        viewModelActivityMain.actionBarTitle.observe(this){title ->
            toolbar.title = title
        }
    }

    // endregion onCreate

    // region onStart
    override fun onStart() {
        setUpToolbar()
        startAndBindServiceMain()
        super.onStart()
    }

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
        // TODO should not be needed
        //toolbar.setTitleTextColor(ContextCompat.getColor(applicationContext, R.color.colorOnPrimary))
        setOverflowIconColor()
        centerToolbarTitleAndSetTextSize()
        setUpDestinationChangedListenerForToolbar()
    }

    private fun setOverflowIconColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.toolbar.overflowIcon?.colorFilter =
                BlendModeColorFilter(resources.getColor(R.color.colorOnPrimary, theme), BlendMode.SRC_ATOP)
        } else {
            binding.toolbar.overflowIcon?.setColorFilter(
                ContextCompat.getColor(applicationContext, R.color.colorOnPrimary), PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun centerToolbarTitleAndSetTextSize() {
        val textViews = ArrayList<View>()
        window.decorView.findViewsWithText(textViews, title, View.FIND_VIEWS_WITH_TEXT)
        if (textViews.isNotEmpty()) {
            var appCompatTextView: AppCompatTextView? = null
            for (v in textViews) {
                if (v.parent is Toolbar) {
                    appCompatTextView = v as AppCompatTextView
                    break
                }
            }
            if (appCompatTextView != null) {
                val params: ViewGroup.LayoutParams = appCompatTextView.layoutParams
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                appCompatTextView.layoutParams = params
                appCompatTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                appCompatTextView.textSize = 28f
            }
        }
    }

    // TODO fragments should do this
    private fun setUpDestinationChangedListenerForToolbar() {
        onDestinationChangedListenerToolbar = NavController.OnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
            runOnUiThread {
                val menu = binding.toolbar.menu
                if (menu.size() > 0) {
                    menu.getItem(MENU_ACTION_RESET_PROBS_INDEX).isVisible =
                            destination.id == R.id.fragmentPlaylist ||
                                    destination.id == R.id.fragmentSongs
                    menu.getItem(MENU_ACTION_LOWER_PROBS_INDEX).isVisible =
                            destination.id == R.id.fragmentPlaylist ||
                                    destination.id == R.id.fragmentSongs
                    menu.getItem(MENU_ACTION_ADD_TO_PLAYLIST_INDEX).isVisible =
                            destination.id == R.id.fragmentSong ||
                                    destination.id == R.id.fragmentPlaylist
                    menu.getItem(MENU_ACTION_SEARCH_INDEX).isVisible =
                            destination.id == R.id.fragmentSongs ||
                                    destination.id == R.id.fragmentPlaylist ||
                                    destination.id == R.id.FragmentPlaylists
                    menu.getItem(MENU_ACTION_ADD_TO_QUEUE).isVisible =
                            destination.id == R.id.fragmentSong ||
                                    destination.id == R.id.fragmentPlaylist
                }
            }
        }
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar)
        }
    }

    private fun startAndBindServiceMain() {
        val intentServiceMain = Intent(applicationContext, ServiceMain::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain)
        } else {
            startService(intentServiceMain)
        }
        applicationContext.bindService(
                intentServiceMain,
                connectionServiceMain,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
        )
    }

    private fun sendBroadcastServiceConnected() {
        val intent = Intent()
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.action = resources.getString(R.string.broadcast_receiver_action_service_connected)
        sendBroadcast(intent)
    }

    private fun setUpAfterServiceConnection() {
        // song, title loading?
        goToDestinationFragment()
        // loaded br
        setUpBroadcastReceiver()
        hideSongPane()
    }

    private fun goToDestinationFragment() {
        if (serviceMain?.loaded() == true) {
            if (mediaPlayerModel.isPlaying.value == true && !fragmentSongVisible()) {
                hideSongPane()
                navigateTo(R.id.fragmentSong)
            } else {
                navigateTo(R.id.FragmentTitle)
            }
        } else {
            val fragmentManager: FragmentManager = supportFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
            if (fragment != null) {
                NavHostFragment.findNavController(fragment).popBackStack(R.id.fragmentLoading, true)
            }
        }
    }

    fun navigateTo(id: Int) {
        runOnUiThread {
            val fragmentManager: FragmentManager = supportFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
            if (fragment != null) {
                val navController: NavController = NavHostFragment.findNavController(fragment)
                navController.navigate(id)
            }
        }
    }

    private fun setUpBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.broadcast_receiver_action_new_song))
        filter.addAction(resources.getString(R.string.broadcast_receiver_action_play_pause))
        filter.addAction(resources.getString(R.string.broadcast_receiver_action_loaded))
        registerReceiver(broadcastReceiverServiceMain, filter)
    }

    fun hideSongPane() {
        val fragmentPaneSong: View = binding.fragmentSongPane
        if (fragmentPaneSong.visibility != View.INVISIBLE) {
            runOnUiThread {
                val constraintLayout: ConstraintLayout = binding.constraintMain
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.constraintMain, ConstraintSet.BOTTOM)
                constraintSet.applyTo(constraintLayout)
                fragmentPaneSong.visibility = View.INVISIBLE
            }
        }
    }

    fun showSongPane() {
        val fragmentPaneSong: View = binding.fragmentSongPane
        if (fragmentPaneSong.visibility != View.VISIBLE) {
            runOnUiThread {
                val constraintLayout: ConstraintLayout = binding.constraintMain
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.fragmentSongPane, ConstraintSet.TOP)
                constraintSet.applyTo(constraintLayout)
                binding.fragmentSongPane.visibility = View.VISIBLE
            }
        }
    }

    fun serviceDisconnected() {
        unregisterReceiver(broadcastReceiverServiceMain)
    }

    // endregion onStart

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unbindService(connectionServiceMain)
        serviceDisconnected()
    }

    // endregion lifecycle

    // TODO in fragments
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        sendBroadcastOnOptionsMenuCreated()
        return true
    }

    private fun sendBroadcastOnOptionsMenuCreated() {
        val intent = Intent()
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.action = resources.getString(R.string.broadcast_receiver_action_on_create_options_menu)
        sendBroadcast(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reset_probs -> {
                mediaController.clearProbabilities(applicationContext)
                return true
            }
            R.id.action_lower_probs -> {
                mediaController.lowerProbabilities(applicationContext)
                return true
            }
            R.id.action_add_to_queue -> {
                // TODO a song not in progress will not be added to the queue
                // Pretty sure that never happens though
                // TODO Playlist wil not get added if there is a song in progress
                if (mediaController.isSongInProgress()) {
                    viewModelActivityMain.songToAddToQueue.value?.let { songQueue.addToQueue(it) }
                } else {
                    viewModelActivityMain.playlistToAddToQueue.value?.getSongs()?.let {
                        for (songs in it) {
                            songQueue.addToQueue(songs.id)
                        }
                        // TODO why set the playlist only when the queue is empty?
                        // No current playlist for when the queue runs out, so what?
                        // Will this ever happen?
                        if (songQueue.isEmpty()) {
                            viewModelActivityMain.playlistToAddToQueue.value?.let { rp ->
                                mediaController.setCurrentPlaylist(rp)
                            }
                        }
                    }
                }
                if (!mediaController.isSongInProgress()) {
                    mediaController.playNext()
                    if (!fragmentSongVisible && mediaController.isSongInProgress()) {
                        showSongPane()
                    }
                }
                return true
            }
            R.id.action_add_to_playlist ->{
                // TODO add a playlist to a playlist
                val bundle = Bundle()
                bundle.putSerializable(
                        DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG,
                        viewModelActivityMain.getSongToAddToQueue()
                )
                val dialogFragment: DialogFragment = DialogFragmentAddToPlaylist()
                dialogFragment.arguments = bundle
                dialogFragment.show(supportFragmentManager, "TAG")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun saveFile() {
        SaveFile.saveFile(applicationContext)
    }

    fun fragmentSongVisible(fragmentSongVisible: Boolean) {
        this.fragmentSongVisible = fragmentSongVisible
    }

    fun fragmentSongVisible(): Boolean {
        return fragmentSongVisible
    }

    // region serviceMain

    fun loaded(loaded: Boolean) {
        serviceMain?.loaded(loaded)
    }

    // endregion serviceMain

    companion object {
        // TODO help page
        // TODO check for leaks
        // TODO warn user about resetting probabilities
        // TODO allow user to create backup
        // TODO start shuffle from user picked playlist when play button in notification is tapped
        // TODO AFTER RELEASE
        // Setting to not keep playing after queue is done
        val MUSIC_CONTROL_LOCK: Any = Any()
        const val MENU_ACTION_RESET_PROBS_INDEX = 0
        const val MENU_ACTION_LOWER_PROBS_INDEX = 1
        const val MENU_ACTION_ADD_TO_PLAYLIST_INDEX = 2
        const val MENU_ACTION_SEARCH_INDEX = 3
        const val MENU_ACTION_ADD_TO_QUEUE = 4
    }
}