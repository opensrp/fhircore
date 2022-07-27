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

package org.smartregister.fhircore.quest.ui.family.profile

import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Date
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.FormConfiguration
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper

class FamilyProfileViewModelTest : RobolectricTest() {

  @Test
  fun onEvent_overflowMenuClick_shouldGetQuestionnaireResponseFromDB() {
    val configurationRegistry = mockk<ConfigurationRegistry>(relaxed = true)
    val questionnaireConfig =
      QuestionnaireConfig(
        form = FamilyProfileViewModel.FAMILY_REGISTRATION_FORM,
        title = "Family Registration",
        identifier = "1923"
      )
    val formConfiguration =
      FormConfiguration(
        appId = "test",
        classification = "forms",
        forms = listOf(questionnaireConfig)
      )
    every {
      configurationRegistry.retrieveConfiguration<FormConfiguration>(AppConfigClassification.FORMS)
    } returns formConfiguration

    val patientRegisterRepository = mockk<PatientRegisterRepository>()
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "1823"
        authored = Date()
        addItem().apply {
          linkId = "link-id-1"
          addAnswer().apply { value = StringType("answer-1") }
        }
      }
    coEvery {
      patientRegisterRepository.registerDaoFactory.familyRegisterDao.searchQuestionnaireResponses(
        subjectId = any(),
        subjectType = any(),
        questionnaireId = any()
      )
    } returns listOf(questionnaireResponse)

    val profileViewDataMapper = mockk<ProfileViewDataMapper>()

    val viewModel =
      FamilyProfileViewModel(
        overflowMenuFactory = OverflowMenuFactory(),
        configurationRegistry = configurationRegistry,
        patientRegisterRepository = patientRegisterRepository,
        profileViewDataMapper = profileViewDataMapper,
        dispatcherProvider = DefaultDispatcherProvider()
      )

    val familyId = "12345"
    val familyProfileEvent =
      FamilyProfileEvent.OverflowMenuClick(
        context = ApplicationProvider.getApplicationContext(),
        menuId = R.id.family_details,
        familyId = familyId
      )

    viewModel.onEvent(familyProfileEvent)

    verify {
      configurationRegistry.retrieveConfiguration<FormConfiguration>(AppConfigClassification.FORMS)
    }
    coVerify {
      patientRegisterRepository.registerDaoFactory.familyRegisterDao.searchQuestionnaireResponses(
        subjectId = "12345",
        subjectType = ResourceType.Group,
        questionnaireId = "1923"
      )
    }
  }
}
