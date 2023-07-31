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
import android.provider.DocumentsContract
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

    private var restoreRequestLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            Log.d(App.TAG, "restoreRequestLauncher callback is triggered ${result}")
            when(result.resultCode) {
                RESULT_OK -> {
                    Log.d(App.TAG, "Receive a result on the request for a document ${result.data!!.data!!}")
                    val uri = result.data!!.data!!
                    viewModel.restoreVocabulary(uri)
                }
                else -> { Log.d(App.TAG, "Haven't received a document on the request ${result.resultCode}") }
            }
        }

    private var createDocBackupRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        Log.d(App.TAG, "createDocBackupRequestLauncher callback is triggered $result")
        when(result.resultCode) {
            RESULT_OK -> {
                Log.d(App.TAG, "Receive result on CREATE_DOCUMENT request ${result.data!!.data!!}")
                val uri = result.data!!.data!!
                viewModel.backupVocabulary(uri)
            }
            else -> { Log.d(App.TAG, "Haven't received a document uri on the CREATE_DOCUMENT request ${result.resultCode}") }
        }
    }

    private fun requestCreateDocStorageAccessFramework() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, getString(R.string.backup_file_json))
        }
        createDocBackupRequestLauncher.launch(intent)
    }

    private fun requestRestorePermissions() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        restoreRequestLauncher.launch(intent)
    }

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
                requestCreateDocStorageAccessFramework()
                return true
            }
            R.id.restoreVocabulary -> {
                requestRestorePermissions()
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
                (binding.dashboardList.adapter as DashboardAdapter).updateData(it.data.asReversed())
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