package io.github.gelassen.wordinmemory.matchers

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import io.github.gelassen.wordinmemory.App
import org.hamcrest.Description
import org.hamcrest.Matcher

class CustomMatchers {

    fun recyclerViewSizeMatch(matcherSize: Int): Matcher<View?>? {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("RecyclerView with list size: $matcherSize")
            }

            override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                Log.d("Test", "Item count ${recyclerView.adapter!!.itemCount}")
                return matcherSize == recyclerView.adapter!!.itemCount
            }
        }
    }
}