package io.github.gelassen.wordinmemory.ui.dashboard

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.AppApplication
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.databinding.FragmentDashboardBinding
import io.github.gelassen.wordinmemory.di.ViewModelFactory
import io.github.gelassen.wordinmemory.dialogs.AddItemDialogProxy
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.utils.ConfigParams
import javax.inject.Inject

class DashboardFragment: Fragment(),
    DashboardAdapter.ClickListener {

    companion object {

        const val TAG = "DashboardFragment"

        const val PERMISSION_REQUEST_CODE = 1001

        fun newInstance() : Fragment {
            return DashboardFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    lateinit var viewModel: DashboardViewModel

    private lateinit var binding: FragmentDashboardBinding

    // FIXME on some old versions of Android (at least on Android 7 and below API 30) files in
    // Downloads folder are not shown open via SAF and using default Files catalog explorer
    // (using non-default one works without any issues). Possible workaround don't use SAF
    // on this platforms versions (use old-school way to write\read from sdcard) or save backup
    // in the sdcard root (have to verify it doesn't have similar issue)

    // FIXME use document intent https://stackoverflow.com/questions/71728153/i-cant-access-to-a-file-even-with-read-permission
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                /*Log.d(App.TAG, "READ_EXTERNAL_STORAGE Permission has been granted for restoreVocabulary()")*/
                /*viewModel.restoreVocabulary()*/
                Log.d(App.TAG, "WRITE_EXTERNAL_STORAGE Permission has been granted for backupVocabulary()")
                viewModel.backupVocabulary()
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
//                Log.d(App.TAG, "READ_EXTERNAL_STORAGE Permission has been declined for restoreVocabulary()")
                Log.d(App.TAG, "WRITE_EXTERNAL_STORAGE Permission has been declined for backupVocabulary()")
                Snackbar.make(
                    binding.noContentPlaceholder,
                    "You have to give WordsInMemory app this permission to backup your vocabulary",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

    var backupRequestLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            Log.d(App.TAG, "backupRequestLauncher callback is triggered ${result}")
            when(result.resultCode) {
                RESULT_OK -> {
                    Log.d(App.TAG, "Receive a result on the request for a document ${result.data!!.data!!}")
                    val uri = result.data!!.data!!
                    viewModel.restoreVocabulary(uri)
                }
                else -> { Log.d(App.TAG, "Haven't received a document on the request ${result.resultCode}") }
            }
        }

    private fun requestBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        backupRequestLauncher.launch(intent)
    }

    /**
     * This is required at least on Android 7 and likely below
     * */
    private fun requestPermissionsIfNecessary(operationUnderPermission: () -> Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            // at least until Android Nougat permissions are required
            val selfPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (selfPermission == PackageManager.PERMISSION_GRANTED) {
                operationUnderPermission()
            } else {
                requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        } else {
            operationUnderPermission()
        }


/*                This code would not work: https://stackoverflow.com/a/33080682/3649629
                use ActivityResultLauncher instead

                ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
 */
    }

/*    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE  -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    viewModel.restoreVocabulary()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Snackbar.make(
                        binding.noContentPlaceholder,
                        "You have to give WordsInMemory app this permission to restore your vocabulary",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
                Log.d(App.TAG, "Got unsupported permission request code $requestCode")
            }
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (requireActivity().application as AppApplication).getComponent().inject(this)

        viewModel = ViewModelProvider(this, viewModelFactory).get(DashboardViewModel::class.java)
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        val dr = ColorDrawable(getApiSupportColor())
        (requireActivity() as AppCompatActivity).supportActionBar!!.setBackgroundDrawable(dr)

        binding.dashboardList.layoutManager = StaggeredGridLayoutManager(
            ConfigParams().getAmountOfColumnsForDashboard(requireActivity()),
            StaggeredGridLayoutManager.VERTICAL
        )
        binding.dashboardList.adapter = DashboardAdapter(this)
        binding.dashboardAddNewWord.apply {
            setOnClickListener {
                AddItemDialogProxy()
                    .show(null, childFragmentManager, requireActivity())
            }
        }

        runOnStart()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.showAll -> {
                viewModel.showAll()
                return true
            }
            R.id.showNonCompletedOnly -> {
                viewModel.showNonCompletedOnly()
                return true
            }
            R.id.backupVocabulary -> {
                requestPermissionsIfNecessary { viewModel.backupVocabulary() }
                return true
            }
            R.id.restoreVocabulary -> {
//                requestPermission()
//                viewModel.restoreVocabulary()
//                requestPermissions { viewModel.restoreVocabulary() }
                requestBackup()
                return true
            }
            R.id.privacyPolicy -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(getString(R.string.privacy_policy_endpoint))
                startActivity(intent)
                return true
            }
            else -> { super.onOptionsItemSelected(item) }
        }
    }

    override fun onClick(data: SubjectToStudy) {
        // no op
    }

    override fun onNonComplete(selectedSubject: SubjectToStudy) {
        lifecycleScope.launchWhenCreated {
            viewModel.updateItem(selectedSubject, isComplete = false)
        }
    }

    override fun onComplete(selectedSubject: SubjectToStudy) {
        lifecycleScope.launchWhenCreated {
            viewModel.updateItem(selectedSubject, isComplete = true)
        }
    }

    override fun onLongPress(selectedSubject: SubjectToStudy) {
        AddItemDialogProxy()
            .show(selectedSubject, childFragmentManager, requireActivity())
    }

    @Suppress("DEPRECATION")
    protected fun getApiSupportColor(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            resources.getColor(R.color.colorActionBar, requireActivity().theme)
        else
            resources.getColor(R.color.colorActionBar)
    }

    private fun runOnStart() {
        lifecycleScope.launchWhenStarted {
            viewModel.showNonCompletedOnly()
        }
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { it ->
                (binding.dashboardList.adapter as DashboardAdapter).updateData(it.data)
                binding.noContentPlaceholder.visibility = if (it.data.isEmpty()) { View.VISIBLE } else { View.GONE }

                if (it.errors.isNotEmpty()) {
                    var error = it.errors.first()
                    Snackbar.make(
                        binding.noContentPlaceholder,
                        error,
                        Snackbar.LENGTH_SHORT
                    )
                        .addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                viewModel.removeError(error)
                            }
                        })
                        .show()
                }
            }
        }

    }
}