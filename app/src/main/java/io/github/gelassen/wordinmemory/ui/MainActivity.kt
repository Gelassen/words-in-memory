package io.github.gelassen.wordinmemory.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment

class MainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, DashboardFragment.newInstance())
//            .addToBackStack(DashboardFragment.TAG)
            .commit()
    }


}