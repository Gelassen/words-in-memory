package io.github.gelassen.wordinmemory.robots

import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.matchers.CustomMatchers
import io.github.gelassen.wordinmemory.robots.Utils.atPositionByTitle
import io.github.gelassen.wordinmemory.robots.Utils.childAtPosition
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardAdapter
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
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
            .check(matches(isDisplayed()))
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
            .check(matches(not(isDisplayed())))
        return this
    }

    fun doesNotSeeAddNewItemDialog(): DashboardRobot {
        onView(withId(R.id.save))
            .check(doesNotExist())
        onView(withId(R.id.toTranslateEditText))
            .check(doesNotExist())
        onView(withId(R.id.translateEditText))
            .check(doesNotExist())
        return this
    }

    fun scrollToTheFirst(): DashboardRobot {
        onView(withId(R.id.dashboardList))
            .perform(RecyclerViewActions.scrollToPosition<DashboardAdapter.ViewHolder>(0))
        return this
    }

    fun scrollToTheLatest(): DashboardRobot {
        onView(withId(R.id.dashboardList))
            .perform(RecyclerViewActions.scrollToLastPosition<DashboardAdapter.ViewHolder>())
        return this
    }

    fun seesTranslationOfSubject(textWithTranslation: String): DashboardRobot {
        onView(withId(R.id.dashboardList))
            .check(
                matches(
                    atPositionByTitle(
                        0,
                        ViewMatchers.withText(StringContains.containsString(textWithTranslation))
                    )
                )
            )
        return this
    }

    fun doesNotSeeToolbar(): DashboardRobot {
        onView(withId(R.id.toolbar))
            .check(matches(not(isDisplayed())))
        return this
    }

    fun doesNotSeeFloatingActionButton(): DashboardRobot {
        onView(withId(R.id.dashboardAddNewWord))
            .check(matches(not(isDisplayed())))
        return this
    }

    fun seesSpecificNumbersOfItemsInList(count: Int): DashboardRobot{
        onView(withId(R.id.dashboardList))
            .check(Utils.assertItemCountInList(count))
        return this
    }

    /* actions*/

    fun clickOnFabButton(): DashboardRobot {
        onView(withId(R.id.dashboardAddNewWord))
            .perform(ViewActions.click())
        return this
    }

    fun enterNewWord(txtSrc: String, txtTranslation: String): DashboardRobot {
        onView(withId(R.id.toTranslateEditText))
            .perform(ViewActions.replaceText(txtSrc))
            .perform(ViewActions.pressKey(KeyEvent.KEYCODE_ENTER))
            /*.perform(ViewActions.typeText(txtSrc)) // doesn't work due known issue of keyboard */
        onView(withId(R.id.translateEditText))
            .perform(ViewActions.replaceText(txtTranslation))
            .perform(ViewActions.pressKey(KeyEvent.KEYCODE_ENTER))
            /*.perform(ViewActions.typeText(txtTranslation)) // doesn't work due known issue of keyboard */
        return this
    }

    fun saveNewWord(): DashboardRobot {
        onView(withId(R.id.save))
            .perform(ViewActions.click())
        return this
    }

    fun clickOnSubjectToStudy(position: Int, text: String): DashboardRobot {
        onView(withId(R.id.dashboardList))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<DashboardAdapter.ViewHolder>(
                    position,
                    ViewActions.click()
                )
            )
        return this
    }

    fun markSubjectAsCompleted(position: Int, text: String = ""): DashboardRobot {
        onView(withId(R.id.dashboardList))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<DashboardAdapter.ViewHolder>(
                    position,
                    Utils.CompleteAction()
                )
            )
        return this
    }

    fun clickMenuShowAll(): DashboardRobot {
        onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("More options"),
                ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.toolbar))),
                isDisplayed()
            )
        )
            .check(matches(isDisplayed()))

        onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("More options"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
            .perform(ViewActions.click())

        onView(
            allOf(
                withId(androidx.appcompat.R.id.title), ViewMatchers.withText("Show all"),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.content),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
            .perform(ViewActions.click())

        return this
    }

    fun clickMenuShowCompletedOnly(): DashboardRobot {
        onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("More options"),
                ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.toolbar))),
                isDisplayed()
            )
        )
            .check(matches(isDisplayed()))

        onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("More options"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
            .perform(ViewActions.click())

        onView(
            allOf(
                withId(androidx.appcompat.R.id.title), ViewMatchers.withText("Show only not completed"),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.content),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
            .perform(ViewActions.click())

        return this
    }
}