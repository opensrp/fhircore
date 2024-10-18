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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.runtime.mutableStateOf
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.search.StringFilterModifier
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterFilterConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterFilterField
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery

@HiltAndroidTest
class RegisterViewModelTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var registerViewModel: RegisterViewModel
  private lateinit var registerRepository: RegisterRepository
  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private val registerId = "register101"
  private val screenTitle = "Register 101"

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()
    registerRepository = mockk()
    sharedPreferencesHelper = mockk()
    registerViewModel =
      spyk(
        RegisterViewModel(
          registerRepository = registerRepository,
          configurationRegistry = configurationRegistry,
          sharedPreferencesHelper = sharedPreferencesHelper,
          resourceDataRulesExecutor = resourceDataRulesExecutor,
        ),
      )

    every {
      sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)
    } returns "Mar 20, 03:01PM"
  }

  @Test
  fun testPaginateRegisterData() {
    every { registerViewModel.retrieveRegisterConfiguration(any()) } returns
      RegisterConfiguration(
        appId = "app",
        id = registerId,
        fhirResource =
          FhirResourceConfig(baseResource = ResourceConfig(resource = ResourceType.Patient)),
        pageSize = 10,
      )
    registerViewModel.paginateRegisterData(registerId, false)
    val paginatedRegisterData = registerViewModel.registerData.value
    Assert.assertNotNull(paginatedRegisterData)
    Assert.assertTrue(registerViewModel.pagesDataCache.isNotEmpty())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testRetrieveRegisterUiState() = runTest {
    every { registerViewModel.retrieveRegisterConfiguration(any()) } returns
      RegisterConfiguration(
        appId = "app",
        id = registerId,
        fhirResource =
          FhirResourceConfig(baseResource = ResourceConfig(resource = ResourceType.Patient)),
        pageSize = 10,
      )
    every { registerViewModel.paginateRegisterData(any(), any()) } just runs
    coEvery { registerRepository.countRegisterData(any()) } returns 200
    registerViewModel.retrieveRegisterUiState(
      registerId = registerId,
      screenTitle = screenTitle,
      params = null,
      clearCache = false,
    )
    val registerUiState = registerViewModel.registerUiState.value
    Assert.assertNotNull(registerUiState)
    Assert.assertEquals(registerId, registerUiState.registerId)
    Assert.assertFalse(registerUiState.isFirstTimeSync)
    Assert.assertEquals(screenTitle, registerUiState.screenTitle)
    val registerConfiguration = registerUiState.registerConfiguration
    Assert.assertNotNull(registerConfiguration)
    Assert.assertEquals("app", registerConfiguration?.appId)
    Assert.assertEquals(200, registerUiState.totalRecordsCount)
    Assert.assertEquals(20, registerUiState.pagesCount)
  }

  @Test
  fun testOnEventSearchRegister() {
    every { registerViewModel.retrieveRegisterConfiguration(any()) } returns
      RegisterConfiguration(
        appId = "app",
        id = registerId,
        fhirResource =
          FhirResourceConfig(baseResource = ResourceConfig(resource = ResourceType.Patient)),
        pageSize = 10,
      )
    every { registerViewModel.registerUiState } returns
      mutableStateOf(RegisterUiState(registerId = registerId))
    // Search with empty string should paginate the data
    registerViewModel.onEvent(RegisterEvent.SearchRegister(SearchQuery.emptyText))
    verify { registerViewModel.retrieveRegisterUiState(any(), any(), any(), any()) }

    // Search for the word 'Khan' should call the filterRegisterData function
    registerViewModel.onEvent(RegisterEvent.SearchRegister(SearchQuery("Khan")))
    verify { registerViewModel.filterRegisterData(any()) }
  }

  @Test
  fun testOnEventMoveToNextPage() {
    every { registerViewModel.registerUiState } returns
      mutableStateOf(RegisterUiState(registerId = registerId))
    registerViewModel.currentPage.value = 1
    every { registerViewModel.paginateRegisterData(any(), any()) } just runs
    registerViewModel.onEvent(RegisterEvent.MoveToNextPage)
    Assert.assertEquals(2, registerViewModel.currentPage.value)
    verify { registerViewModel.paginateRegisterData(any(), any()) }
  }

  @Test
  fun testOnEventMoveToPreviousPage() {
    every { registerViewModel.registerUiState } returns
      mutableStateOf(RegisterUiState(registerId = registerId))
    registerViewModel.currentPage.value = 2
    every { registerViewModel.paginateRegisterData(any(), any()) } just runs
    registerViewModel.onEvent(RegisterEvent.MoveToPreviousPage)
    Assert.assertEquals(1, registerViewModel.currentPage.value)
    verify { registerViewModel.paginateRegisterData(any(), any()) }
  }

  // TODO Test (CHT) remaining scenarios (e.g. filter by quantity, reference, number, datetime etc)
  @Test
  fun testUpdateRegisterFilterStateShouldUpdateFilterState() {
    // Get state before the resource config is updated
    val registerFilterStateBefore = registerViewModel.registerFilterState.value

    val questionnaireConfig =
      QuestionnaireConfig(id = "register-patient", saveQuestionnaireResponse = false)

    val fhirResourceConfig = filterFhirResourceConfig()

    // QuestionnaireResponse linkId should match the register filter field IDs
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "1234"
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "gender-filter"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType("female")
              },
            )
          },
        )
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "birthdate-filter"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = DateType("2000-01-01")
              },
            )
          },
        )
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "task-status-filter"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = Coding("http://hl7.org/fhir/task-status", "ready", "Task status")
              },
            )
          },
        )
      }

    val dataFilterFields = registerFilterFields()
    every { registerViewModel.retrieveRegisterConfiguration(registerId) } returns
      RegisterConfiguration(
        appId = "app",
        id = registerId,
        fhirResource = fhirResourceConfig,
        pageSize = 10,
        registerFilter =
          RegisterFilterConfig(
            dataFilterActions =
              listOf(
                ActionConfig(
                  trigger = ActionTrigger.ON_CLICK,
                  workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE.name,
                  questionnaire = questionnaireConfig,
                ),
              ),
            dataFilterFields = dataFilterFields,
          ),
      )
    registerViewModel.updateRegisterFilterState(
      registerId = registerId,
      questionnaireResponse = questionnaireResponse,
    )

    val registerFilterStateAfter = registerViewModel.registerFilterState.value
    Assert.assertNull(registerFilterStateBefore.fhirResourceConfig)
    val updatedFhirResourceConfig = registerFilterStateAfter.fhirResourceConfig
    Assert.assertNotNull(updatedFhirResourceConfig)

    // Data queries assertions to confirm that the queries were updated.
    // If no queries existed before filter then new ones will be created.
    val newBaseResourceQueries = updatedFhirResourceConfig?.baseResource?.dataQueries
    Assert.assertNotNull(newBaseResourceQueries)
    Assert.assertNotNull(
      newBaseResourceQueries!!.find { dataQuery ->
        dataQuery.paramName == "gender" &&
          dataQuery.filterCriteria.any {
            it.dataType == Enumerations.DataType.STRING && it.value == "female"
          }
      },
    )

    Assert.assertNotNull(
      newBaseResourceQueries.find { dataQuery ->
        dataQuery.paramName == "birthdate" &&
          dataQuery.filterCriteria.any {
            it.dataType == Enumerations.DataType.DATE && it.value == "2000-01-01"
          }
      },
    )

    Assert.assertNotNull(
      newBaseResourceQueries.find { dataQuery ->
        dataQuery.paramName == "relationship" &&
          dataQuery.filterCriteria.any {
            it.dataType == Enumerations.DataType.CODE && (it.value as Code).code == "N" ||
              it.dataType == Enumerations.DataType.CODE && (it.value as Code).code == "M"
          }
      },
    )

    val taskRelatedResourceDataQueries =
      updatedFhirResourceConfig.relatedResources
        .find { it.id == ResourceType.Task.name }
        ?.dataQueries
    Assert.assertNotNull(taskRelatedResourceDataQueries)
    Assert.assertNotNull(
      taskRelatedResourceDataQueries!!.find { dataQuery ->
        dataQuery.paramName == "status" &&
          dataQuery.filterCriteria.any {
            it.dataType == Enumerations.DataType.CODE && (it.value as Code).code == "ready"
          }
      },
    )
  }

  private fun registerFilterFields() =
    listOf(
      RegisterFilterField(
        filterId = ResourceType.Patient.name,
        dataQueries =
          listOf(
            DataQuery(
              paramName = "gender",
              filterCriteria =
                listOf(
                  FilterCriterionConfig.StringFilterCriterionConfig(
                    dataType = Enumerations.DataType.STRING,
                    modifier = StringFilterModifier.MATCHES_EXACTLY,
                    dataFilterLinkId = "gender-filter",
                  ),
                ),
            ),
            DataQuery(
              paramName = "birthdate",
              filterCriteria =
                listOf(
                  FilterCriterionConfig.DateFilterCriterionConfig(
                    dataType = Enumerations.DataType.DATE,
                    prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS,
                    dataFilterLinkId = "birthdate-filter",
                  ),
                ),
            ),
            DataQuery(
              paramName = "relationship",
              filterCriteria =
                listOf(
                  FilterCriterionConfig.TokenFilterCriterionConfig(
                    dataType = Enumerations.DataType.CODE,
                    value = Code(code = "N", system = "http://hl7.org/fhir/v2/0131"),
                  ),
                  FilterCriterionConfig.TokenFilterCriterionConfig(
                    dataType = Enumerations.DataType.CODE,
                    value = Code(code = "M", system = "http://hl7.org/fhir/v2/0132"),
                  ),
                ),
            ),
          ),
      ),
      RegisterFilterField(
        filterId = ResourceType.Task.name,
        dataQueries =
          listOf(
            DataQuery(
              paramName = "status",
              filterCriteria =
                listOf(
                  FilterCriterionConfig.TokenFilterCriterionConfig(
                    dataType = Enumerations.DataType.CODE,
                    dataFilterLinkId = "task-status-filter",
                  ),
                ),
            ),
          ),
      ),
    )

  private fun filterFhirResourceConfig() =
    FhirResourceConfig(
      baseResource =
        ResourceConfig(
          id = ResourceType.Patient.name,
          resource = ResourceType.Patient,
          filterId = ResourceType.Patient.name,
        ),
      relatedResources =
        listOf(
          ResourceConfig(
            id = ResourceType.Task.name,
            resource = ResourceType.Task,
            filterId = ResourceType.Task.name,
          ),
        ),
    )

  @Test
  fun testUpdateRegisterFilterStateWithNoAnswersForFilterQR() {
    val registerFilterStateBefore = registerViewModel.registerFilterState
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "1234"
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "gender-filter"
          },
        )
      }
    val fhirResourceConfig = filterFhirResourceConfig()
    val dataFilterFields = registerFilterFields()
    val questionnaireConfig =
      QuestionnaireConfig(id = "register-patient", saveQuestionnaireResponse = false)
    every { registerViewModel.retrieveRegisterConfiguration(any()) } returns
      RegisterConfiguration(
        appId = "app",
        id = registerId,
        fhirResource = fhirResourceConfig,
        pageSize = 10,
        registerFilter =
          RegisterFilterConfig(
            dataFilterActions =
              listOf(
                ActionConfig(
                  trigger = ActionTrigger.ON_CLICK,
                  workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE.name,
                  questionnaire = questionnaireConfig,
                ),
              ),
            dataFilterFields = dataFilterFields,
          ),
      )

    registerViewModel.updateRegisterFilterState(registerId, questionnaireResponse)

    val registerFilterStateAfter = registerViewModel.registerFilterState

    Assert.assertNull(registerFilterStateBefore.value.fhirResourceConfig?.baseResource?.dataQueries)
    Assert.assertNull(registerFilterStateAfter.value.fhirResourceConfig?.baseResource?.dataQueries)
  }
}
