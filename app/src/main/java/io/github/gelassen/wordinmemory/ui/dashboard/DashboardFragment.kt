package io.github.gelassen.wordinmemory.ui.dashboard

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.github.gelassen.wordinmemory.AppApplication
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.databinding.FragmentDashboardBinding
import io.github.gelassen.wordinmemory.di.ViewModelFactory
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import javax.inject.Inject

class DashboardFragment: Fragment(),
    DashboardAdapter.ClickListener {

    companion object {

        const val TAG = "DashboardFragment"

        fun newInstance() : Fragment {
            return DashboardFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    lateinit var viewModel: DashboardViewModel

    private lateinit var binding: FragmentDashboardBinding

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

        binding.dashboardList.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.dashboardList.adapter = DashboardAdapter(this)
        binding.dashboardAddNewWord.apply {
            setOnClickListener {
                childFragmentManager.let {
                    AddItemBottomSheetDialogFragment.newInstance(Bundle.EMPTY)
                        .show(it, AddItemBottomSheetDialogFragment.TAG)
                }
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
        childFragmentManager.let {
            AddItemBottomSheetDialogFragment.newInstance(selectedSubject)
                .show(it, AddItemBottomSheetDialogFragment.TAG)
        }
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
            }
        }
    }
}