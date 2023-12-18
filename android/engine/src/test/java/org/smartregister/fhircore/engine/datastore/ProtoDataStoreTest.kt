package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.datastore.mockdata.SerializablePractitionerDetails
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
internal class ProtoDataStoreTest : RobolectricTest()  {
    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var protoDataStore: ProtoDataStore

    @Before
    fun setUp() {
        hiltRule.inject()
        protoDataStore = ProtoDataStore(testContext)
    }

    @Test
    fun testReadPractitionerDetails() {
        val expectedPreferencesValue = SerializablePractitionerDetails()
        runTest {
            protoDataStore.practitioner.map { dataStoreValue ->
                assert(dataStoreValue == expectedPreferencesValue)
            }
        }
    }

    @Test
    fun testWritePractitionerDetails() {
        val valueToWrite = SerializablePractitionerDetails( name = "Kelvin", id = 1 )
        runTest {
            protoDataStore.writePractitioner(valueToWrite)
            protoDataStore.practitioner.map {
                assert(it == (valueToWrite))
            }
        }
    }

    @Test
    fun testReadUserInfo() {
        val expectedPreferencesValue = SerializablePractitionerDetails()
        runTest {
            protoDataStore.practitioner.map { dataStoreValue ->
                assert(dataStoreValue == expectedPreferencesValue)
            }
        }
    }

    @Test
    fun testWriteUserInfo() {
        val valueToWrite = SerializableUserInfo( name = "Kelvin")
        runTest {
            protoDataStore.writeUserInfo(valueToWrite)
            protoDataStore.userInfo.map {
                assert(it == valueToWrite)
            }
        }
    }

}