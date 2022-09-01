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
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.model.practitioner.KeycloakUserDetails

@HiltAndroidTest
internal class SharedPreferencesHelperTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var gson: Gson

  @Before
  fun setUp() {
    hiltRule.inject()
    sharedPreferencesHelper = SharedPreferencesHelper(context = application, gson = gson)
  }

  @Test
  fun testReadString() {
    Assert.assertNotNull(sharedPreferencesHelper.read("anyStringKey", ""))
  }

  @Test
  fun testReadBoolean() {
    Assert.assertNotNull(sharedPreferencesHelper.read("anyBooleanKey", false))
  }

  @Test
  fun testReadLong() {
    Assert.assertNotNull(sharedPreferencesHelper.read("anyBooleanKey", 0))
  }

  @Test
  fun testWriteString() {
    sharedPreferencesHelper.write("anyStringKey", "test write String")
    Assert.assertEquals("test write String", sharedPreferencesHelper.read("anyStringKey", ""))
  }

  @Test
  fun testWriteBoolean() {
    sharedPreferencesHelper.write("anyBooleanKey", true)
    Assert.assertEquals(true, sharedPreferencesHelper.read("anyBooleanKey", false))
  }

  @Test
  fun testWriteLong() {
    sharedPreferencesHelper.write("anyLongKey", 123456789)
    Assert.assertEquals(123456789, sharedPreferencesHelper.read("anyLongKey", 0))
  }

  @Test
  fun writeObjectUsingSerialized() {
    val questionnaireConfig = QuestionnaireConfig(id = "123", title = "my-questionnaire")
    sharedPreferencesHelper.write("object", questionnaireConfig)
    Assert.assertEquals(
      questionnaireConfig.id,
      sharedPreferencesHelper.read<QuestionnaireConfig>("object", isSerialized = true)?.id
    )
  }

  @Test
  fun writeObjectUsingGson() {
    val keycloakUserDetails = KeycloakUserDetails().apply { id = "12345" }
    sharedPreferencesHelper.write("object", keycloakUserDetails)
    Assert.assertEquals(
      keycloakUserDetails.id,
      sharedPreferencesHelper.read<KeycloakUserDetails>("object")?.id
    )
  }

  @Test
  fun testResetSharedPrefsClearsData() {

    sharedPreferencesHelper.write("anyBooleanKey", true)
    sharedPreferencesHelper.write("anyLongKey", 123456789)

    Assert.assertEquals(123456789, sharedPreferencesHelper.read("anyLongKey", 0))
    Assert.assertEquals(true, sharedPreferencesHelper.read("anyBooleanKey", false))

    sharedPreferencesHelper.resetSharedPrefs()

    Assert.assertEquals(0, sharedPreferencesHelper.read("anyLongKey", 0))
    Assert.assertEquals(false, sharedPreferencesHelper.read("anyBooleanKey", false))
  }
}
