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

package org.smartregister.fhircore.quest.ui.patient.details

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.util.QuestConfigClassification

@HiltAndroidTest
@Ignore("To be deleted test class; new test to be written after refactor")
class QuestionnaireDataDetailDetailActivityTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val patientRepository: PatientRepository = mockk()
  @BindValue val libraryEvaluator: LibraryEvaluator = mockk()
  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("g6pd", mockk())

  private val hiltTestApplication = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private lateinit var questDetailActivity: QuestionnaireDataDetailActivity

  private lateinit var questDetailActivityController:
    ActivityController<QuestionnaireDataDetailActivity>

  @Before
  fun setUp() {
    hiltRule.inject()

    Faker.initPatientRepositoryMocks(patientRepository)

    QuestConfigClassification.CONTROL_TEST_DETAILS_VIEW

    every { sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null) } returns
      "{\"organization\":\"111\"}"

    val intent =
      Intent().apply {
        this.putExtra(
          QuestionnaireDataDetailActivity.CLASSIFICATION_ARG,
          "CONTROL_TEST_DETAILS_VIEW"
        )
      }
    questDetailActivityController =
      Robolectric.buildActivity(QuestionnaireDataDetailActivity::class.java, intent)
    questDetailActivity = questDetailActivityController.create().resume().get()
  }

  @After
  override fun tearDown() {
    super.tearDown()
    questDetailActivityController.pause().stop().destroy()
  }

  @Test
  fun testOnBackPressListenerShouldFinishActivity() {
    questDetailActivity.viewModel.onBackPressed(true)
    Assert.assertTrue(questDetailActivity.isFinishing)
  }

  @Test
  fun testOnFormItemClickListenerShouldStartQuestionnaireActivity() {
    questDetailActivity.viewModel.onFormItemClickListener(
      QuestionnaireConfig(form = "test-form", title = "Title", identifier = "1234")
    )

    val expectedIntent = Intent(questDetailActivity, QuestionnaireActivity::class.java)
    val actualIntent = shadowOf(hiltTestApplication).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }
}
