package com.example.waveplayer.activity_main;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.R;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.databinding.ActivityMainBinding;
import com.example.waveplayer.media_controller.MediaPlayerWUri;
import com.example.waveplayer.media_controller.SaveFile;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.media_controller.ServiceMain;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ActivityMain extends AppCompatActivity {

    // TODO help page
    // TODO check for leaks
    // TODO warn user about resetting probabilities
    // TODO allow user to create backup
    // TODO start shuffle from user picked playlist when play button in notification is tapped

    // TODO AFTER RELEASE
    // Setting to not keep playing after queue is done

    static final String TAG = "ActivityMain";

    public final static Object MUSIC_CONTROL_LOCK = new Object();

    public static final int MENU_ACTION_RESET_PROBS_INDEX = 0;
    public static final int MENU_ACTION_LOWER_PROBS_INDEX = 1;
    public static final int MENU_ACTION_ADD_TO_PLAYLIST_INDEX = 2;
    public static final int MENU_ACTION_SEARCH_INDEX = 3;
    public static final int MENU_ACTION_ADD_TO_QUEUE = 4;

    private ServiceMain serviceMain;
    private ServiceConnection connectionServiceMain;

    private ActivityMainBinding binding;

    private ViewModelActivityMain viewModelActivityMain;
    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;
    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private Observer<String> observerActionBarTitle;
    private Observer<Boolean> observerShowFAB;
    private Observer<Integer> observerFABText;
    private Observer<Integer> observerFABImage;
    private Observer<View.OnClickListener> observerFABOnClickListener;

    private BroadcastReceiver broadcastReceiver;

    private NavController.OnDestinationChangedListener onDestinationChangedListenerToolbar;

    private boolean fragmentSongVisible = false;

    // region lifecycle

    // region onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Log.v(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        createViewModels();
        setUpViewModelActivityMainObservers();
        // Log.v(TAG, "onCreate ended");
    }

    private void createViewModels() {
        viewModelUserPickedPlaylist =
                new ViewModelProvider(this).get(ViewModelUserPickedPlaylist.class);
        viewModelUserPickedSongs =
                new ViewModelProvider(this).get(ViewModelUserPickedSongs.class);
        viewModelActivityMain =
                new ViewModelProvider(this).get(ViewModelActivityMain.class);
    }

    private void setUpViewModelActivityMainObservers() {
        observerActionBarTitle = title -> {
            // Log.v(TAG, "setting ActionBar title");
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
            // Log.v(TAG, "done setting ActionBar title");
        };
        viewModelActivityMain.getActionBarTitle().observe(this, observerActionBarTitle);
        observerShowFAB = showFAB -> {
            // Log.v(TAG, "showing or hiding FAB");
            ExtendedFloatingActionButton fab = binding.fab;
            if (showFAB) {
                // Log.v(TAG, "showing FAB");
                fab.show();
            } else {
                // Log.v(TAG, "hiding FAB");
                fab.hide();
            }
            // Log.v(TAG, "done showing or hiding FAB");
        };
        viewModelActivityMain.showFab().observe(this, observerShowFAB);
        observerFABText = fabText -> {
            // Log.v(TAG, "setFABText start");
            ExtendedFloatingActionButton fab;
            fab = binding.fab;
            fab.setText(fabText);
            // Log.v(TAG, "setFABText end");
        };
        viewModelActivityMain.getFABText().observe(this, observerFABText);
        observerFABImage = drawableID -> {
            // Log.v(TAG, "setting FAB image");
            ExtendedFloatingActionButton fab = binding.fab;
            fab.setIcon(ResourcesCompat.getDrawable(getResources(), drawableID, null));
            // Log.v(TAG, "done setting FAB image");
        };
        viewModelActivityMain.getFABImage().observe(this, observerFABImage);
        observerFABOnClickListener = onClickListener -> {
            // Log.v(TAG, "setting FAB OnClickListener");
            ExtendedFloatingActionButton fab = binding.fab;
            fab.setOnClickListener(null);
            fab.setOnClickListener(onClickListener);
            // Log.v(TAG, "done setting FAB OnClickListener");
        };
        viewModelActivityMain.getFabOnClickListener().observe(this, observerFABOnClickListener);
    }

    // endregion onCreate

    // region onStart

    @Override
    protected void onStart() {
        // Log.v(TAG, "onStart started");
        setUpActionBar();
        super.onStart();
        // Log.v(TAG, "onStart ended");
    }

    private void setUpActionBar() {
        // Log.v(TAG, "Setting up ActionBar");
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorOnPrimary));
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(
                    getResources().getColor(R.color.colorOnPrimary), PorterDuff.Mode.SRC_ATOP);
        }
        centerActionBarTitleAndSetTextSize();
        setUpDestinationChangedListenerForToolbar();
        // Log.v(TAG, "Done setting up ActionBar");
    }

    private void centerActionBarTitleAndSetTextSize() {
        // Log.v(TAG, "Centering the ActionBar title");
        ArrayList<View> textViews = new ArrayList<>();
        getWindow().getDecorView().findViewsWithText(textViews, getTitle(), View.FIND_VIEWS_WITH_TEXT);
        if (!textViews.isEmpty()) {
            AppCompatTextView appCompatTextView = null;
            for (View v : textViews) {
                if (v.getParent() instanceof Toolbar) {
                    appCompatTextView = (AppCompatTextView) v;
                    break;
                }
            }
            if (appCompatTextView != null) {
                ViewGroup.LayoutParams params = appCompatTextView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                appCompatTextView.setLayoutParams(params);
                appCompatTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                appCompatTextView.setTextSize(28);
            }
        }
        // Log.v(TAG, "Centered the ActionBar title");
    }

    private void setUpDestinationChangedListenerForToolbar() {
        // Log.v(TAG, "setUpDestinationChangedListenerForToolbar started");
        onDestinationChangedListenerToolbar = (controller, destination, arguments) ->
                runOnUiThread(() -> {
                    Toolbar toolbar = binding.toolbar;
                    Menu menu = toolbar.getMenu();
                    if (menu.size() > 0) {
                        menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(
                                destination.getId() == R.id.fragmentPlaylist ||
                                        destination.getId() == R.id.fragmentSongs);
                        menu.getItem(ActivityMain.MENU_ACTION_LOWER_PROBS_INDEX).setVisible(
                                destination.getId() == R.id.fragmentPlaylist ||
                                        destination.getId() == R.id.fragmentSongs
                        );
                        menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).setVisible(
                                destination.getId() == R.id.fragmentSong ||
                                        destination.getId() == R.id.fragmentPlaylist);
                        menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(
                                destination.getId() == R.id.fragmentSongs ||
                                        destination.getId() == R.id.fragmentPlaylist ||
                                        destination.getId() == R.id.FragmentPlaylists);
                        menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).setVisible(
                                destination.getId() == R.id.fragmentSong ||
                                        destination.getId() == R.id.fragmentPlaylist);
                    }
                });
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar);
        }
        // Log.v(TAG, "setUpDestinationChangedListenerForToolbar ended");
    }

    // endregion onStart

    // region onResume

    @Override
    protected void onResume() {
        // Log.v(TAG, "onResume started");
        super.onResume();
        startAndBindServiceMain();
        // Log.v(TAG, "onResume ended");
    }

    private void startAndBindServiceMain() {
        // Log.v(TAG, "starting and binding ServiceMain");
        Intent intentServiceMain = new Intent(getApplicationContext(), ServiceMain.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain);
        } else {
            startService(intentServiceMain);
        }
        connectionServiceMain = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.v(ActivityMain.TAG, "onServiceConnected started");
                ServiceMain.ServiceMainBinder binder = (ServiceMain.ServiceMainBinder) service;
                setServiceMain(binder.getService());
                setUpAfterServiceConnection();
                sendBroadcastServiceConnected();
                Log.v(ActivityMain.TAG, "onServiceConnected ended");
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                Log.v(ActivityMain.TAG, "onServiceDisconnected start");
                serviceDisconnected();
            }
        };
        getApplicationContext().bindService(
                intentServiceMain, connectionServiceMain, BIND_AUTO_CREATE | BIND_IMPORTANT);
        // Log.v(TAG, "started and bound ServiceMain");
    }

    public void setServiceMain(ServiceMain serviceMain) {
        // Log.v(TAG, "setServiceMain started");
        this.serviceMain = serviceMain;
        // Log.v(TAG, "setServiceMain ended");
    }

    private void setUpAfterServiceConnection() {
        // Log.v(TAG, "setUpAfterServiceConnection started");
        if (serviceMain.loaded()) {
            if (isPlaying() && !fragmentSongVisible()) {
                hideSongPane();
                navigateTo(R.id.fragmentSong);
            } else {
                navigateTo(R.id.FragmentTitle);
            }
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            if (fragment != null) {
                NavHostFragment.findNavController(fragment).popBackStack(R.id.fragmentLoading, true);
            }
        }
        setUpBroadcastReceiver();
        hideSongPane();
        // Log.v(TAG, "setUpAfterServiceConnection ended");
    }

    public void navigateTo(final int id) {
        // Log.v(TAG, "navigateTo start");
        runOnUiThread(() -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            if (fragment != null) {
                NavController navController = NavHostFragment.findNavController(fragment);
                navController.navigate(id);
            }
        });
        // Log.v(TAG, "navigateTo end");
    }

    void setUpBroadcastReceiver() {
        // Log.v(TAG, "setting up BroadcastReceivers");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_play_pause))) {
                        viewModelActivityMain.setIsPlaying(isPlaying());
                    } else if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_loaded))) {
                        // TODO race condition
                        loaded(true);
                        navigateTo(R.id.FragmentTitle);
                    } else if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_new_song))) {
                        // TODO serviceMain.setCurrentSong()... might not be needed
                        viewModelActivityMain.setCurrentSong(serviceMain.getCurrentAudioUri());
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(getResources().getString(R.string.broadcast_receiver_action_new_song));
        filter.addAction(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        filter.addAction(getResources().getString(R.string.broadcast_receiver_action_loaded));
        registerReceiver(broadcastReceiver, filter);
        // Log.v(TAG, "done setting up BroadcastReceivers");
    }

    public void hideSongPane() {
        // Log.v(TAG, "sending runnable to hide song pane");
        final View fragmentPaneSong = binding.fragmentSongPane;
        if (fragmentPaneSong.getVisibility() != View.INVISIBLE) {
            runOnUiThread(() -> {
                // Log.v(TAG, "hiding song pane");
                fragmentPaneSong.setVisibility(View.INVISIBLE);
                ConstraintLayout constraintLayout = binding.constraintMain;
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.constraintMain, ConstraintSet.BOTTOM);
                constraintSet.applyTo(constraintLayout);
                // Log.v(TAG, "done hiding song pane");
            });
        }
        // Log.v(TAG, "done sending runnable to hide song pane");
    }

    public void showSongPane() {
        // Log.v(TAG, "sending runnable to show song pane");
        final View fragmentPaneSong = binding.fragmentSongPane;
        if (fragmentPaneSong.getVisibility() != View.VISIBLE) {
            runOnUiThread(() -> {
                // Log.v(TAG, "showing song pane");
                binding.fragmentSongPane.setVisibility(View.VISIBLE);
                ConstraintLayout constraintLayout = binding.constraintMain;
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.fragmentSongPane, ConstraintSet.TOP);
                constraintSet.applyTo(constraintLayout);
                // Log.v(TAG, "done showing song pane");
            });
        }
        // Log.v(TAG, "done sending runnable to show song pane");
    }

    private void sendBroadcastServiceConnected() {
        Log.v(ActivityMain.TAG, "Sending Broadcast onServiceConnected");
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        sendBroadcast(intent);
        Log.v(ActivityMain.TAG, "Done sending Broadcast onServiceConnected");
    }

    public void serviceDisconnected() {
        // Log.v(TAG, "serviceDisconnected started");
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        // Log.v(TAG, "serviceDisconnected end");
    }

    // endregion onResume

    @Override
    protected void onPause() {
        // Log.v(TAG, "onPause started");
        super.onPause();
        // Log.v(TAG, "onPause ended");
    }

    @Override
    protected void onStop() {
        // Log.v(TAG, "onStop started");
        super.onStop();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar);
        }
        // Log.v(TAG, "onStop ended");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TODO do this onPause, by making LiveData for the serviceMain
        getApplicationContext().unbindService(connectionServiceMain);
        connectionServiceMain = null;
        serviceMain = null;
        serviceDisconnected();
        // end TODO

        onDestinationChangedListenerToolbar = null;
        ExtendedFloatingActionButton fab = binding.fab;
        fab.setOnClickListener(null);
        observerActionBarTitle = null;
        observerShowFAB = null;
        observerFABText = null;
        observerFABImage = null;
        observerFABOnClickListener = null;
        viewModelActivityMain.getActionBarTitle().removeObservers(this);
        viewModelActivityMain.showFab().removeObservers(this);
        viewModelActivityMain.getFABText().removeObservers(this);
        viewModelActivityMain.getFABImage().removeObservers(this);
        viewModelActivityMain.getFabOnClickListener().removeObservers(this);
        viewModelActivityMain = null;
        viewModelUserPickedPlaylist = null;
        viewModelUserPickedSongs = null;
        binding = null;
    }

    // endregion lifecycle

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Log.v(TAG, "onCreateOptionsMenu start");
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        sendBroadcastOnOptionsMenuCreated();
        // Log.v(TAG, "onCreateOptionsMenu end");
        return true;
    }

    private void sendBroadcastOnOptionsMenuCreated() {
        // Log.v(TAG, "sendBroadcastOnOptionsMenuCreated start");
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu));
        sendBroadcast(intent);
        // Log.v(TAG, "sendBroadcastOnOptionsMenuCreated end");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Log.v(TAG, "onOptionsItemSelected start");
        if (item.getItemId() == R.id.action_reset_probs) {
            clearProbabilities();
            return true;
        } else if (item.getItemId() == R.id.action_lower_probs) {
            lowerProbabilities();
            return true;
        } else if (item.getItemId() == R.id.action_add_to_queue) {
            if (isSongInProgress() && viewModelActivityMain.getSongToAddToQueue() != null) {
                addToQueue(viewModelActivityMain.getSongToAddToQueue());
            } else if (viewModelActivityMain.getPlaylistToAddToQueue() != null) {
                for (Song songs : viewModelActivityMain.getPlaylistToAddToQueue().getSongs()) {
                    addToQueue(songs.id);
                }
                if (songQueueIsEmpty()) {
                    setCurrentPlaylist(viewModelActivityMain.getPlaylistToAddToQueue());
                }
                if (!fragmentSongVisible() && isSongInProgress()) {
                    showSongPane();
                }
            }
            if (!isSongInProgress()) {
                playNext();
                if (!fragmentSongVisible() && isSongInProgress()) {
                    showSongPane();
                }
            }
        }
        // Log.v(TAG, "onOptionsItemSelected action_unknown end");
        return super.onOptionsItemSelected(item);
    }

    public void saveFile() {
        // Log.v(TAG, "saveFile start");
        SaveFile.saveFile(serviceMain.getApplicationContext());
        // Log.v(TAG, "saveFile end");
    }

    public void showToast(int idMessage) {
        // Log.v(TAG, "showToast start");
        Toast toast = Toast.makeText(getApplicationContext(), idMessage, Toast.LENGTH_LONG);
        if (toast.getView() != null) {
            toast.getView().getBackground().setColorFilter(
                    getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
        }
        TextView text = toast.getView().findViewById(android.R.id.message);
        text.setTextSize(16);
        toast.show();
        // Log.v(TAG, "showToast end");
    }

    public void hideKeyboard(View view) {
        // Log.v(TAG, "hideKeyboard start");
        InputMethodManager imm = (InputMethodManager)
                getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        // Log.v(TAG, "hideKeyboard end");
    }

    public void fragmentLoadingStarted() {
        startAndBindServiceMain();
    }

    public void permissionGranted() {
        if (serviceMain != null) {
            serviceMain.permissionGranted();
        }
    }

    public void setUserPickedPlaylist(RandomPlaylist randomPlaylist) {
        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist);
    }

    public void clearUserPickedSongs() {
        viewModelUserPickedSongs.clearUserPickedSongs();
    }

    public void addUserPickedSong(Song song) {
        viewModelUserPickedSongs.addUserPickedSong(song);
    }

    public RandomPlaylist getUserPickedPlaylist() {
        return viewModelUserPickedPlaylist.getUserPickedPlaylist();
    }

    //-------------------------------------------------

    public boolean songInProgress() {
        // Log.v(TAG, "songInProgress start");
        // Log.v(TAG, "songInProgress end");
        return (serviceMain != null) && serviceMain.isSongInProgress();
    }

    public boolean isPlaying() {
        // Log.v(TAG, "isPlaying start");
        // Log.v(TAG, "isPlaying end");
        if (serviceMain == null) {
            return false;
        }
        return serviceMain.isPlaying();
    }

    public void fragmentSongVisible(boolean fragmentSongVisible) {
        // Log.v(TAG, "fragmentSongVisible start");
        this.fragmentSongVisible = fragmentSongVisible;
        // Log.v(TAG, "fragmentSongVisible end");
    }

    public boolean fragmentSongVisible() {
        // Log.v(TAG, "fragmentSongVisible start and end");
        return fragmentSongVisible;
    }

    // region serviceMain

    public void loaded(boolean loaded) {
        serviceMain.loaded(loaded);
    }

    public Song getCurrentSong() {
        return serviceMain.getCurrentSong();
    }

    public void addToQueue(Long songID) {
        // Log.v(TAG, "addToQueue start");
        serviceMain.addToQueue(songID);
        // Log.v(TAG, "addToQueue end");
    }

    public boolean songQueueIsEmpty() {
        return serviceMain.songQueueIsEmpty();
    }

    public void clearSongQueue() {
        serviceMain.clearSongQueue();
    }

    public void goToFrontOfQueue() {
        serviceMain.goToFrontOfQueue();
    }

    public RandomPlaylist getCurrentPlaylist() {
        // Log.v(TAG, "getCurrentPlaylist start");
        // Log.v(TAG, "getCurrentPlaylist end");
        return serviceMain.getCurrentPlaylist();
    }

    public void setCurrentPlaylist(RandomPlaylist userPickedPlaylist) {
        // Log.v(TAG, "setCurrentPlaylist start");
        serviceMain.setCurrentPlaylist(userPickedPlaylist);
        // Log.v(TAG, "setCurrentPlaylist end");
    }

    public void setCurrentPlaylistToMaster() {
        // Log.v(TAG, "setCurrentPlaylistToMaster start");
        serviceMain.setCurrentPlaylistToMaster();
        // Log.v(TAG, "setCurrentPlaylistToMaster end");
    }

    public void clearProbabilities() {
        serviceMain.clearProbabilities();
    }

    public void lowerProbabilities() {
        serviceMain.lowerProbabilities();
    }

    public AudioUri getCurrentAudioUri() {
        // Log.v(TAG, "getCurrentAudioUri start");
        // Log.v(TAG, "getCurrentAudioUri end");
        return serviceMain.getCurrentAudioUri();
    }

    public Uri getCurrentUri() {
        return serviceMain.getCurrentUri();
    }

    public int getCurrentTime() {
        // Log.v(TAG, "getCurrentTime start");
        // Log.v(TAG, "getCurrentTime end");
        return serviceMain.getCurrentTime();
    }

    public MediaPlayerWUri getCurrentMediaPlayerWUri() {
        return serviceMain.getCurrentMediaPlayerWUri();
    }

    public boolean isSongInProgress() {
        return serviceMain.isSongInProgress();
    }

    public boolean isShuffling() {
        // Log.v(TAG, "shuffling start");
        // Log.v(TAG, "shuffling end");
        return serviceMain.isShuffling();
    }

    public void setShuffling(boolean shuffling) {
        // Log.v(TAG, "set shuffling start");
        serviceMain.setShuffling(shuffling);
        // Log.v(TAG, "set shuffling end");
    }

    public boolean isLooping() {
        // Log.v(TAG, "looping start");
        // Log.v(TAG, "looping end");
        return serviceMain.isLooping();
    }

    public void setLooping(boolean looping) {
        // Log.v(TAG, "set looping start");
        serviceMain.setLooping(looping);
        // Log.v(TAG, "set looping end");
    }

    public boolean isLoopingOne() {
        // Log.v(TAG, "loopingOne start");
        // Log.v(TAG, "loopingOne end");
        return serviceMain.isLoopingOne();
    }

    public void setLoopingOne(boolean loopingOne) {
        // Log.v(TAG, "set loopingOne start");
        serviceMain.setLoopingOne(loopingOne);
        // Log.v(TAG, "set loopingOne end");
    }

    public void pauseOrPlay() {
        if (serviceMain.getCurrentAudioUri() != null) {
            serviceMain.pauseOrPlay();
        }
    }

    public void playNext() {
        // Log.v(TAG, "playNext start");
        serviceMain.playNext();
        // Log.v(TAG, "playNext end");
    }

    public void playPrevious() {
        serviceMain.playPrevious();
    }

    public void seekTo(int progress) {
        // Log.v(TAG, "seekTo start");
        serviceMain.seekTo(progress);
        // Log.v(TAG, "seekTo end");
    }

    public Song getSong(long songID) {
        return serviceMain.getSong(songID);
    }

    public List<Song> getAllSongs() {
        return serviceMain.getAllSongs();
    }

    public RandomPlaylist getMasterPlaylist() {
        return serviceMain.getMasterPlaylist();
    }

    public RandomPlaylist getPlaylist(String playlistName) {
        return serviceMain.getPlaylist(playlistName);
    }

    public List<RandomPlaylist> getPlaylists() {
        return serviceMain.getPlaylists();
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        serviceMain.addPlaylist(randomPlaylist);
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        serviceMain.addPlaylist(position, randomPlaylist);
    }


    public void removePlaylist(RandomPlaylist randomPlaylist) {
        serviceMain.removePlaylist(randomPlaylist);
    }

    public double getMaxPercent() {
        return serviceMain.getMaxPercent();
    }

    public void setMaxPercent(double maxPercent) {
        serviceMain.setMaxPercent(maxPercent);
    }

    public double getPercentChangeUp() {
        return serviceMain.getPercentChangeUp();
    }

    public void setPercentChangeUp(double percentChangeUp) {
        serviceMain.setPercentChangeUp(percentChangeUp);
    }

    public double getPercentChangeDown() {
        return serviceMain.getPercentChangeDown();
    }

    public void setPercentChangeDown(double percentChangeDown) {
        serviceMain.setPercentChangeDown(percentChangeDown);
    }

    public void popBackStack(Fragment fragment) {
        NavController navController = NavHostFragment.findNavController(
                fragment);
        navController.popBackStack();
    }

    // endregion serviceMain

}