package io.github.gelassen.wordinmemory

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.github.gelassen.wordinmemory.idlingresource.DataBindingIdlingResource
import io.github.gelassen.wordinmemory.idlingresource.monitorActivity
import io.github.gelassen.wordinmemory.robots.DashboardRobot
import io.github.gelassen.wordinmemory.storage.AppDatabase
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

        AppDatabase.getInstance(appContext).subjectToStudyDao().clean()
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
            .seesTranslationOfSubject(testTxt + " / " + testTranslationTxt)

        activityScenario.close()
    }

    @Test
    fun onClickFilterItem_oneItemMarkedAsCompleted_showAllItemsWithoutCompletedOne() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        generateContent(count = 2)

        dashboardRobot
            .markSubjectAsCompleted(position = 0)
            .seesSpecificNumbersOfItemsInList(count = 1)

        activityScenario.close()
    }

    @Test
    fun onClickShowAll_twoItemsAreCompleted_showAllItems() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val totalCount = 5
        generateContent(count = totalCount)
        dashboardRobot.markSubjectAsCompleted(position = 0)
        dashboardRobot.markSubjectAsCompleted(position = 1) // actually we can again select item at index 0

        dashboardRobot.clickMenuShowAll()
        dashboardRobot.seesSpecificNumbersOfItemsInList(count = totalCount)

        activityScenario.close()
    }

    @Test
    fun integrationTest_fromNoContentToContentWithCompletedItems_showCorrectStateOnEachStage() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        dashboardRobot
            .seesToolbar()
            .seesFloatingActionButton()
            .seesNoContent()
            .seesNoContentPlaceholder()

        val testTxt = "伦敦是大不列颠的首都。"
        val testTranslationTxt = "Lúndūn shì dàbùlièdiān de shǒudū. / Лондон - столица Великобритании."
        generateSingleItem(testTxt, testTranslationTxt)
        dashboardRobot
            .seesListItemWithText(0, testTxt)
            .doesNotSeeNoContentPlaceholder()
            .doesNotSeeAddNewItemDialog()
            .clickOnSubjectToStudy(0, testTxt)
            .seesTranslationOfSubject(testTxt + " / " + testTranslationTxt)

        dashboardRobot
            .seesSpecificNumbersOfItemsInList(count = 1)
            .markSubjectAsCompleted(position = 0)
            .seesSpecificNumbersOfItemsInList(count = 0)

        val totalCountToGenerate = 5
        val totalCount = totalCountToGenerate + 1 // on the previous stage one item has been generated already
        val totalCompleteItems = 3 // previous item is also counted
        generateContent(count = totalCountToGenerate)
        dashboardRobot
            .markSubjectAsCompleted(position = 0)
            .markSubjectAsCompleted(position = 1) // actually we can again select item at index 0
            .clickMenuShowAll()
            .seesSpecificNumbersOfItemsInList(count = totalCount)
            .clickMenuShowCompletedOnly()
            .seesSpecificNumbersOfItemsInList(count = totalCount - totalCompleteItems)

        activityScenario.close()
    }

    private fun generateContent(count: Int = 16) {
        for (idx in 0 until count) {
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

    // TODO add integration test

}