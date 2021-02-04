package com.example.waveplayer.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentLoadingBinding;
import com.example.waveplayer.media_controller.ViewModelFragmentLoading;

public class FragmentLoading extends Fragment {

    private static final short REQUEST_CODE_PERMISSION = 245;

    private FragmentLoadingBinding binding;

    private ViewModelActivityMain viewModelActivityMain;
    private ViewModelFragmentLoading viewModel;

    private Handler  handler = HandlerCompat.createAsync(Looper.getMainLooper());

    private Runnable runnableAskForPermission = this::askForPermissionAndCreateMediaController;

    private Observer<Double> observerLoadingProgress;
    private Observer<String> observerLoadingText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setUpViewModels();
        setUpObservers();
        binding = FragmentLoadingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void setUpViewModels() {
        viewModel = new ViewModelProvider(this).get(ViewModelFragmentLoading.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
    }

    private void setUpObservers() {
        observerLoadingProgress = loadingProgress -> {
            final ProgressBar progressBar = binding.progressBarLoading;
            progressBar.post(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress((int) Math.round(loadingProgress * 100), true);
                } else {
                    progressBar.setProgress((int) Math.round(loadingProgress * 100));
                }
            });
        };
        observerLoadingText = loadingText -> {
            final TextView textView = binding.textViewLoading;
            textView.post(() -> textView.setText(loadingText));
        };
        viewModel.getLoadingProgress().observe(getViewLifecycleOwner(), observerLoadingProgress);
        viewModel.getLoadingText().observe(getViewLifecycleOwner(), observerLoadingText);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMainContent();
        askForPermissionAndCreateMediaController();
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        activityMain.fragmentLoadingStarted();
    }

    void askForPermissionAndCreateMediaController() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = requireActivity().getApplicationContext();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION);
            } else {
                permissionGranted();
            }
        } else {
            permissionGranted();
        }
    }


    private void updateMainContent() {
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.loading));
        viewModelActivityMain.showFab(false);
    }

    private void permissionGranted() {
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        activityMain.permissionGranted();
    }

    @SuppressLint("ShowToast")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Context context = requireActivity().getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == REQUEST_CODE_PERMISSION && grantResults.length > 1 &&
                    grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast toast;
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    toast = Toast.makeText(context, R.string.permission_read_needed, Toast.LENGTH_LONG);
                } else {
                    toast = Toast.makeText(context, R.string.permission_write_needed, Toast.LENGTH_LONG);
                }
                toast.show();
                handler.postDelayed(runnableAskForPermission, 1000);
            } else {
                permissionGranted();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        runnableAskForPermission = null;
        handler = null;
        viewModel.getLoadingProgress().removeObservers(getViewLifecycleOwner());
        viewModel.getLoadingText().removeObservers(getViewLifecycleOwner());
        observerLoadingProgress = null;
        observerLoadingText = null;
        viewModel = null;
        viewModelActivityMain = null;
        binding = null;
    }

}