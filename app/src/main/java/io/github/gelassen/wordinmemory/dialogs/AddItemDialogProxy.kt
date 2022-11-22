package io.github.gelassen.wordinmemory.dialogs

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import io.github.gelassen.wordinmemory.model.SubjectToStudy
import io.github.gelassen.wordinmemory.utils.ConfigParams

class AddItemDialogProxy {

    private val configParams: ConfigParams = ConfigParams()

    fun show(selectedSubject: SubjectToStudy?, childFragmentManager: FragmentManager, activity: Activity) {
        if (configParams.showDialogAsBottomSheet(activity)) {
            showBottomSheetDialog(selectedSubject, childFragmentManager)
        } else {
            showDialogFragment(selectedSubject, childFragmentManager)
        }
    }

    private fun showDialogFragment(selectedSubject: SubjectToStudy?, childFragmentManager: FragmentManager) {
        if (selectedSubject == null) {
            AddItemDialogFragment.newInstance(Bundle.EMPTY)
                .show(childFragmentManager, AddItemDialogFragment.TAG)
        } else {
            AddItemDialogFragment.newInstance(selectedSubject)
                .show(childFragmentManager, AddItemDialogFragment.TAG)
        }
    }

    private fun showBottomSheetDialog(selectedSubject: SubjectToStudy?, childFragmentManager: FragmentManager) {
        childFragmentManager.let {
            if (selectedSubject == null) {
                AddItemBottomSheetDialogFragment.newInstance(Bundle.EMPTY)
                    .show(it, AddItemBottomSheetDialogFragment.TAG)
            } else {
                AddItemBottomSheetDialogFragment.newInstance(selectedSubject)
                    .show(it, AddItemBottomSheetDialogFragment.TAG)
            }
        }
    }
}