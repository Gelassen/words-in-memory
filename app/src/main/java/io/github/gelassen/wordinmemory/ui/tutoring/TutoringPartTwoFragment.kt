package io.github.gelassen.wordinmemory.ui.tutoring

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardAdapter

class TutoringPartTwoFragment : BaseTutoringFragment() {

    companion object {

        fun newInstance(): Fragment {
            return TutoringPartTwoFragment()
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
        (binding.dashboardList.adapter as DashboardAdapter).turnOnTutoring(true)
        binding.completeText.text = getString(R.string.complete_daily_practice).plus(" 2 / 2")
    }

    override fun onLongPress(selectedSubject: SubjectToStudy) {
        /* no ops */
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
            if (viewModel.shallSkipPartTwoTutoringScreen()
                || viewModel.areNotEnoughWordsForPractice()) {
                finishWork()
            }
        }
    }

}