package com.example.waveplayer.activity_main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
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
import com.example.waveplayer.media_controller.MediaController;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.SaveFile;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.service_main.ServiceMain;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;

public class ActivityMain extends AppCompatActivity {

    // TODO help page
    // TODO check for leaks
    // TODO warn user about resetting probabilities
    // TODO allow user to create backup
    // TODO start shuffle from user picked playlist when play button in notification is tapped

    // TODO AFTER RELEASE
    // Setting to not keep playing after queue is done

    static final String TAG = "ActivityMain";

    public final static Object lock = new Object();

    public static final int MENU_ACTION_RESET_PROBS_INDEX = 0;
    public static final int MENU_ACTION_LOWER_PROBS_INDEX = 1;
    public static final int MENU_ACTION_ADD_TO_PLAYLIST_INDEX = 2;
    public static final int MENU_ACTION_SEARCH_INDEX = 3;
    public static final int MENU_ACTION_ADD_TO_QUEUE = 4;

    private static final String KEY_BOOLEAN_LOADED = "KEY_BOOLEAN_LOADED";

    private ActivityMainBinding binding;

    private ServiceMain serviceMain;

    private MediaController mediaController;

    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private ViewModelActivityMain viewModelActivityMain;

    private Observer<String> observerActionBarTitle;
    private Observer<Boolean> observerShowFAB;
    private Observer<Integer> observerFABText;
    private Observer<Integer> observerFABImage;
    private Observer<View.OnClickListener> observerFABOnClickListener;

    private boolean loaded = false;

    public void loaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void setServiceMain(ServiceMain serviceMain) {
        // Log.v(TAG, "setServiceMain started");
        this.serviceMain = serviceMain;
        setUpAfterServiceConnection();
        // Log.v(TAG, "setServiceMain ended");
    }

    private void setUpAfterServiceConnection() {
        // Log.v(TAG, "setUpAfterServiceConnection started");
        if (loaded) {
            mediaController = MediaController.getInstance(getApplicationContext());
            if (!fragmentSongVisible()) {
                if (MediaController.getInstance(getApplicationContext()).isPlaying()) {
                    navigateTo(R.id.fragmentSong);
                } else {
                    navigateTo(R.id.FragmentTitle);
                }
            }
        } else {
            mediaController = MediaController.getInstance(getApplicationContext());
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            if (fragment != null) {
                NavHostFragment.findNavController(fragment).popBackStack(R.id.fragmentLoading, true);
            }
        }
        serviceMain.permissionGranted();
        setUpBroadcastReceiver();
        setUpSongPane();
        // Log.v(TAG, "setUpAfterServiceConnection ended");
    }

    public void serviceDisconnected() {
        // Log.v(TAG, "serviceDisconnected started");
        unregisterReceiver(broadcastReceiver);
        mediaController = null;
        // Log.v(TAG, "serviceDisconnected end");
    }

    private ServiceConnection connectionServiceMain;

    private BroadcastReceiver broadcastReceiver;

    private OnDestinationChangedListenerToolbar onDestinationChangedListenerToolbar;

    // TODO get rid of?

    private boolean isSong;

    public void isSong(boolean isSong) {
        // Log.v(TAG, "isSong started");
        this.isSong = isSong;
        // Log.v(TAG, "isSong ended");
    }

    private Long songToAddToQueue;

    public void setSongToAddToQueue(Long songID) {
        // Log.v(TAG, "setSongToAddToQueue started");
        songToAddToQueue = songID;
        // Log.v(TAG, "setSongToAddToQueue ened");
    }

    private RandomPlaylist playlistToAddToQueue;

    public void setPlaylistToAddToQueue(RandomPlaylist randomPlaylist) {
        // Log.v(TAG, "setPlaylistToAddToQueue started");
        this.playlistToAddToQueue = randomPlaylist;
        // Log.v(TAG, "setPlaylistToAddToQueue ended");
    }

    private boolean fragmentSongVisible = false;

    public void fragmentSongVisible(boolean fragmentSongVisible) {
        // Log.v(TAG, "fragmentSongVisible start");
        this.fragmentSongVisible = fragmentSongVisible;
        // Log.v(TAG, "fragmentSongVisible end");
    }

