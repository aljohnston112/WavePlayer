package io.fourth_finger.pinky_player.activity_main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
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
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import io.fourth_finger.pinky_player.NavUtil.Companion.navigateTo
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.ServiceMain
import io.fourth_finger.pinky_player.databinding.ActivityMainBinding


enum class MenuActionIndex {
    MENU_ACTION_RESET_PROBABILITIES_INDEX,
    MENU_ACTION_LOWER_PROBABILITIES_INDEX,
    MENU_ACTION_ADD_TO_PLAYLIST_INDEX,
    MENU_ACTION_SEARCH_INDEX,
    MENU_ACTION_ADD_TO_QUEUE_INDEX,
    MENU_ACTION_QUEUE_INDEX;
}

class ActivityMain : AppCompatActivity() {

    // TODO Navigate by responding to LiveData of Actions and Destinations

    private lateinit var binding: ActivityMainBinding

    private val viewModelActivityMain by viewModels<ViewModelActivityMain> {
        ViewModelActivityMain.Companion.Factory
    }

    private var loaded = false

    private var broadcastReceiverServiceMain = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                (resources.getString(R.string.action_loaded)) -> {
                    loaded = true
                    val fragment: Fragment? = supportFragmentManager.findFragmentById(
                        binding.navHostFragment.id
                    )
                    fragment?.let {
                        val navController: NavController =
                            NavHostFragment.findNavController(fragment)
                        navController.popBackStack()
                        navigateTo(it, R.id.FragmentTitle)
                    }
                }
            }
        }
    }

    private var onDestinationChangedListenerSongPane: NavController.OnDestinationChangedListener? =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.fragmentLoading) {
                if (destination.id != R.id.fragmentSong) {
                    if (viewModelActivityMain.songInProgress.value == true) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setUpOnDestinationChangedListener()
        setUpViewModelActivityMainObservers()
    }

    private fun setUpOnDestinationChangedListener() {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.let {
            NavHostFragment.findNavController(it).addOnDestinationChangedListener(
                onDestinationChangedListenerSongPane!!
            )
        }
    }

    private fun setUpViewModelActivityMainObservers() {
        setUpFABObservers()

        val toolbar = binding.toolbar
        viewModelActivityMain.actionBarTitle.observe(this) { title ->
            toolbar.title = title
        }

        viewModelActivityMain.songInProgress.observe(this) { songInProgress ->
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
                    makeToolbarQueueActionVisible()
                }
            }
        }
    }

    private fun setUpFABObservers() {
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
            fab.icon = ResourcesCompat.getDrawable(
                resources,
                drawableID,
                theme
            )
        }
        viewModelActivityMain.fabOnClickListener.observe(this) {
            fab.setOnClickListener(it)
        }
    }

    private fun makeToolbarQueueActionVisible() {
        val toolbar = binding.toolbar
        if (toolbar.menu.size() > 0) {
            toolbar.menu.getItem(MenuActionIndex.MENU_ACTION_QUEUE_INDEX.ordinal).isVisible = true
        }
    }

    override fun onStart() {
        super.onStart()
        setUpToolbar()
        startServiceMain()
        setUpBroadcastReceiver()
    }

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun startServiceMain() {
        val intentServiceMain = Intent(
            applicationContext,
            ServiceMain::class.java
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain)
        } else {
            startService(intentServiceMain)
        }
    }

    private fun setUpBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.action_loaded))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                broadcastReceiverServiceMain,
                filter,
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                broadcastReceiverServiceMain,
                filter
            )
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastReceiverServiceMain)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeListeners()
    }

    private fun removeListeners() {
        val fragment: Fragment? = supportFragmentManager.findFragmentById(
            binding.navHostFragment.id
        )
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                onDestinationChangedListenerSongPane!!
            )
        }
        onDestinationChangedListenerSongPane = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        if (viewModelActivityMain.songInProgress.value == true) {
            makeToolbarQueueActionVisible()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reset_probs -> {
                viewModelActivityMain.resetProbabilities(applicationContext)
                return true
            }

            R.id.action_lower_probs -> {
                viewModelActivityMain.lowerProbabilities(applicationContext)
                return true
            }

            R.id.action_add_to_queue -> {
                viewModelActivityMain.actionAddToQueue(applicationContext)
                if (viewModelActivityMain.fragmentSongVisible.value == false &&
                    viewModelActivityMain.songInProgress.value == true
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

    }

}