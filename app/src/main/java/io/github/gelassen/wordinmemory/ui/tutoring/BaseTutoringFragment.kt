package io.github.gelassen.wordinmemory.ui.tutoring

import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment

open class BaseTutoringFragment: DashboardFragment() {

    protected open fun clearModelState() {
        viewModel.clearState()
    }

}