    public boolean fragmentSongVisible() {
        // Log.v(TAG, "fragmentSongVisible start and end");
        return fragmentSongVisible;
    }

    // region lifecycle

    // region onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Log.v(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState != null) {
            loaded = savedInstanceState.getBoolean(KEY_BOOLEAN_LOADED, false);
        }
        viewModelUserPickedPlaylist =
                new ViewModelProvider(this).get(ViewModelUserPickedPlaylist.class);
        viewModelUserPickedSongs =
                new ViewModelProvider(this).get(ViewModelUserPickedSongs.class);
        viewModelActivityMain =
                new ViewModelProvider(this).get(ViewModelActivityMain.class);
        setUpViewModelActivityMain();
        // Log.v(TAG, "onCreate ended");
    }

    private void setUpViewModelActivityMain() {
        observerActionBarTitle = s -> {
            // Log.v(TAG, "setting ActionBar title");
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(s);
            }
            // Log.v(TAG, "done setting ActionBar title");
        };
        viewModelActivityMain.getActionBarTitle().observe(this, observerActionBarTitle);
        observerShowFAB = b -> {
            // Log.v(TAG, "showing or hiding FAB");
            ExtendedFloatingActionButton fab = binding.fab;
            if (b) {
                // Log.v(TAG, "showing FAB");
                fab.show();
            } else {
                // Log.v(TAG, "hiding FAB");
                fab.hide();
            }
            // Log.v(TAG, "done showing or hiding FAB");
        };
        viewModelActivityMain.showFab().observe(this, observerShowFAB);
        observerFABText = s -> {
            // Log.v(TAG, "setFABText start");
            ExtendedFloatingActionButton fab;
            fab = binding.fab;
            fab.setText(s);
            // Log.v(TAG, "setFABText end");
        };
        viewModelActivityMain.getFABText().observe(this, observerFABText);
        observerFABImage = i -> {
            // Log.v(TAG, "setting FAB image");
            ExtendedFloatingActionButton fab = binding.fab;
            fab.setIcon(ResourcesCompat.getDrawable(getResources(), i, null));
            // Log.v(TAG, "done setting FAB image");
        };
        viewModelActivityMain.getFABImage().observe(this, observerFABImage);
        observerFABOnClickListener = l -> {
            // Log.v(TAG, "setting FAB OnClickListener");
            ExtendedFloatingActionButton fab = binding.fab;
            fab.setOnClickListener(null);
            fab.setOnClickListener(l);
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

    private void startAndBindServiceMain() {
        // Log.v(TAG, "starting and binding ServiceMain");
        Intent intentServiceMain = new Intent(getApplicationContext(), ServiceMain.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain);
        } else {
            startService(intentServiceMain);
        }
        connectionServiceMain = new ConnectionServiceMain(this);
        getApplicationContext().bindService(
                intentServiceMain, connectionServiceMain, BIND_AUTO_CREATE | BIND_IMPORTANT);
        // Log.v(TAG, "started and bound ServiceMain");
    }

    public List<Song> getAllSongs() {
        return mediaController.getAllSongs();
    }

    void setUpBroadcastReceiver() {
        // Log.v(TAG, "setting up BroadcastReceivers");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_next)) ||
                            action.equals(getResources().getString(
                                    R.string.broadcast_receiver_action_play_pause)) ||
                            action.equals(getResources().getString(
                                    R.string.broadcast_receiver_action_previous))) {
                        // TODO
                    } else if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_loaded))) {
                        loaded(true);
                        navigateTo(R.id.FragmentTitle);
                    } else if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_on_completion))) {
                        updateAudioUri();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(getResources().getString(R.string.broadcast_receiver_action_on_completion));
        filter.addAction(getResources().getString(R.string.broadcast_receiver_action_next));
        filter.addAction(getResources().getString(R.string.broadcast_receiver_action_previous));
        filter.addAction(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        filter.addAction(getResources().getString(R.string.broadcast_receiver_action_loaded));
        registerReceiver(broadcastReceiver, filter);
        // Log.v(TAG, "done setting up BroadcastReceivers");
    }

    void setUpSongPane() {
        // Log.v(TAG, "setting up song pane");
        hideSongPane();
        // Log.v(TAG, "done setting up song pane");
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
        onDestinationChangedListenerToolbar = new OnDestinationChangedListenerToolbar(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar);
        }
        // Log.v(TAG, "setUpDestinationChangedListenerForToolbar ended");
    }

    // endregion onStart

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean(KEY_BOOLEAN_LOADED, loaded);
    }

    @Override
    protected void onStop() {
        // Log.v(TAG, "onStop started");
        super.onStop();
        getApplicationContext().unbindService(connectionServiceMain);
        connectionServiceMain = null;
        serviceDisconnected();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar);
        }
        onDestinationChangedListenerToolbar = null;
        ExtendedFloatingActionButton fab = binding.fab;
        fab.setOnClickListener(null);
        viewModelUserPickedPlaylist = null;
        viewModelUserPickedSongs = null;
        viewModelActivityMain.getActionBarTitle().removeObservers(this);
        viewModelActivityMain.showFab().removeObservers(this);
        viewModelActivityMain.getFABText().removeObservers(this);
        viewModelActivityMain.getFABImage().removeObservers(this);
        viewModelActivityMain.getFabOnClickListener().removeObservers(this);
        observerActionBarTitle = null;
        observerShowFAB = null;
        observerFABText = null;
        observerFABImage = null;
        observerFABOnClickListener = null;
        serviceMain = null;
        playlistToAddToQueue = null;
        // Log.v(TAG, "onStop ended");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        viewModelActivityMain = null;
    }

    @Override
    protected void onResume() {
        // Log.v(TAG, "onResume started");
        super.onResume();
        if (loaded) {
            startAndBindServiceMain();
        }
        // Log.v(TAG, "onResume ended");
    }

    @Override
    protected void onPause() {
        // Log.v(TAG, "onPause started");
        super.onPause();
        // Log.v(TAG, "onPause ended");
    }

    // endregion lifecycle

    // region UI

    // region updateSongUI

    public int getCurrentTime() {
        // Log.v(TAG, "getCurrentTime start");
        // Log.v(TAG, "getCurrentTime end");
        return mediaController.getCurrentTime();
    }

    // endregion updateSongUI

    public void hideKeyboard(View view) {
        // Log.v(TAG, "hideKeyboard start");
        InputMethodManager imm = (InputMethodManager)
                getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        // Log.v(TAG, "hideKeyboard end");
    }

    public void showToast(int idMessage) {
        // Log.v(TAG, "showToast start");
        Toast toast = Toast.makeText(getApplicationContext(), idMessage, Toast.LENGTH_LONG);
        toast.getView().getBackground().setColorFilter(
                getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
        TextView text = toast.getView().findViewById(android.R.id.message);
        text.setTextSize(16);
        toast.show();
        // Log.v(TAG, "showToast end");
    }

    // endregion UI

    // region playbackControls

    public void addToQueue(Long songID) {
        // Log.v(TAG, "addToQueue start");
        mediaController.addToQueue(songID);
        // Log.v(TAG, "addToQueue end");
    }

    public void addToQueueAndPlay(Long songID) {
        // Log.v(TAG, "addToQueueAndPlay start");
        mediaController.addToQueueAndPlay(serviceMain.getApplicationContext(), songID);
        // Log.v(TAG, "addToQueueAndPlay end");
    }

    public void playNext() {
        // Log.v(TAG, "playNext start");
        if (mediaController.getCurrentAudioUri() != null) {
            RandomPlaylist randomPlaylist = getCurrentPlaylist();
            if (randomPlaylist != null) {
                randomPlaylist.bad(
                        serviceMain.getApplicationContext(),
                        MediaController.getInstance(getApplicationContext())
                                .getSong(mediaController.getCurrentAudioUri().id),
                        mediaController.getPercentChangeDown());
            }
        }
        mediaController.playNext(serviceMain.getApplicationContext());
        updateAudioUri();
        // Log.v(TAG, "playNext end");
    }

    public void playPrevious() {
        // Log.v(TAG, "playPrevious start");
        mediaController.playPrevious(serviceMain.getApplicationContext());
        updateAudioUri();
        // Log.v(TAG, "playPrevious end");
    }

    public void pauseOrPlay() {
        // Log.v(TAG, "pauseOrPlay start");
        if (mediaController.getCurrentAudioUri() != null) {
            mediaController.pauseOrPlay(serviceMain.getApplicationContext());
        }
        if (isPlaying()) {
            viewModelActivityMain.setIsPlaying(true);
        } else {
            viewModelActivityMain.setIsPlaying(false);
        }
        // Log.v(TAG, "pauseOrPlay end");
    }

    public void seekTo(int progress) {
        // Log.v(TAG, "seekTo start");
        mediaController.seekTo(serviceMain.getApplicationContext(), progress);
        // Log.v(TAG, "seekTo end");
    }

    // endregion playbackControls

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
            mediaController.clearProbabilities(serviceMain.getApplicationContext());
            return true;
        } else if (item.getItemId() == R.id.action_lower_probs) {
            mediaController.lowerProbabilities(serviceMain.getApplicationContext());
        } else if (item.getItemId() == R.id.action_add_to_playlist) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                    new DialogFragmentAddToPlaylist();
            dialogFragmentAddToPlaylist.setArguments(loadBundleForAddToPlaylist(isSong));
            dialogFragmentAddToPlaylist.show(fragmentManager, fragment.getTag());
            return true;
        } else if (item.getItemId() == R.id.action_add_to_queue) {
            if (mediaController.songInProgress() && songToAddToQueue != null) {
                mediaController.addToQueue(songToAddToQueue);

            } else if (playlistToAddToQueue != null) {
                for (Song songs : playlistToAddToQueue.getSongs()) {
                    mediaController.addToQueue(songs.id);
                }
                if (mediaController.songQueueIsEmpty()) {
                    mediaController.setCurrentPlaylist(playlistToAddToQueue);
                }
                if (!mediaController.songInProgress()) {
                    showSongPane();
                }
            }
            if (!mediaController.songInProgress()) {
                mediaController.playNext(serviceMain.getApplicationContext());
            }
        }
        // Log.v(TAG, "onOptionsItemSelected action_unknown end");
        return super.onOptionsItemSelected(item);
    }

    private Bundle loadBundleForAddToPlaylist(boolean isSong) {
        // Log.v(TAG, "loadBundleForAddToPlaylist start");
        Bundle bundle = new Bundle();
        bundle.putBoolean(BUNDLE_KEY_IS_SONG, isSong);
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG,
                MediaController.getInstance(getApplicationContext())
                        .getSong(mediaController.getCurrentAudioUri().id));
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST,
                viewModelUserPickedPlaylist.getUserPickedPlaylist());
        // Log.v(TAG, "loadBundleForAddToPlaylist end");
        return bundle;
    }

    public void saveFile() {
        // Log.v(TAG, "saveFile start");
        SaveFile.saveFile(serviceMain.getApplicationContext());
        // Log.v(TAG, "saveFile end");
    }

    public void setSongPaneArtHeight(int songArtHeight) {
        // Log.v(TAG, "setSongPaneArtHeight start");
        if (serviceMain != null) {
            serviceMain.setSongPaneArtHeight(songArtHeight);
        }
        // Log.v(TAG, "setSongPaneArtHeight end");
    }

    public int getSongPaneArtHeight() {
        // Log.v(TAG, "getSongPaneArtHeight start");
        if (serviceMain != null) {
            // Log.v(TAG, "getSongPaneArtHeight end");
            return serviceMain.getSongPaneArtHeight();
        }
        // Log.v(TAG, "getSongPaneArtHeight default end");
        return -1;
    }

    public void setSongPaneArtWidth(int songArtWidth) {
        // Log.v(TAG, "setSongPaneArtWidth start");
        if (serviceMain != null) {
            serviceMain.setSongPaneArtWidth(songArtWidth);
        }
        // Log.v(TAG, "setSongPaneArtWidth end");
    }

    public int getSongPaneArtWidth() {
        // Log.v(TAG, "getSongPaneArtWidth start");
        if (serviceMain != null) {
            // Log.v(TAG, "getSongPaneArtWidth end");
            return serviceMain.getSongPaneArtWidth();
        }
        // Log.v(TAG, "getSongPaneArtWidth default end");
        return -1;
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

    public boolean serviceConnected() {
        return (serviceMain != null);
    }

    public boolean songInProgress() {
        // Log.v(TAG, "songInProgress start");
        // Log.v(TAG, "songInProgress end");
        return (mediaController != null) && mediaController.songInProgress();
    }

    public boolean isPlaying() {
        // Log.v(TAG, "isPlaying start");
        // Log.v(TAG, "isPlaying end");
        return mediaController.isPlaying();
    }

    public void setCurrentPlaylistToMaster() {
        // Log.v(TAG, "setCurrentPlaylistToMaster start");
        mediaController.setCurrentPlaylistToMaster();
        // Log.v(TAG, "setCurrentPlaylistToMaster end");
    }

    public void setCurrentPlaylist(RandomPlaylist userPickedPlaylist) {
        // Log.v(TAG, "setCurrentPlaylist start");
        mediaController.setCurrentPlaylist(userPickedPlaylist);
        // Log.v(TAG, "setCurrentPlaylist end");
    }

    public AudioUri getCurrentAudioUri() {
        // Log.v(TAG, "getCurrentSong start");
        // Log.v(TAG, "getCurrentSong end");
        return mediaController.getCurrentAudioUri();
    }

    public RandomPlaylist getCurrentPlaylist() {
        // Log.v(TAG, "getCurrentPlaylist start");
        // Log.v(TAG, "getCurrentPlaylist end");
        return mediaController.getCurrentPlaylist();
    }

    public boolean shuffling() {
        // Log.v(TAG, "shuffling start");
        // Log.v(TAG, "shuffling end");
        return mediaController.shuffling();
    }

    public void shuffling(boolean shuffling) {
        // Log.v(TAG, "set shuffling start");
        mediaController.shuffling(shuffling);
        // Log.v(TAG, "set shuffling end");
    }

    public boolean loopingOne() {
        // Log.v(TAG, "loopingOne start");
        // Log.v(TAG, "loopingOne end");
        return mediaController.loopingOne();
    }

    public void loopingOne(boolean loopingOne) {
        // Log.v(TAG, "set loopingOne start");
        mediaController.loopingOne(loopingOne);
        // Log.v(TAG, "set loopingOne end");
    }

    public boolean looping() {
        // Log.v(TAG, "looping start");
        // Log.v(TAG, "looping end");
        return mediaController.looping();
    }

    public void looping(boolean looping) {
        // Log.v(TAG, "set looping start");
        mediaController.looping(looping);
        // Log.v(TAG, "set looping end");
    }

    public void clearSongQueue() {
        mediaController.clearSongQueue();
    }

    // region fragmentLoading

    public void fragmentLoadingStarted() {
        startAndBindServiceMain();
    }

    public Song getCurrentSong() {
        if (getCurrentAudioUri() != null) {
            return MediaController.getInstance(getApplicationContext())
                    .getSong(getCurrentAudioUri().id);
        }
        return null;
    }

    public void updateAudioUri() {
        viewModelActivityMain.setCurrentSong(mediaController.getCurrentAudioUri());
    }

    public Uri getCurrentUri() {
        return mediaController.getCurrentUri();
    }

    public double getMaxPercent() {
        return mediaController.getMaxPercent();
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        mediaController.addPlaylist(randomPlaylist);
    }

    public List<RandomPlaylist> getPlaylists() {
        return mediaController.getPlaylists();
    }

    public Song getSong(long songID) {
        return mediaController.getSong(songID);
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        mediaController.removePlaylist(randomPlaylist);
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        mediaController.addPlaylist(position, randomPlaylist);
    }

    public double getPercentChangeUp() {
        return mediaController.getPercentChangeUp();
    }

    public double getPercentChangeDown() {
        return mediaController.getPercentChangeDown();
    }

    public void setMaxPercent(double maxPercent) {
        mediaController.setMaxPercent(maxPercent);
    }

    public void setPercentChangeUp(double percentChangeUp) {
        mediaController.setPercentChangeUp(percentChangeUp);
    }

    public void setPercentChangeDown(double percentChangeDown) {
        mediaController.setPercentChangeDown(percentChangeDown);
    }

    public RandomPlaylist getMasterPlaylist() {
        return mediaController.getMasterPlaylist();
    }

    public RandomPlaylist getPlaylist(String playlistName) {
        return mediaController.getPlaylist(playlistName);
    }

    // endregion fragmentLoading

}