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

class TutoringFragment : DashboardFragment() {

    companion object {

        const val MAX_COUNTER = 2

        const val REQUIRED_AMOUNT_OF_ITEMS_FOR_TUTORING = 10

        fun newInstance(): Fragment {
            return TutoringFragment()
        }
    }

    private val quickStorage: AppQuickStorage = AppQuickStorage()
    private val provider: DashboardProvider = DashboardProvider()
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
        binding.completeDailyPractice.apply {
            setOnClickListener {
                Log.d(App.TAG, "Click on complete daily practice")
                val dataset = (binding.dashboardList.adapter as DashboardAdapter).getDataset()
                lifecycleScope.launchWhenCreated {
                    viewModel.completeDailyPractice(requireActivity(), dataset)
                }
                showMainScreen()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /* super.onCreateOptionsMenu(menu, inflater) */
        /* disable menu for this screen */
    }

    override fun runOnStart() {

        if (provider.isTimeToShowDailyTraining(
                lastShownTime = quickStorage.getLastTrainedTime(requireActivity()),
                currentTime = System.currentTimeMillis())) {
            lifecycleScope.launchWhenStarted {
                viewModel.showDailyPractice()
            }

            listenOnModelUpdates() { dataset ->
                if (shallSkipTutoringScreen(dataset)
                    || areNotEnoughWordsForPractice(dataset)) {
                    showMainScreen()
                }
            }
        } else {
            showMainScreen()
        }
    }

    private fun showMainScreen() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, DashboardFragment.newInstance())
            .commit()
    }

    private fun shallSkipTutoringScreen(dataset: List<SubjectToStudy>): Boolean {
        // we have to add counter, because at first we always receive model's default state
        return ++counter >= MAX_COUNTER && dataset.isEmpty()
    }
    private fun areNotEnoughWordsForPractice(dataset: List<SubjectToStudy>): Boolean {
        return dataset.isNotEmpty() && dataset.size < REQUIRED_AMOUNT_OF_ITEMS_FOR_TUTORING
    }

}