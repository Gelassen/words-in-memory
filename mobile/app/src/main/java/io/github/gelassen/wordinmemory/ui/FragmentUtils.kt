package io.github.gelassen.wordinmemory.ui

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.github.gelassen.wordinmemory.R
import kotlinx.coroutines.CoroutineScope

class FragmentUtils {

    fun showProgressIndicator(fragment: Fragment?) {
        val toolbar = findRightFragmentWithRightComponent(fragment)
        val progressIndicator = toolbar?.findViewById<ProgressBar>(R.id.progress_indicator)
        progressIndicator?.visibility = View.VISIBLE
    }

    fun hideProgressIndicator(fragment: Fragment?) {
        val toolbar = findRightFragmentWithRightComponent(fragment)
        val progressIndicator = toolbar?.findViewById<ProgressBar>(R.id.progress_indicator)
        progressIndicator?.visibility = View.GONE
    }

    private fun findRightFragmentWithRightComponent(fragment: Fragment?): Toolbar? {
        if (fragment == null)
            return null

        val toolbar = fragment.view?.findViewById<Toolbar>(R.id.toolbar)
        if (toolbar == null) {
            return findRightFragmentWithRightComponent(fragment.parentFragment)
        } else {
            return toolbar
        }
    }
}