package io.github.gelassen.wordinmemory.robots

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import io.github.gelassen.wordinmemory.R

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
}