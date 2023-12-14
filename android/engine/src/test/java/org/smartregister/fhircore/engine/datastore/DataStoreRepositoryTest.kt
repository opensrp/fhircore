package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.datastore.mockdata.PrefsDataStoreParams
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
internal class DataStoreRepositoryTest: RobolectricTest() {

    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var dataStoresRepository: DataStoresRepository

    private val keys = DataStoresRepository.Keys

    @Before
    fun setUp() {
        hiltRule.inject()
        dataStoresRepository = DataStoresRepository(testContext)
    }

    @Test
    fun testInitialReadPreferencesDataStore() {
        val expectedPreferencesValue = PrefsDataStoreParams()
        runTest {
            dataStoresRepository.preferences.map {dataStoreValue ->
                assert(dataStoreValue.equals(expectedPreferencesValue))
            }
        }
    }

    @Test
    fun testWriteAppId() {
        val newAppId = "new_app_id"
        val key = keys.APP_ID

        runTest {
            dataStoresRepository.writePrefs(key, newAppId)
            assert(dataStoresRepository.preferences.first().appId.equals(newAppId))
        }
    }
}