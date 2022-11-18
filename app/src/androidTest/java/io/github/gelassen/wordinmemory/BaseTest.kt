package io.github.gelassen.wordinmemory

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before

open class BaseTest {

    protected lateinit var appContext: Context

    @Before
    open fun setUp() {
        // implement custom test runner https://developer.android.com/codelabs/android-dagger#13
        appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    }

    @After
    open fun tearDown() {
        // no op
    }

}