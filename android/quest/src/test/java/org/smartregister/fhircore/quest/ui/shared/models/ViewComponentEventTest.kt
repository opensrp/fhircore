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

package org.smartregister.fhircore.quest.ui.shared.models

import android.content.Context
import android.os.Bundle
import androidx.navigation.NavHostController
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class ViewComponentEventTest : RobolectricTest() {

  private lateinit var viewComponentEvent: ViewComponentEvent

  private val navController = mockk<NavHostController>()

  @Test
  fun testHandleEventShouldOpenProfile() {
    viewComponentEvent = ViewComponentEvent.OpenProfile("1234", "res123")
    val urlPathSlot = slot<Int>()
    val bundleSlot = slot<Bundle>()
    every { navController.navigate(capture(urlPathSlot), capture(bundleSlot)) } just runs

    viewComponentEvent.handleEvent(navController)

    Assert.assertEquals(R.id.profileFragment, urlPathSlot.captured)
    val bundle = bundleSlot.captured
    val profileIdKey = "profileId"
    val resourceIdKey = "resourceId"
    Assert.assertTrue(bundle.containsKey(profileIdKey))
    Assert.assertTrue(bundle.containsKey(resourceIdKey))
    Assert.assertEquals("1234", bundle.getString(profileIdKey))
    Assert.assertEquals("res123", bundle.getString(resourceIdKey))
  }

  @Test
  @Ignore("Fix java.lang.NullPointerException")
  fun testHandleEventShouldLaunchQuestionnaire() {
    viewComponentEvent =
      ViewComponentEvent.LaunchQuestionnaire(
        ActionConfig(
          trigger = ActionTrigger.ON_CLICK,
          questionnaire = QuestionnaireConfig(id = "q123", type = QuestionnaireType.DEFAULT),
        ),
        resourceData = ResourceData(baseResource = Patient().apply { id = "p123" })
      )
    val questionnaireIdSlot = slot<String>()
    val clientIdentifierSlot = slot<String>()
    val questionnaireTypeSlot = slot<QuestionnaireType>()
    val intentBundleSlot = slot<Bundle>()

    val mockContext = mockk<Context>(relaxed = true, relaxUnitFun = true)
    every { navController.context } returns mockContext
    every { mockContext.startActivity(any()) } just runs
    every {
      navController.context.launchQuestionnaire<QuestionnaireActivity>(
        questionnaireId = capture(questionnaireIdSlot),
        clientIdentifier = capture(clientIdentifierSlot),
        questionnaireType = capture(questionnaireTypeSlot),
        intentBundle = capture(intentBundleSlot)
      )
    } just runs

    viewComponentEvent.handleEvent(navController)

    val captured = questionnaireIdSlot.captured
    Assert.assertEquals("q123", captured)
  }
}
