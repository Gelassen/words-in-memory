package io.github.gelassen.wordinmemory.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.gelassen.wordinmemory.AppApplication
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.databinding.AddItemFragmentBinding
import io.github.gelassen.wordinmemory.di.ViewModelFactory
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.ui.FragmentUtils
import io.github.gelassen.wordinmemory.ui.addnewrecord.NewRecordViewModel
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardViewModel
import javax.inject.Inject

class AddItemDialogFragment: DialogFragment() {

    companion object {

        const val TAG = "AddItemDialogFragment"
        const val EXTRA_DATA = "EXTRA_DATA"

        fun newInstance(args: Bundle): AddItemDialogFragment {
            val fragment = AddItemDialogFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subject: SubjectToStudy): AddItemDialogFragment {
            val fragment = AddItemDialogFragment()
            val data = Bundle()
            data.putParcelable(EXTRA_DATA, subject)
            fragment.arguments = data
            return fragment
        }
    }

    private lateinit var binding: AddItemFragmentBinding

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    lateinit var viewModel: NewRecordViewModel

    private val fragmentUtils: FragmentUtils = FragmentUtils()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        (requireActivity().application as AppApplication).getComponent().inject(this)
        // keep an eye on owner parameter, it should be the same scope for view model which is shared among component
        viewModel = ViewModelProvider(requireParentFragment(), viewModelFactory)
            .get(NewRecordViewModel::class.java)
        binding = AddItemFragmentBinding.inflate(LayoutInflater.from(context))
        binding.withBackend = isWithExperimentalFeatureSupport()
        binding.model = viewModel
        binding.title.visibility = View.VISIBLE
        binding.isOnEdit = false
        binding.save.setOnClickListener {
            if (requireArguments().isEmpty) {
                if (isWithExperimentalFeatureSupport()) {
                    fragmentUtils.showProgressIndicator(this)
                    viewModel.start()
                } else {
                    viewModel.addItem()
                }
            } else {
                viewModel.updateItem(requireArguments().getParcelable<SubjectToStudy>(
                    AddItemBottomSheetDialogFragment.EXTRA_DATA
                )!!)
            }
            dismiss()
        }
        preSetIfNecessary()
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun preSetIfNecessary() {
        if (arguments?.containsKey(EXTRA_DATA) == true) {
            val data = requireArguments().getParcelable<SubjectToStudy>(EXTRA_DATA)!!
            viewModel.wordToTranslate.set(data.toTranslate)
            viewModel.translation.set(data.translation)
            binding.isOnEdit = true
        }
    }

    private fun isWithExperimentalFeatureSupport(): Boolean {
        return resources.getBoolean(R.bool.with_backend)
    }

}