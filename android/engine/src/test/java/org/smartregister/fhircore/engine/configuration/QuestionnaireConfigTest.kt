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

package org.smartregister.fhircore.engine.configuration

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.event.EventWorkflow
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class QuestionnaireConfigTest : RobolectricTest() {
  @Test
  fun testDefaultQuestionnaireConfig() {
    val id = "123"
    val questionnaireConfig = QuestionnaireConfig(id)
    val interpolatedConfig = questionnaireConfig.interpolate(emptyMap())
    interpolatedConfig.apply {
      Assert.assertEquals(id, this.id)
      Assert.assertEquals(null, taskId)
      Assert.assertEquals(null, title)
      Assert.assertEquals(null, resourceIdentifier)
      Assert.assertEquals(null, groupResource)
      Assert.assertEquals(null, confirmationDialog)
      Assert.assertEquals(null, planDefinitions)
      Assert.assertEquals(emptyList<String>(), readOnlyLinkIds)
    }
  }

  @Test
  fun testVerifyInterpolationInQuestionnaireConfig() {
    val questionnaireConfig =
      QuestionnaireConfig(
        id = "@{id}",
        title = "@{title}",
        setPractitionerDetails = false,
        setOrganizationDetails = false,
        setAppVersion = false,
        type = QuestionnaireType.EDIT,
        resourceIdentifier = "@{resourceIdentifier}",
        confirmationDialog =
          ConfirmationDialog(
            title = "@{dialogTitle}",
            message = "@{dialogMessage}",
            actionButtonText = "@{dialogActionButtonText}",
          ),
        groupResource =
          GroupResourceConfig(
            groupIdentifier = "@{groupIdentifier}",
            memberResourceType = "Condition",
            removeMember = true,
            removeGroup = true,
            deactivateMembers = false
          ),
        taskId = "@{taskId}",
        saveDraft = true,
        eventWorkflows = listOf(EventWorkflow()),
        planDefinitions = listOf("@{planDef1}"),
        readOnlyLinkIds = listOf("@{linkId1}", "@{linkId2}")
      )

    val map =
      mapOf(
        "id" to "123",
        "title" to "New Title",
        "taskId" to "333",
        "resourceIdentifier" to "999",
        "groupIdentifier" to "777",
        "dialogTitle" to "Alert",
        "dialogMessage" to "Are you sure?",
        "dialogActionButtonText" to "Yes",
        "planDef1" to "97c5f33b-389c-4ecb-abd3-46c5a3ac4026",
        "linkId1" to "1",
        "linkId2" to "2"
      )

    val interpolatedConfig = questionnaireConfig.interpolate(map)

    Assert.assertEquals("123", interpolatedConfig.id)
    Assert.assertEquals("New Title", interpolatedConfig.title)
    Assert.assertEquals("333", interpolatedConfig.taskId)
    Assert.assertEquals("999", interpolatedConfig.resourceIdentifier)
    Assert.assertEquals("777", interpolatedConfig.groupResource?.groupIdentifier)
    Assert.assertEquals("Alert", interpolatedConfig.confirmationDialog?.title)
    Assert.assertEquals("Are you sure?", interpolatedConfig.confirmationDialog?.message)
    Assert.assertEquals("Yes", interpolatedConfig.confirmationDialog?.actionButtonText)
    Assert.assertEquals(
      "97c5f33b-389c-4ecb-abd3-46c5a3ac4026",
      interpolatedConfig.planDefinitions?.firstOrNull()
    )
    Assert.assertEquals("1", interpolatedConfig.readOnlyLinkIds!![0])
    Assert.assertEquals("2", interpolatedConfig.readOnlyLinkIds!![1])
  }

  @Test
  fun testDefaultConfirmationDialog() {
    ConfirmationDialog().apply {
      Assert.assertEquals("", title)
      Assert.assertEquals("", message)
      Assert.assertEquals("", actionButtonText)
    }
  }

  @Test
  fun testSetConfirmationDialog() {
    val title = "Alert"
    val message = "Are you sure?"
    val actionButtonText = "Yes"
    ConfirmationDialog(title, message, actionButtonText).apply {
      Assert.assertEquals(title, this.title)
      Assert.assertEquals(message, this.message)
      Assert.assertEquals(actionButtonText, this.actionButtonText)
    }
  }

  @Test
  fun testDefaultGroupResourceConfig() {
    val groupIdentifier = "groupIdentifier"
    val memberResourceType = "Condition"
    val groupResourceConfig = GroupResourceConfig(groupIdentifier, memberResourceType)
    groupResourceConfig.apply {
      Assert.assertEquals(groupIdentifier, this.groupIdentifier)
      Assert.assertEquals(memberResourceType, this.memberResourceType)
      Assert.assertEquals(false, removeMember)
      Assert.assertEquals(false, removeGroup)
      Assert.assertEquals(true, deactivateMembers)
    }
  }
}
