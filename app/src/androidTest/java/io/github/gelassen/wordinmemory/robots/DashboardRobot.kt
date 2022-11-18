package io.github.gelassen.wordinmemory.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.github.gelassen.wordinmemory.matchers.CustomMatchers
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.robots.Utils.atPositionByTitle
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.StringContains

class DashboardRobot {

    fun seesNoContent() : DashboardRobot {
        seesListItems(resId = R.id.dashboardList, count = 0)
        return this
    }

    fun seesToolbar() : DashboardRobot {
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
        return this
    }

    fun seesFloatingActionButton() : DashboardRobot {
        onView(withId(R.id.dashboardAddNewWord))
            .check(matches(isDisplayed()))
        return this
    }

    fun seesNoContentPlaceholder() : DashboardRobot {
        onView(withId(R.id.noContentPlaceholder))
            .check(matches(isDisplayed()))
        return this
    }

    fun seesListItems(resId: Int, count: Int): DashboardRobot {
        onView(withId(resId))
            .check(matches(CustomMatchers().recyclerViewSizeMatch(count)))
        return this
    }

    fun seesAddNewItemDialog(): DashboardRobot {
        onView(withId(R.id.save))
            .check(matches(isDisplayed()))
        onView(withId(R.id.toTranslateEditText))
            .check(matches(isDisplayed()))
        onView(withId(R.id.translateEditText))
            .check(matches(isDisplayed()))
        return this
    }

    fun seesListItemWithText(order: Int, text: String): DashboardRobot {
        onView(withId(R.id.dashboardList))
            .check(matches(isDisplayed()))
        onView(withId(R.id.dashboardList))
            .check(matches(atPositionByTitle(order,
                ViewMatchers.withText(StringContains.containsString(text))
            )))
        return this
    }

    fun doesNotSeeNoContentPlaceholder(): DashboardRobot {
        onView(withId(R.id.noContentPlaceholder))
            .check(matches(isDisplayed()))
        return this
    }

    fun doesNotSeeAddNewItemDialog(): DashboardRobot {
        onView(withId(R.id.save))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.toTranslateEditText))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.translateEditText))
            .check(matches(not(isDisplayed())))
        return this
    }

    /* actions*/

    fun clickOnFabButton() {
        onView(withId(R.id.dashboardAddNewWord))
            .perform(ViewActions.click())
    }

    fun enterNewWord(txtSrc: String, txtTranslation: String) {
        onView(withId(R.id.toTranslateEditText))
            .perform(ViewActions.typeText(txtSrc))
        onView(withId(R.id.translateEditText))
            .perform(ViewActions.typeText(txtTranslation))
    }

    fun saveNewWord() {
        onView(withId(R.id.save))
            .perform(ViewActions.click())
    }


}