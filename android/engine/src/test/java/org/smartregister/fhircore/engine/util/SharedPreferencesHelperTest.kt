/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
internal class SharedPreferencesHelperTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Before
  fun setUp() {
    sharedPreferencesHelper = SharedPreferencesHelper(application)
  }

  @Test
  fun testReadString() {
    Assert.assertNotNull(sharedPreferencesHelper.read("anyStringKey", EMPTY_STRING))
  }

  @Test
  fun testReadBoolean() {
    Assert.assertNotNull(sharedPreferencesHelper.read("anyBooleanKey", false))
  }

  @Test
  fun testReadLong() {
    Assert.assertNotNull(sharedPreferencesHelper.read("anyBooleanKey", 0))
  }

  @Ignore
  @Test
  fun testWriteString() {
    verify { sharedPreferencesHelper.write("anyStringKey", "test write") }
  }

  @Ignore
  @Test
  fun testWriteBoolean() {
    verify { sharedPreferencesHelper.write("anyBooleanKey", true) }
  }

  @Ignore
  @Test
  fun testWriteLong() {
    verify { sharedPreferencesHelper.write("anyLongKey", 0) }
  }
}
