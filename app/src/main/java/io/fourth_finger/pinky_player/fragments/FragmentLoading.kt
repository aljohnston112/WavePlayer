package io.fourth_finger.pinky_player.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.activity_main.MenuActionIndex
import io.fourth_finger.pinky_player.activity_main.ViewModelActivityMain
import io.fourth_finger.pinky_player.databinding.FragmentLoadingBinding


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

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // TODO stop permission warning after user comes back from settings screen

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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

    private fun getManifestAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Requests permissions.
     */
    private fun registerPermissions() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.entries.all { it.value == true }){
                audioPermissionGranted()
            } else {
                for (permission in permissions){
                    if(!permission.value){
                        if(isAudioPermission(permission.key)){
                            showAudioMediaPermissionRationale()
                        } else {
                            showNotificationPermissionRationale()
                        }
                    }
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

    private fun askForPermissionAndLoadMusic() {
        val permissions = mutableListOf<String>()
        val audioPermission = getManifestAudioPermission()
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                audioPermission
            ) == PackageManager.PERMISSION_GRANTED -> {
                audioPermissionGranted()
            }

            shouldShowRequestPermissionRationale(audioPermission) -> {
                showAudioMediaPermissionRationale()
            }

            else -> {
                permissions.add(audioPermission)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Nothing needs to be done
                }

                shouldShowRequestPermissionRationale(audioPermission) -> {
                    showNotificationPermissionRationale()
                }

                else -> {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(
                permissions.toTypedArray()
            )
        }
    }

    private fun showAudioMediaPermissionRationale() {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(
                getString(R.string.permission_audio_needed)
            )
            .setTitle(getString(R.string.permission_title))

        dialog.setPositiveButton(R.string.go_to_settings) { clickedDialog, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts(
                "package",
                requireActivity().packageName,
                null
            )
            intent.data = uri
            clickedDialog.dismiss()
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

    private fun setUpMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater,
            ) {
                menuInflater.inflate(
                    R.menu.menu_toolbar,
                    menu
                )
                for (menuActionIndex in MenuActionIndex.entries) {
                    val menuItem = menu.getItem(menuActionIndex.ordinal)
                    menuItem.isVisible = false
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpMenu()
    }

    override fun onStart() {
        super.onStart()
        askForPermissionAndLoadMusic()
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