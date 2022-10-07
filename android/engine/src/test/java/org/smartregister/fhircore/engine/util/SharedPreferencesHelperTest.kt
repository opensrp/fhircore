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
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hl7.fhir.r4.model.Practitioner
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString

@HiltAndroidTest
class SharedPreferencesHelperTest : RobolectricTest() {

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
  fun testWriteStringAsync() {
    sharedPreferencesHelper.write("anyStringKey", "test write String", async = true)
    Assert.assertEquals("test write String", sharedPreferencesHelper.read("anyStringKey", ""))
  }

  @Test
  fun testWriteBoolean() {
    sharedPreferencesHelper.write("anyBooleanKey", true)
    Assert.assertEquals(true, sharedPreferencesHelper.read("anyBooleanKey", false))
  }

  @Test
  fun testWriteBooleanAsync() {
    sharedPreferencesHelper.write("anyBooleanKey", true, async = true)
    Assert.assertEquals(true, sharedPreferencesHelper.read("anyBooleanKey", false))
  }

  @Test
  fun testWriteLong() {
    sharedPreferencesHelper.write("anyLongKey", 123456789)
    Assert.assertEquals(123456789, sharedPreferencesHelper.read("anyLongKey", 0))
  }

  @Test
  fun testWriteLongAsync() {
    sharedPreferencesHelper.write("anyLongKey", 123456789, async = true)
    Assert.assertEquals(123456789, sharedPreferencesHelper.read("anyLongKey", 0))
  }

  @Test
  fun testReadObject() {
    val practitioner = Practitioner().apply { id = "1234" }
    sharedPreferencesHelper.write(LOGGED_IN_PRACTITIONER, practitioner.encodeResourceToString())

    val readPractitioner =
      sharedPreferencesHelper.read<Practitioner>(LOGGED_IN_PRACTITIONER, decodeFhirResource = true)
    Assert.assertNotNull(readPractitioner!!.logicalId)
    Assert.assertEquals(practitioner.logicalId, readPractitioner.logicalId)

    sharedPreferencesHelper.write(
      USER_INFO_SHARED_PREFERENCE_KEY,
      UserInfo(keycloakUuid = "1244").encodeJson()
    )
    Assert.assertNotNull(sharedPreferencesHelper.read<UserInfo>(USER_INFO_SHARED_PREFERENCE_KEY))
  }
}
