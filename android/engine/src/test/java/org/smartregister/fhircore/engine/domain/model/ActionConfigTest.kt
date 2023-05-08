/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.domain.model

import dagger.hilt.android.testing.HiltAndroidTest
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class ActionConfigTest : RobolectricTest() {
  @Test
  fun testActionConfigParamsBundleCreatesBundleAndFiltersOutPrepopulationParams() {
    val computedValuesMap =
      mapOf<String, Any>("patientId" to "patient-id", "patientName" to "Test Name")
    val actionParams =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "my-param1",
          dataType = DataType.INTEGER,
          key = "my-key",
          value = "100"
        ),
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "my-param2",
          dataType = DataType.STRING,
          key = "my-key",
          value = "@{value}"
        ),
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "my-param2",
          dataType = DataType.STRING,
          key = "my-key",
          value = ""
        ),
        ActionParameter(key = "patientId", value = "patient-id"),
        ActionParameter(key = "patientName", value = "@{patientName}")
      )
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444"),
        params = actionParams
      )

    val result = actionConfig.paramsBundle(computedValuesMap)

    Assert.assertEquals(2, result.size())
    Assert.assertEquals("patient-id", result.getString("patientId"))
    Assert.assertEquals("Test Name", result.getString("patientName"))
  }

  @Test
  fun testDisplayStringIsInterpolatedCorrectly() {
    val computedValuesMap = mapOf<String, Any>("testDisplay" to "This is a test Display")
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444"),
        display = "@{testDisplay}"
      )

    val result = actionConfig.display(computedValuesMap)

    Assert.assertEquals("This is a test Display", result)
  }

  fun testManagingEntityStringIsInterpolatedCorrectly() {
    val computedValuesMap =
      mapOf<String, Any>(
        "dialogTitle" to "Change household head",
        "dialogWarningMessage" to "Are you sure",
        "dialogContentMessage" to "Changing head",
        "noMembersErrorMessage" to "No members found",
        "managingEntityReassignedMessage" to "Head reassigned successfully",
      )
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444"),
        display = "Display",
        managingEntity =
          ManagingEntityConfig(
            dialogTitle = "@{dialogTitle}",
            dialogWarningMessage = "@{dialogWarningMessage}",
            dialogContentMessage = "@{dialogContentMessage}",
            noMembersErrorMessage = "@{noMembersErrorMessage}",
            managingEntityReassignedMessage = "@{managingEntityReassignedMessage}",
          )
      )

    val result = actionConfig.interpolateManagingEntity(computedValuesMap)
    Assert.assertEquals("Change household head", result?.dialogTitle)
    Assert.assertEquals("Are you sure", result?.dialogWarningMessage)
    Assert.assertEquals("Changing head", result?.dialogContentMessage)
    Assert.assertEquals("No members found", result?.noMembersErrorMessage)
    Assert.assertEquals("Head reassigned successfully", result?.managingEntityReassignedMessage)
  }

  @Test
  fun testInterpolateManagingEntityNoChangeIfNoValuesArePassed() {
    val actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444"),
        display = "Display",
        managingEntity =
          ManagingEntityConfig(
            dialogTitle = "@{dialogTitle}",
            dialogWarningMessage = "@{dialogWarningMessage}",
            dialogContentMessage = "@{dialogContentMessage}",
            noMembersErrorMessage = "@{noMembersErrorMessage}",
            managingEntityReassignedMessage = "@{managingEntityReassignedMessage}",
          )
      )
    val oldActionConfig = actionConfig.copy()
    actionConfig.interpolateManagingEntity(emptyMap())

    Assert.assertEquals(oldActionConfig, actionConfig)
  }

  @Test
  fun testInterpolateManagingEntityHandlesNulls() {
    var actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444"),
        display = "Display"
      )
    var oldActionConfig = actionConfig.copy()
    actionConfig.interpolateManagingEntity(emptyMap())

    Assert.assertEquals(oldActionConfig, actionConfig)

    actionConfig =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "444"),
        display = "Display",
        managingEntity = ManagingEntityConfig()
      )
    oldActionConfig = actionConfig.copy()
    actionConfig.interpolateManagingEntity(emptyMap())

    Assert.assertEquals(oldActionConfig, actionConfig)
  }
}
