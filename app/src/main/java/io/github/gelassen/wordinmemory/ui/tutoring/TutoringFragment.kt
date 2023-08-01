package io.github.gelassen.wordinmemory.ui.tutoring

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardAdapter
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment

class TutoringFragment : DashboardFragment() {

    companion object {
        fun newInstance(): Fragment {
            return TutoringFragment()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        binding.isTutoringMode = true
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.completeDailyPractice.apply {
            setOnClickListener {
                Log.d(App.TAG, "Click on complete daily practice")
                val dataset = (binding.dashboardList.adapter as DashboardAdapter).getDataset()
                lifecycleScope.launchWhenCreated {
                    viewModel.completeDailyPractice(dataset)
                }
                showMainScreen()
            }
        }
    }

    override fun runOnStart() {
        lifecycleScope.launchWhenStarted {
            viewModel.showDailyPractice()
        }
        listenOnModelUpdates() { dataset ->
            Log.d(App.TAG, "Lambda code block has been executed")
//            if (dataset.isEmpty()) {
//                showMainScreen()
//            }
        }
    }

    private fun showMainScreen() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, DashboardFragment.newInstance())
            .commit()
    }

}