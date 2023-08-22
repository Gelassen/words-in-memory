package io.github.gelassen.wordinmemory.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.ui.tutoring.TutoringPartOneFragment
import io.github.gelassen.wordinmemory.utils.Qualifier

class MainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, TutoringPartOneFragment.newInstance())
            .commit()

        Log.d(App.TAG, "Is screen big enough? ${Qualifier().isScreenBigEnough(this)}")
    }


}