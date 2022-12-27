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

package org.smartregister.fhircore.engine.configuration

import androidx.compose.material.SnackbarDuration
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig

class QuestionnaireConfigTest {

  private val planDefinitionsList = listOf<String>("plan1", "plan2")

  private val confirmationDialog =
    ConfirmationDialog("dialog title", "dialog message", "action text")
  private val snackBarActionsList = emptyList<ActionConfig>()
  private val groupResourceConfig =
    GroupResourceConfig(
      groupIdentifier = "identifier",
      memberResourceType = "resourceType",
      removeMember = false,
      removeGroup = true,
      deactivateMembers = true
    )
  private val snackBarMessageConfig =
    SnackBarMessageConfig(
      "message",
      "label",
      SnackbarDuration.Short,
      snackBarActionsList,
    )

  private val questionnaireConfig =
    QuestionnaireConfig(
      id = "1234",
      title = "title",
      saveButtonText = "buttonText",
      setPractitionerDetails = true,
      setOrganizationDetails = false,
      planDefinitions = planDefinitionsList,
      type = QuestionnaireType.DEFAULT,
      resourceIdentifier = "resourceIdentifier",
      resourceType = "resourceType",
      confirmationDialog = confirmationDialog,
      groupResource = groupResourceConfig,
      taskId = "2222",
      saveDraft = false,
      snackBarMessage = snackBarMessageConfig
    )

  @Test
  fun testQuestionnaireConfig() {
    Assert.assertEquals("1234", questionnaireConfig.id)
    Assert.assertEquals("title", questionnaireConfig.title)
    Assert.assertEquals("buttonText", questionnaireConfig.saveButtonText)
    Assert.assertEquals(true, questionnaireConfig.setPractitionerDetails)
    Assert.assertEquals(false, questionnaireConfig.setOrganizationDetails)
    Assert.assertEquals(planDefinitionsList, questionnaireConfig.planDefinitions)
    Assert.assertEquals(QuestionnaireType.DEFAULT, questionnaireConfig.type)
    Assert.assertEquals("resourceIdentifier", questionnaireConfig.resourceIdentifier)
    Assert.assertEquals("resourceType", questionnaireConfig.resourceType)
    Assert.assertEquals(confirmationDialog, questionnaireConfig.confirmationDialog)
    Assert.assertEquals("dialog title", questionnaireConfig.confirmationDialog?.title)
    Assert.assertEquals("dialog message", questionnaireConfig.confirmationDialog?.message)
    Assert.assertEquals("action text", questionnaireConfig.confirmationDialog?.actionButtonText)
    Assert.assertEquals(groupResourceConfig, questionnaireConfig.groupResource)
    Assert.assertEquals("identifier", questionnaireConfig.groupResource?.groupIdentifier)
    Assert.assertEquals("resourceType", questionnaireConfig.groupResource?.memberResourceType)
    Assert.assertEquals(false, questionnaireConfig.groupResource?.removeMember)
    Assert.assertEquals(true, questionnaireConfig.groupResource?.removeGroup)
    Assert.assertEquals(true, questionnaireConfig.groupResource?.deactivateMembers)
    Assert.assertEquals("2222", questionnaireConfig.taskId)
    Assert.assertEquals(snackBarMessageConfig, questionnaireConfig.snackBarMessage)
    Assert.assertEquals("message", questionnaireConfig.snackBarMessage?.message)
    Assert.assertEquals("label", questionnaireConfig.snackBarMessage?.actionLabel)
    Assert.assertEquals(SnackbarDuration.Short, questionnaireConfig.snackBarMessage?.duration)
  }
}
