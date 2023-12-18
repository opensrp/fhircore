package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
internal class PreferenceDataStoreTest : RobolectricTest()  {
    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var preferenceDataStore: PreferenceDataStore

    private val keys = PreferenceDataStore.Keys

    @Before
    fun setUp() {
        hiltRule.inject()
        preferenceDataStore = PreferenceDataStore(testContext)
    }

    @Test
    fun testReadAppId() {
        val expectedValue = ""
        runTest {
            val valueFlow = preferenceDataStore.read(keys.APP_ID)
            valueFlow.map { value ->
                assert(value == expectedValue)
            }
        }
    }

    @Test
    fun testWriteAppId() {
        val newAppId = "new_app_id"
        val key = keys.APP_ID

        runTest {
            preferenceDataStore.write(key, newAppId)
            assert(preferenceDataStore.read(keys.APP_ID).first() == newAppId)
        }
    }
}