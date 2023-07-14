package io.github.gelassen.wordinmemory.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment
import io.github.gelassen.wordinmemory.utils.ConfigParams
import io.github.gelassen.wordinmemory.utils.Qualifier

class MainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // FIXME resolve this WIP commit caused by unclear sentry behaviour
        // https://github.com/getsentry/sentry-java/issues/2841

        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, DashboardFragment.newInstance())
            .commit()

        Log.d(App.TAG, "Is screen big enough? ${Qualifier().isScreenBigEnough(this)}")
    }


}