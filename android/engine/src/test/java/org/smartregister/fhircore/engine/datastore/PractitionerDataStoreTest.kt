/*
 * Copyright 2021-2024 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.domain.model.PractitionerPreferences
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import javax.inject.Inject

@HiltAndroidTest
internal class PractitionerDataStoreTest : RobolectricTest() {
  private val testContext: Context = ApplicationProvider.getApplicationContext()

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @Inject
  private lateinit var dataStore: PractitionerDataStore

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testReadPractitionerDetails() {
    val expectedPreferencesValue = PractitionerPreferences()
    runTest {
      dataStore.observe.map { dataStoreValue ->
        assert(dataStoreValue == expectedPreferencesValue)
      }
    }
  }

  @Test
  fun testWritePractitionerDetails() { // can just test writing any value of the data class
    val valueToWrite = listOf("careTeamId1", "careTeamId2")
    runTest {
      dataStore.write(PractitionerDataStore.Keys.CARE_TEAM_IDS,valueToWrite)
      dataStore.observe.map { assert(it.careTeamIds == valueToWrite) }
    }
  }
}
