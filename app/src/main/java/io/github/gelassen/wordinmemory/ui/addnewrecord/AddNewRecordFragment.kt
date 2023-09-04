package io.github.gelassen.wordinmemory.ui.addnewrecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.github.gelassen.wordinmemory.AppApplication
import io.github.gelassen.wordinmemory.databinding.FragmentDashboardBinding
import io.github.gelassen.wordinmemory.di.ViewModelFactory
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardViewModel
import javax.inject.Inject

@Deprecated(message = "we would not need a separate fragment if we would put all items immediately into db")
class AddNewRecordFragment: Fragment() {

    companion object {

        const val TAG = "AddNewRecordFragment"

        fun newInstance() : Fragment {
            return AddNewRecordFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    lateinit var viewModel: NewRecordViewModel

    protected lateinit var binding: FragmentDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (requireActivity().application as AppApplication).getComponent().inject(this)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(NewRecordViewModel::class.java)
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        binding.isTutoringMode = false
        return binding.root
    }

}