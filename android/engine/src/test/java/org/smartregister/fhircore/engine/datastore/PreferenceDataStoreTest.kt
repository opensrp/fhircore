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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
internal class PreferenceDataStoreTest : RobolectricTest() {
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
      valueFlow.map { value -> assert(value == expectedValue) }
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
