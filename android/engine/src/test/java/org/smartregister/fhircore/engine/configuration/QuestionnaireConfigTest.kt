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

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class QuestionnaireConfigTest : RobolectricTest() {

  @Test
  fun testVerifyInterpolationInQuestionnaireConfig() {

    val questionnaireConfig =
      QuestionnaireConfig(
        id = "@{id}",
        title = "@{title}",
        resourceIdentifier = "@{resourceIdentifier}",
        confirmationDialog =
          ConfirmationDialog(
            title = "@{dialogTitle}",
            message = "@{dialogMessage}",
            actionButtonText = "@{dialogActionButtonText}"
          ),
        groupResource =
          GroupResourceConfig(
            groupIdentifier = "@{groupIdentifier}",
            memberResourceType = "Condition",
          ),
        taskId = "@{taskId}"
      )

    val map = mutableMapOf<String, String>()
    map["id"] = "123"
    map["title"] = "New Title"
    map["taskId"] = "333"
    map["resourceIdentifier"] = "999"
    map["groupIdentifier"] = "777"
    map["dialogTitle"] = "Alert"
    map["dialogMessage"] = "Are you sure?"
    map["dialogActionButtonText"] = "Yes"

    val interpolatedConfig = questionnaireConfig.interpolate(map)

    Assert.assertEquals("123", interpolatedConfig.id)
    Assert.assertEquals("New Title", interpolatedConfig.title)
    Assert.assertEquals("333", interpolatedConfig.taskId)
    Assert.assertEquals("999", interpolatedConfig.resourceIdentifier)
    Assert.assertEquals("777", interpolatedConfig.groupResource?.groupIdentifier)
    Assert.assertEquals("Alert", interpolatedConfig.confirmationDialog?.title)
    Assert.assertEquals("Are you sure?", interpolatedConfig.confirmationDialog?.message)
    Assert.assertEquals("Yes", interpolatedConfig.confirmationDialog?.actionButtonText)
  }
}
