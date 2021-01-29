package com.example.waveplayer.fragments.fragment_loading;

import android.Manifest;
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
import com.example.waveplayer.databinding.FragmentLoadingBinding;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.service_main.ServiceMain;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentLoading extends Fragment {

    private static final int REQUEST_CODE_PERMISSION = 245083964;

    private FragmentLoadingBinding mBinding;

    private Handler mHandler = HandlerCompat.createAsync(Looper.myLooper());

    private Runnable mRunnableAskForPermission = new Runnable() {
        @Override
        public void run() {
            askForPermissionAndFillMediaController();
        }
    };

    private Observer<Double> mObserverLoadingProgress = new Observer<Double>() {
        @Override
        public void onChanged(final Double loadingProgress) {
            final ProgressBar progressBar = mBinding.progressBarLoading;
            progressBar.post(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        progressBar.setProgress((int) Math.round(loadingProgress * 100), true);
                    } else {
                        progressBar.setProgress((int) Math.round(loadingProgress * 100));
                    }
                }
            });
        }
    };

    private Observer<String> mObserverLoadingText = new Observer<String>() {
        @Override
        public void onChanged(final String loadingText) {
            final TextView textView = mBinding.textViewLoading;
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(loadingText);
                        }
                    });
                }
            });
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentLoadingBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpViewModel();
        updateMainContent();
        askForPermissionAndFillMediaController();
    }

    private void setUpViewModel() {
        ViewModelFragmentLoading model = new ViewModelProvider(this).get(ViewModelFragmentLoading.class);
        model.getLoadingProgress().observe(getViewLifecycleOwner(), mObserverLoadingProgress);
        model.getLoadingText().observe(getViewLifecycleOwner(), mObserverLoadingText);
    }

    private void updateMainContent() {
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.loading));
        updateFAB();
    }

    private void updateFAB() {
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        activityMain.showFab(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        activityMain.fragmentLoadingStarted();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
        mRunnableAskForPermission = null;
        mHandler = null;
        mObserverLoadingProgress = null;
        mObserverLoadingText = null;
    }

    void askForPermissionAndFillMediaController() {
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
                mHandler.postDelayed(mRunnableAskForPermission, 1000);
            } else {
                permissionGranted();
            }
        }
    }

    private void permissionGranted() {
        ServiceMain.executorServicePool.execute(new Runnable() {
            @Override
            public void run() {
                MediaData.getInstance().loadData((ActivityMain) requireActivity());
            }
        });
    }

}