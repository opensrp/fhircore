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
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hl7.fhir.r4.model.Practitioner
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.encodeJson

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
    sharedPreferencesHelper = SharedPreferencesHelper(application, gson)
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
    sharedPreferencesHelper.write("anyStringKey", "test write String")
    Assert.assertEquals("test write String", sharedPreferencesHelper.read("anyStringKey", ""))
  }

  @Test
  fun testWriteBoolean() {
    sharedPreferencesHelper.write("anyBooleanKey", true)
    Assert.assertEquals(true, sharedPreferencesHelper.read("anyBooleanKey", false))
  }

  @Test
  fun testWriteBooleanAsync() {
    sharedPreferencesHelper.write("anyBooleanKey", true)
    Assert.assertEquals(true, sharedPreferencesHelper.read("anyBooleanKey", false))
  }

  @Test
  fun testWriteLong() {
    sharedPreferencesHelper.write("anyLongKey", 123456789)
    Assert.assertEquals(123456789, sharedPreferencesHelper.read("anyLongKey", 0))
  }

  @Test
  fun testWriteLongAsync() {
    sharedPreferencesHelper.write("anyLongKey", 123456789)
    Assert.assertEquals(123456789, sharedPreferencesHelper.read("anyLongKey", 0))
  }

  @Test
  fun writeObjectUsingSerialized() {
    val questionnaireConfig =
      QuestionnaireConfig(form = "123", identifier = "123", title = "my-questionnaire")
    sharedPreferencesHelper.write("object", questionnaireConfig)
    Assert.assertEquals(
      questionnaireConfig.identifier,
      sharedPreferencesHelper.read<QuestionnaireConfig>("object")?.identifier
    )
  }

  @Test
  fun writeObjectUsingGson() {
    val practitioner = Practitioner().apply { id = "1234" }
    sharedPreferencesHelper.write("object", practitioner, encodeWithGson = true)
    Assert.assertEquals(
      practitioner.id,
      sharedPreferencesHelper.read<Practitioner>("object", decodeWithGson = true)?.id
    )
  }

  @Test
  fun testReadObject() {
    val practitioner = Practitioner().apply { id = "1234" }
    sharedPreferencesHelper.write(LOGGED_IN_PRACTITIONER, practitioner, encodeWithGson = true)

    val readPractitioner =
      sharedPreferencesHelper.read<Practitioner>(LOGGED_IN_PRACTITIONER, decodeWithGson = true)
    Assert.assertNotNull(readPractitioner!!.logicalId)
    Assert.assertEquals(practitioner.logicalId, readPractitioner.logicalId)

    sharedPreferencesHelper.write(
      USER_INFO_SHARED_PREFERENCE_KEY,
      UserInfo(keycloakUuid = "1244").encodeJson()
    )
    Assert.assertNotNull(sharedPreferencesHelper.read<UserInfo>(USER_INFO_SHARED_PREFERENCE_KEY))
  }
}
