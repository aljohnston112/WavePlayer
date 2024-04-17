package com.fourthFinger.pinkyPlayer.activity_main

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.NavUtil.Companion.navigateTo
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ServiceMain
import com.fourthFinger.pinkyPlayer.databinding.ActivityMainBinding
import com.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class ActivityMain : AppCompatActivity() {

    // TODO Navigate by responding to LiveData of Actions and Destinations

    private lateinit var binding: ActivityMainBinding

    private lateinit var serviceMain: ServiceMain

    private val viewModelActivityMain by viewModels<ViewModelActivityMain>{
        ViewModelActivityMain.Factory
    }

    private lateinit var mediaPlayerManager: MediaPlayerManager

    private var loaded = false

    // region lifecycle
    // region onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpOnDestinationChangedListener()
        setUpViewModelActivityMainObservers()
        mediaPlayerManager = (application as ApplicationMain).mediaPlayerManager
        mediaPlayerManager.songInProgress.observe(this) { songInProgress ->
            val currentFragment: Fragment? = supportFragmentManager.findFragmentById(
                binding.navHostFragment.id
            )
            if (currentFragment != null) {
                val n = NavHostFragment.findNavController(currentFragment)
                if (n.currentDestination?.id != R.id.fragmentSong &&
                    songInProgress == true
                ) {
                    showSongPane()
                }
                if (songInProgress) {
                    val toolbar = binding.toolbar
                    if (toolbar.menu.size() > 0)
                        toolbar.menu.getItem(MENU_ACTION_QUEUE).isVisible = true
                }
            }
        }
    }

    private fun setUpOnDestinationChangedListener() {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.let {
            NavHostFragment.findNavController(it).addOnDestinationChangedListener(
                onDestinationChangedListenerSongPane
            )
        }
    }

    private val onDestinationChangedListenerSongPane =
        { _: NavController, destination: NavDestination, _: Bundle? ->
            if (destination.id != R.id.fragmentLoading) {
                if (destination.id != R.id.fragmentSong) {
                    if (mediaPlayerManager.isSongInProgress() == true) {
                        fragmentSongVisible(false)
                        showSongPane()
                    }
                } else {
                    fragmentSongVisible(true)
                    hideSongPane()
                }
            }
        }

    private fun fragmentSongVisible(fragmentSongVisible: Boolean) {
        viewModelActivityMain.fragmentSongVisible(fragmentSongVisible)
    }

    private fun showSongPane() {
        val fragmentPaneSong: View = binding.fragmentSongPane
        if (fragmentPaneSong.visibility != VISIBLE) {
            runOnUiThread {
                val constraintLayout: ConstraintLayout = binding.constraintMain
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    R.id.fab,
                    ConstraintSet.BOTTOM,
                    R.id.fragmentSongPane,
                    ConstraintSet.TOP
                )
                constraintSet.applyTo(constraintLayout)
                binding.fragmentSongPane.visibility = VISIBLE
                viewModelActivityMain.songPaneVisible(VISIBLE)
            }
        }
    }

    private fun hideSongPane() {
        val fragmentPaneSong: View = binding.fragmentSongPane
        if (fragmentPaneSong.visibility != INVISIBLE) {
            runOnUiThread {
                val constraintLayout: ConstraintLayout = binding.constraintMain
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    R.id.fab,
                    ConstraintSet.BOTTOM,
                    R.id.constraintMain,
                    ConstraintSet.BOTTOM
                )
                constraintSet.applyTo(constraintLayout)
                fragmentPaneSong.visibility = INVISIBLE
                viewModelActivityMain.songPaneVisible(INVISIBLE)
            }
        }
    }

    private fun setUpViewModelActivityMainObservers() {
        val fab: ExtendedFloatingActionButton = binding.fab
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
        viewModelActivityMain.fabImageID.observe(this) { drawableID: Int ->
            fab.icon = ResourcesCompat.getDrawable(resources, drawableID, theme)
        }
        viewModelActivityMain.fabOnClickListener.observe(this) {
            fab.setOnClickListener(it)
        }
        val toolbar = binding.toolbar
        viewModelActivityMain.actionBarTitle.observe(this) { title ->
            toolbar.title = title
        }
    }

    // endregion onCreate

    // region onStart
    override fun onStart() {
        setUpToolbar()
        startAndBindServiceMain()
        setUpBroadcastReceiver()
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastReceiverServiceMain)
    }

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
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

    private val connectionServiceMain = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder: ServiceMain.ServiceMainBinder = service as ServiceMain.ServiceMainBinder
            serviceMain = binder.getService()
            sendBroadcastServiceConnected()
            setUpAfterServiceConnection()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    private fun sendBroadcastServiceConnected() {
        val intent = Intent()
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.action = resources.getString(R.string.action_service_connected)
        sendBroadcast(intent)
    }

    private fun setUpAfterServiceConnection() {
        goToFirstFragment()
        hideSongPane()
    }

    private fun goToFirstFragment() {
        val fragment: Fragment? = supportFragmentManager.findFragmentById(
            binding.navHostFragment.id
        )
        if (fragment != null) {
            if (loaded) {
                if (mediaPlayerManager.isPlaying.value == true) {
                    hideSongPane()
                    navigateTo(fragment, R.id.fragmentSong)
                } else {
                    navigateTo(fragment, R.id.FragmentTitle)
                }

            } else {
                // TODO inclusive why?
//                val navController = NavHostFragment.findNavController(fragment)
//                navController.popBackStack(
//                    R.id.fragmentLoading,
//                    true
//                )
            }
        }
    }

    private fun setUpBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.action_loaded))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiverServiceMain, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiverServiceMain, filter)
        }
    }

    private var broadcastReceiverServiceMain = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                (resources.getString(R.string.action_loaded)) -> {
                    loaded = true
                    val fragment: Fragment? = supportFragmentManager.findFragmentById(
                        binding.navHostFragment.id
                    )
                    fragment?.let {
                        navigateTo(it, R.id.FragmentTitle)
                    }
                }
            }
        }
    }
