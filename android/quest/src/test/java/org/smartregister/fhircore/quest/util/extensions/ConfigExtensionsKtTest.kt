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

package org.smartregister.fhircore.quest.util.extensions

import android.content.Context
import android.os.Bundle
import androidx.navigation.NavController
import com.google.android.fhir.logicalId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity

class ConfigExtensionsKtTest : RobolectricTest() {

  private val navController = mockk<NavController>(relaxUnitFun = true)

  private val context = mockk<Context>(relaxUnitFun = true, relaxed = true)

  private val navigationMenuConfig by lazy {
    NavigationMenuConfig(id = "id", display = "menu", visible = true)
  }

  private val patient = Faker.buildPatient()

  private val resourceData by lazy {
    ResourceData(
      baseResource = patient,
      computedValuesMap = mapOf("logicalId" to patient.id, "name" to patient.name)
    )
  }

  @Before
  fun setUp() {
    every { navController.context } returns context
  }

  @Test
  fun testLaunchProfileActionOnClick() {
    val resourceConfig = FhirResourceConfig(ResourceConfig(resource = "Patient"))
    val clickAction =
      ActionConfig(
        id = "profileId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_PROFILE,
        resourceConfig = resourceConfig
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle)) }
    Assert.assertEquals(MainNavigationScreen.Profile.route, slotInt.captured)
    Assert.assertEquals(3, slotBundle.captured.size())
    Assert.assertEquals("profileId", slotBundle.captured.getString(NavigationArg.PROFILE_ID))
    Assert.assertEquals(patient.logicalId, slotBundle.captured.getString(NavigationArg.RESOURCE_ID))
    Assert.assertEquals(
      resourceConfig,
      slotBundle.captured.getParcelable(NavigationArg.RESOURCE_CONFIG)
    )
  }

  @Test
  fun testLaunchRegisterActionOnClick() {
    val clickAction =
      ActionConfig(
        id = "registerId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_REGISTER
      )
    listOf(clickAction)
      .handleClickEvent(
        navController = navController,
        resourceData = resourceData,
        navMenu = navigationMenuConfig
      )
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle)) }
    Assert.assertEquals(MainNavigationScreen.Home.route, slotInt.captured)
    Assert.assertEquals(2, slotBundle.captured.size())
    Assert.assertEquals("id", slotBundle.captured.getString(NavigationArg.REGISTER_ID))
    Assert.assertEquals("menu", slotBundle.captured.getString(NavigationArg.SCREEN_TITLE))
  }

  @Test
  fun testLaunchSettingsActionOnClick() {
    val clickAction =
      ActionConfig(trigger = ActionTrigger.ON_CLICK, workflow = ApplicationWorkflow.LAUNCH_SETTINGS)
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    verify { navController.navigate(capture(slotInt)) }
    Assert.assertEquals(MainNavigationScreen.Settings.route, slotInt.captured)
  }

  @Test
  fun testLaunchReportActionOnClick() {
    val clickAction =
      ActionConfig(trigger = ActionTrigger.ON_CLICK, workflow = ApplicationWorkflow.LAUNCH_REPORT)
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    verify { navController.navigate(capture(slotInt)) }
    Assert.assertEquals(MainNavigationScreen.Reports.route, slotInt.captured)
  }

  @Test
  fun testLaunchGeoWidgetMapActionOnClick() {
    val clickAction =
      ActionConfig(
        id = "geoWidgetId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_MAP
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle)) }
    Assert.assertEquals(MainNavigationScreen.GeoWidget.route, slotInt.captured)
    verify { navController.navigate(capture(slotInt), capture(slotBundle)) }
    Assert.assertEquals(1, slotBundle.captured.size())
    Assert.assertEquals("geoWidgetId", slotBundle.captured.getString(NavigationArg.CONFIG_ID))
  }

  @Test
  fun testDeviceToDeviceSyncActionOnClick() {
    val clickAction =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.DEVICE_TO_DEVICE_SYNC
      )
    listOf(clickAction).handleClickEvent(navController, resourceData)
    verify { context.startActivity(any()) }
  }

  @Test
  @Ignore("Fix java.lang.NullPointerException")
  fun testLaunchQuestionnaireActionOnClick() {
    val clickAction =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE,
        questionnaire = QuestionnaireConfig(id = "qid", title = "Form")
      )
    listOf(clickAction).handleClickEvent(navController, resourceData)
    verify { context.launchQuestionnaire<QuestionnaireActivity>(any(), any(), any()) }
  }
}
