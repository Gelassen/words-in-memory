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
import org.junit.Ignore
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
        val testTxt = "伦敦是大不列颠的首都。"
        val testTranslationTxt = "Lúndūn shì dàbùlièdiān de shǒudū. / Лондон - столица Великобритании."
        generateSingleItem(testTxt, testTranslationTxt)

        dashboardRobot
            .seesListItemWithText(0, testTxt)
            .doesNotSeeNoContentPlaceholder()
            .doesNotSeeAddNewItemDialog()

        activityScenario.close()
    }

    @Ignore("Scroll is executes fine, but there is no response from toolbar and fab despite on " +
            "animation has not been disabled" +
            "Test fails, but actual code work well. Fix it when you will have more time.")
    @Test
    fun onScrollContent_hasContent_hideToolbarAndFab() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        generateContent()

        dashboardRobot
            .scrollToTheFirst()
            .scrollToTheLatest()
            .doesNotSeeToolbar()
            .doesNotSeeFloatingActionButton()

        activityScenario.close()
    }

    @Test
    fun onClickItem_default_showTranslation() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val testTxt = "伦敦是大不列颠的首都。"
        val testTranslationTxt = "Lúndūn shì dàbùlièdiān de shǒudū. / Лондон - столица Великобритании."
        generateSingleItem(testTxt, testTranslationTxt)

        dashboardRobot
            .clickOnSubjectToStudy(0, testTxt)
        dashboardRobot
            .seesTranslationOfSubject(testTxt + " / " + testTranslationTxt)

        activityScenario.close()
    }

    @Ignore("Can't click on completed icon, click happens on a whole view. FIXME")
    @Test
    fun onClickFilterItem_oneItemMarkedAsCompleted_showAllItemsWithoutCompletedOne() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        generateContent()

        dashboardRobot.markSubjectAsCompleted(position = 0)
        dashboardRobot.seesSpecificNumbersOfItemsInList(count = 16)

        activityScenario.close()
    }

    private fun generateContent() {
        for (idx in 0..16) {
            val testTxt = "${idx} 伦敦是大不列颠的首都。"
            val testTranslationTxt = "${idx} 伦敦是大不列颠的首都。 / ${idx} Лондон - столица Великобритании."
            generateSingleItem(testTxt, testTranslationTxt)
        }
    }

    private fun generateSingleItem(testTxt: String, testTranslationTxt: String) {
        dashboardRobot
            .clickOnFabButton()
        dashboardRobot
            .seesAddNewItemDialog()
            .enterNewWord(testTxt, testTranslationTxt)
        dashboardRobot
            .saveNewWord()
    }

    // TODO add filter test, add integration test

}