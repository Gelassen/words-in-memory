package io.github.gelassen.wordinmemory

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.github.gelassen.wordinmemory.idlingresource.DataBindingIdlingResource
import io.github.gelassen.wordinmemory.idlingresource.monitorActivity
import io.github.gelassen.wordinmemory.robots.DashboardRobot
import io.github.gelassen.wordinmemory.ui.MainActivity
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DashboardTest : BaseTest() {

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val dashboardRobot: DashboardRobot = DashboardRobot()

    override fun setUp() {
        super.setUp()
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    override fun tearDown() {
        super.tearDown()
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun onAppStart_noContent_toolbarFabNoContentPlaceholderAreShown() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        dashboardRobot
            .seesToolbar()
            .seesFloatingActionButton()
            .seesNoContent()
            .seesNoContentPlaceholder()

        activityScenario.close()
    }

    @Test
    fun onFabTap_noContentYet_showAddNewItemDialog() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        dashboardRobot
            .clickOnFabButton()
        dashboardRobot
            .seesAddNewItemDialog()

        activityScenario.close()
    }

    @Test
    fun onFabTap_enterContentAndSave_newItemIsVisible() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val testTxt = "The London is the capital of the Great Britain."
        val testTranslationTxt = "Лондон - столица Великобритании."

        dashboardRobot
            .clickOnFabButton()
        dashboardRobot
            .seesAddNewItemDialog()
            .enterNewWord(testTxt, testTranslationTxt)
        dashboardRobot
            .saveNewWord()

        dashboardRobot
            .seesListItemWithText(0, testTxt)
            .doesNotSeeNoContentPlaceholder()
            .doesNotSeeAddNewItemDialog()

        activityScenario.close()
    }

    // TODO add scroll test, add filter test, add integration test

}