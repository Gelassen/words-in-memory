package io.github.gelassen.wordinmemory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class DashboardViewModel {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    @Mock
    lateinit var storageRepository: StorageRepository

    private lateinit var autoCloseable: AutoCloseable

    private lateinit var subj: DashboardViewModel

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        subj = DashboardViewModel(storageRepository)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    @Test
    fun `on getWords() call when there are data in db returns all data`() = runBlocking {
        // TODO complete me
    }
}