package com.fourthFinger.pinkyPlayer.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentLoadingBinding


class FragmentLoading : Fragment() {

    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain
            by activityViewModels<ViewModelActivityMain> {
                ViewModelActivityMain.Factory
            }
    private val viewModelFragmentLoading
            by activityViewModels<ViewModelFragmentLoading> {
                ViewModelFragmentLoading.Factory
            }

    private lateinit var audioPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    // TODO stop permission warning after user comes back from settings screen

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setUpObservers()
        registerPermissions()
        _binding = FragmentLoadingBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    private fun setUpObservers() {
        viewModelFragmentLoading.loadingProgress.observe(viewLifecycleOwner) { loadingProgress: Int ->
            val progressBar: ProgressBar = binding.progressBarLoading
            progressBar.post {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(loadingProgress, true)
                } else {
                    progressBar.progress = loadingProgress
                }
            }
        }
        viewModelFragmentLoading.loadingText.observe(viewLifecycleOwner) { loadingText: String? ->
            val textView: TextView = binding.textViewLoading
            textView.post { textView.text = loadingText }
        }
        viewModelFragmentLoading.showLoadingBar.observe(viewLifecycleOwner) { showLoadingBar: Boolean ->
            if (showLoadingBar) {
                binding.progressBarLoading.visibility = VISIBLE
            }
        }
    }

    private fun registerPermissions() {
        registerAudioPermission()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun registerAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerPermission(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            registerPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Requests a permission.
     *
     * @param permission The [Manifest.permission] to request.
     */
    private fun registerPermission(permission: String) {
        val isAudioPermission = isAudioPermission(permission)
        if (isAudioPermission) {
            audioPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    audioPermissionGranted()
                } else {
                    showAudioMediaPermissionRationale()
                }
            }
        } else {
            notificationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    showNotificationPermissionRationale()
                }
            }
        }

    }

    private fun isAudioPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission == Manifest.permission.READ_MEDIA_AUDIO
        } else {
            permission == Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun askForPermission(permission: String) {
        val isAudioPermission = isAudioPermission(permission)
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                if (isAudioPermission) {
                    audioPermissionGranted()
                }
            }

            shouldShowRequestPermissionRationale(permission) -> {
                if (isAudioPermission) {
                    showAudioMediaPermissionRationale()
                } else {
                    showNotificationPermissionRationale()
                }
            }

            else -> {
                if(isAudioPermission) {
                    audioPermissionLauncher.launch(permission)
                } else {
                    notificationPermissionLauncher.launch(permission)
                }
            }
        }
    }

    private fun askForPermissionsAndLoadMusic() {
        askForAudioPermissionAndLoadMusic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askForPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun askForAudioPermissionAndLoadMusic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askForPermission(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun showAudioMediaPermissionRationale() {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(
                getString(R.string.permission_audio_needed)
            )
            .setTitle(getString(R.string.permission_title))

        dialog.setPositiveButton(R.string.go_to_settings) { clicked_dialog, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts(
                "package",
                requireActivity().packageName,
                null
            )
            intent.data = uri
            clicked_dialog.dismiss()
            startActivity(intent)
        }
            .setNegativeButton(R.string.cancel) { _, _ ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_audio_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
            .create()
        dialog.show()
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setMessage(
                getString(R.string.permission_notification_needed)
            )
            .setTitle(getString(R.string.permission_title))
            .setPositiveButton(R.string.go_to_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts(
                    "package",
                    requireActivity().packageName,
                    null
                )
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_notification_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
            .create()
            .show()
    }

    private fun audioPermissionGranted() {
        viewModelFragmentLoading.permissionGranted(requireActivity())
    }

    override fun onStart() {
        super.onStart()
        askForPermissionsAndLoadMusic()
    }

    override fun onResume() {
        super.onResume()
        updateMainContent()
    }

    private fun updateMainContent() {
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.loading))
        viewModelActivityMain.showFab(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}