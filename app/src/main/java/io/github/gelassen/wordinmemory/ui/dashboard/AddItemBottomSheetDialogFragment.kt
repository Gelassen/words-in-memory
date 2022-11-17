package io.github.gelassen.wordinmemory.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.AppApplication
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.databinding.AddItemFragmentBinding
import io.github.gelassen.wordinmemory.di.ViewModelFactory
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import javax.inject.Inject

class AddItemBottomSheetDialogFragment: BottomSheetDialogFragment() {

    private lateinit var binding: AddItemFragmentBinding

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle)
        (requireActivity().application as AppApplication).getComponent().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // keep an eye on owner parameter, it should be the same scope for view model which is shared among component
        viewModel = ViewModelProvider(requireParentFragment(), viewModelFactory).get(DashboardViewModel::class.java)
        binding = AddItemFragmentBinding.inflate(inflater, container, false)
        binding.model = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!requireArguments().isEmpty
            && requireArguments().containsKey(EXTRA_DATA)) {
            val data = requireArguments().getParcelable<SubjectToStudy>(EXTRA_DATA)
            viewModel.wordToTranslate.set(data?.toTranslate)
            viewModel.translation.set(data?.translation)
        }

        binding.save.setOnClickListener {
            Log.d(App.TAG, "${viewModel.wordToTranslate.get()}")
            if (requireArguments().isEmpty) {
                viewModel.addItem()
            } else {
                viewModel.updateItem(requireArguments().getParcelable<SubjectToStudy>(EXTRA_DATA)!!)
            }
            dismiss()
        }
    }

    fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    companion object {

        const val TAG = "AddItemBottomSheetDialogFragment"
        const val EXTRA_DATA = "EXTRA_DATA"

        fun newInstance(args: Bundle): AddItemBottomSheetDialogFragment {
            val fragment = AddItemBottomSheetDialogFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subject: SubjectToStudy): AddItemBottomSheetDialogFragment {
            val fragment = AddItemBottomSheetDialogFragment()
            val data = Bundle()
            data.putParcelable(EXTRA_DATA, subject)
            fragment.arguments = data
            return fragment
        }
    }
}