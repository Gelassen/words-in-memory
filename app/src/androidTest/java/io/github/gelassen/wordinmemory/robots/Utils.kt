package io.github.gelassen.wordinmemory.robots

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardAdapter
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers

object Utils {

    fun atPositionByTitle(position: Int, itemMatcher: Matcher<View?>): Matcher<View?>? {
        checkNotNull(itemMatcher)
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position)
                    ?: // has no item on such position
                    return false
                val item = matchByTitle(viewHolder.itemView)
                return itemMatcher.matches(item)
            }
        }
    }

    fun matchByTitle(root: View): View {
        return root.findViewById(R.id.toTranslate)
    }

    fun assertItemCountInList(count : Int): ViewAssertion {
        return ViewAssertion { view, noViewFoundException ->
            val adapter = (view as RecyclerView).adapter as DashboardAdapter
            assertThat(adapter.itemCount, Matchers.`is`(count)) }
    }

    class CompleteAction() : ViewAction {
        override fun getDescription(): String {
            return ("CompleteAction is performed")
        }

        override fun getConstraints(): Matcher<View> {
            return Matchers.allOf(
                ViewMatchers.isAssignableFrom(
                    RecyclerView::class.java
                ), ViewMatchers.isDisplayed()
            )
        }

        override fun perform(uiController: UiController?, view: View?) {
            uiController?.loopMainThreadUntilIdle()

            ViewActions.click().perform(
                uiController,
                view?.findViewById<ImageView>(R.id.completeIcon)
            )

            uiController?.loopMainThreadUntilIdle()
        }
    }
}