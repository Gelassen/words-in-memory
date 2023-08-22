package io.github.gelassen.wordinmemory.ui.tutoring

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardAdapter
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment

abstract class BaseTutoringFragment: DashboardFragment() {

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
                onCompleteDailyPractice(dataset)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /* super.onCreateOptionsMenu(menu, inflater) */
        /* disable menu for this screen */
    }

    abstract fun onCompleteDailyPractice(dataset: MutableList<SubjectToStudy>)

    protected open fun clearModelState() {
        viewModel.clearState()
    }

    protected open fun finishWork() {
        viewModel.clearState()
        showMainScreen()
    }

    protected fun showMainScreen() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, DashboardFragment.newInstance())
            .commit()
    }
}