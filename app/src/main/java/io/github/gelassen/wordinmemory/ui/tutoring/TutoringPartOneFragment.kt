package io.github.gelassen.wordinmemory.ui.tutoring

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.providers.DashboardProvider
import io.github.gelassen.wordinmemory.storage.AppQuickStorage
import io.github.gelassen.wordinmemory.ui.dashboard.StateFlag

class TutoringPartOneFragment : BaseTutoringFragment() {

    companion object {

        fun newInstance(): Fragment {
            return TutoringPartOneFragment()
        }
    }

    private val quickStorage: AppQuickStorage = AppQuickStorage()
    private val provider: DashboardProvider = DashboardProvider()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.completeText.text = getString(R.string.complete_daily_practice).plus(" 1 / 2")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(App.TAG, "onDestroy() from ${this.javaClass.simpleName}")
    }

    override fun onCompleteDailyPractice(dataset: MutableList<SubjectToStudy>) {
        lifecycleScope.launchWhenCreated {
            viewModel.completePartOneDailyPractice(requireActivity(), dataset)
        }
        completePartOneTutoring()
//        finishWork()
    }

    override fun runOnStart() {
        Log.d(App.TAG, "${this.javaClass.simpleName} runOnStart()")
        if (provider.isTimeToShowDailyTraining(
                lastShownTime = quickStorage.getLastTrainedTime(requireActivity()),
                currentTime = System.currentTimeMillis())) {
            lifecycleScope.launchWhenStarted {
                Log.d(App.TAG, "${this.javaClass.simpleName} showDailyPractice()")
                viewModel.showDailyPractice()
            }

            listenOnModelUpdates() { dataset ->
                Log.d(App.TAG, "${this.javaClass.simpleName} listenOnModelUpdates()")
//                if (viewModel.uiState.value.status == StateFlag.TUTORING_PART_ONE) { return@listenOnModelUpdates }
                if (viewModel.shallSkipPartOneTutoringScreen()
                    || viewModel.areNotEnoughWordsForPractice()) {
                    Log.d(App.TAG, "${this.javaClass.simpleName} completePartOneTutoring()")
                    completePartOneTutoring()
//                    finishWork()
                }
            }
        } else {
            finishWork()
        }
    }

    private fun completePartOneTutoring() {
        /**
         * without explicit cancellation from coroutine, it is triggered quicker rather than onDestroy()
         * call (onDestroy() would cancel coroutines automatically) which leads to infinite invocation loop
         * */
//        viewModel.viewModelScope.coroutineContext.cancelChildren()
        viewModel.clearState()
        showNextTutoringPart()
//        showMainScreen()
    }

    private fun showNextTutoringPart() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, TutoringPartTwoFragment.newInstance())
            .commit()
    }

}