package com.example.waveplayer.fragments

import android.Manifest
import android.content.Context
import android.os.Handler
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.waveplayer.databinding.FragmentLoadingBinding

class FragmentLoading : Fragment() {
    private var binding: FragmentLoadingBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var viewModel: ViewModelFragmentLoading? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var handler: Handler? = HandlerCompat.createAsync(Looper.getMainLooper())
    private var runnableAskForPermission: Runnable? = Runnable { askForPermissionAndCreateMediaController() }
    private var observerLoadingProgress: Observer<Double?>? = null
    private var observerLoadingText: Observer<String?>? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setUpViewModels()
        setUpObservers()
        binding = FragmentLoadingBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    private fun setUpViewModels() {
        viewModel = ViewModelProvider(this).get<ViewModelFragmentLoading?>(ViewModelFragmentLoading::class.java)
        viewModelActivityMain = ViewModelProvider(requireActivity()).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
    }

    private fun setUpObservers() {
        observerLoadingProgress = Observer { loadingProgress: Double? ->
            val progressBar: ProgressBar = binding.progressBarLoading
            progressBar.post(Runnable {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(Math.round(loadingProgress * 100) as Int, true)
                } else {
                    progressBar.setProgress(Math.round(loadingProgress * 100) as Int)
                }
            })
        }
        observerLoadingText = Observer { loadingText: String? ->
            val textView: TextView = binding.textViewLoading
            textView.post(Runnable { textView.setText(loadingText) })
        }
        viewModel.getLoadingProgress().observe(viewLifecycleOwner, observerLoadingProgress)
        viewModel.getLoadingText().observe(viewLifecycleOwner, observerLoadingText)
    }

    override fun onResume() {
        super.onResume()
        updateMainContent()
        setUpBroadcastReceiver()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.fragmentLoadingStarted()
    }

    private fun setUpBroadcastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected))
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action: String = intent.getAction()
                if (action != null) {
                    if (action == resources.getString(
                                    R.string.broadcast_receiver_action_service_connected)) {
                        // TODO is this needed if onResume has it?
                        askForPermissionAndCreateMediaController()
                    }
                }
            }
        }
        activityMain.registerReceiver(broadcastReceiver, filterComplete)
    }

    fun askForPermissionAndCreateMediaController() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val context = requireActivity().applicationContext
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf<String?>(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_PERMISSION.toInt())
            } else {
                permissionGranted()
            }
        } else {
            permissionGranted()
        }
    }

    private fun updateMainContent() {
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.loading))
        viewModelActivityMain.showFab(false)
    }

    private fun permissionGranted() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.permissionGranted()
    }

    @SuppressLint("ShowToast")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val context = requireActivity().applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == REQUEST_CODE_PERMISSION.toInt() && grantResults.size > 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                val toast: Toast
                toast = if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, R.string.permission_read_needed, Toast.LENGTH_LONG)
                } else {
                    Toast.makeText(context, R.string.permission_write_needed, Toast.LENGTH_LONG)
                }
                toast.show()
                handler.postDelayed(runnableAskForPermission, 1000)
            } else {
                permissionGranted()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
        runnableAskForPermission = null
        handler = null
        /*
        viewModel.getLoadingProgress().removeObservers(getViewLifecycleOwner());
        viewModel.getLoadingText().removeObservers(getViewLifecycleOwner());

         */observerLoadingProgress = null
        observerLoadingText = null
        viewModel = null
        viewModelActivityMain = null
        binding = null
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION: Short = 245
    }
}