// endregion onStart

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unbindService(connectionServiceMain)
        removeListeners()
    }

    private fun removeListeners() {
        val fragment: Fragment? = supportFragmentManager.findFragmentById(
            binding.navHostFragment.id
        )
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                onDestinationChangedListenerSongPane
            )
        }
    }

// endregion lifecycle

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        if (mediaPlayerManager.isSongInProgress() == true) {
            val toolbar = binding.toolbar
            if (toolbar.menu.size() > 0)
                toolbar.menu.getItem(MENU_ACTION_QUEUE).isVisible = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val mediaSession = (application as ApplicationMain).mediaSession
        when (item.itemId) {
            R.id.action_reset_probs -> {
                viewModelActivityMain.resetProbabilities(
                    applicationContext,
                    mediaSession
                )
                return true
            }

            R.id.action_lower_probs -> {
                viewModelActivityMain.lowerProbabilities(
                    applicationContext,
                    mediaSession
                )
                return true
            }

            R.id.action_add_to_queue -> {
                viewModelActivityMain.actionAddToQueue(applicationContext)
                if (viewModelActivityMain.fragmentSongVisible.value == false &&
                    mediaPlayerManager.isSongInProgress() == true
                ) {
                    showSongPane()
                }
                return true
            }

            R.id.action_add_to_playlist -> {
                viewModelActivityMain.actionAddToPlaylist(supportFragmentManager)
                return true
            }

            R.id.action_queue -> {
                val fragment: Fragment? = supportFragmentManager.findFragmentById(
                    binding.navHostFragment.id
                )
                if (fragment != null) {
                    navigateTo(fragment, R.id.fragmentQueue)
                }
            }

        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        // TODO help page
        // TODO check for leaks
        // TODO warn user about resetting probabilities
        // TODO add a way to display folder file is in
        // TODO make media controls work with bluetooth
        // TODO actually handle Uri passed in via an external Intent
        // TODO add a way to exclude a folder.
        // TODO Paid version setting to discover songs not in the playlist while listening to the playlist
        // TODO allow user to create backup
        // TODO start shuffle from user picked playlist when play button in notification is tapped
        // TODO Setting to not keep playing after queue is done
        // TODO User time boundaries on the algorithm
        // TODO Paid version with time boundaries
        // TODO Paid version with ability to make new playlist out of favorite songs
        // TODO consider exo player
        // TODO Make a playlist based on favorites from the algorithm
        // TODO Make a UI for the queue
        // TODO notification art doesn't work
        // TODO song pane art has blue background, but it should be black.
        // TODO remove songs from master playlist that are in a folder
        // TODO check what happens when files are removed.
        // TODO add feature to show what is playing next
        // TODO play song from file system (mp3 intent)
        val MUSIC_CONTROL_LOCK: Any = Any()

        const val MENU_ACTION_RESET_PROBABILITIES_INDEX = 0
        const val MENU_ACTION_LOWER_PROBABILITIES_INDEX = 1
        const val MENU_ACTION_ADD_TO_PLAYLIST_INDEX = 2
        const val MENU_ACTION_SEARCH_INDEX = 3
        const val MENU_ACTION_ADD_TO_QUEUE_INDEX = 4
        const val MENU_ACTION_QUEUE = 5

    }

}