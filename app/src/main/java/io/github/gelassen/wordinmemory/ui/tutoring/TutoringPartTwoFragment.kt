package io.github.gelassen.wordinmemory.ui.tutoring

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.providers.DashboardProvider
import io.github.gelassen.wordinmemory.storage.AppQuickStorage
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardAdapter
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment

class TutoringPartTwoFragment : BaseTutoringFragment() {

    companion object {

        fun newInstance(): Fragment {
            return TutoringPartTwoFragment()
        }
    }

    private var counter = 0

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
        binding.completeText.text = getString(R.string.complete_daily_practice).plus(" 2 / 2")
    }

    override fun onCompleteDailyPractice(dataset: MutableList<SubjectToStudy>) {
        lifecycleScope.launchWhenCreated {
            viewModel.completePartTwoDailyPractice(requireActivity(), dataset)
        }
        finishWork()
    }

    override fun runOnStart() {

        lifecycleScope.launchWhenStarted {
            viewModel.showPartTwoDailyPractice()
        }

        listenOnModelUpdates() { dataset ->
            if (viewModel.shallSkipTutoringScreen()
                || viewModel.areNotEnoughWordsForPractice()) {
                finishWork()
            }
        }
    }

}