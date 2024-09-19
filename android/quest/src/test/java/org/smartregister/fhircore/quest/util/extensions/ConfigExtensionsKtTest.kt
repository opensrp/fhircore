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

package org.smartregister.fhircore.quest.util.extensions

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.mutableStateMapOf
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItemConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler

@HiltAndroidTest
class ConfigExtensionsKtTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var registerRepository: RegisterRepository

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var parser: IParser

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private val navController = mockk<NavController>(relaxUnitFun = true, relaxed = true)
  private val context = mockk<Context>(relaxUnitFun = true, relaxed = true)
  private val navigationMenuConfig by lazy {
    NavigationMenuConfig(
      id = "id",
      display = "menu",
      visible = true,
      menuIconConfig =
        ImageConfig(
          type = ICON_TYPE_REMOTE,
          reference = "d60ff460-7671-466a-93f4-c93a2ebf2077",
        ),
    )
  }
  private val overflowMenuItemConfig by lazy {
    OverflowMenuItemConfig(
      visible = "true",
      icon =
        ImageConfig(
          type = ICON_TYPE_REMOTE,
          reference = "d60ff460-7671-466a-93f4-c93a2ebf2077",
        ),
    )
  }
  private val imageProperties =
    ImageProperties(
      imageConfig =
        ImageConfig(
          type = ICON_TYPE_REMOTE,
          reference = "d60ff460-7671-466a-93f4-c93a2ebf2077",
        ),
    )

  private val profileConfiguration =
    ProfileConfiguration(
      id = "1",
      appId = "a",
      fhirResource =
        FhirResourceConfig(
          baseResource = ResourceConfig(resource = ResourceType.Patient),
          relatedResources =
            listOf(
              ResourceConfig(
                resource = ResourceType.Encounter,
              ),
              ResourceConfig(
                resource = ResourceType.Task,
              ),
            ),
        ),
      views =
        listOf(
          CardViewProperties(
            viewType = ViewType.CARD,
            content =
              listOf(
                ListProperties(
                  viewType = ViewType.LIST,
                  registerCard =
                    RegisterCardConfig(
                      views =
                        listOf(
                          ColumnProperties(
                            viewType = ViewType.COLUMN,
                            children =
                              listOf(
                                RowProperties(
                                  viewType = ViewType.ROW,
                                  children =
                                    listOf(
                                      imageProperties,
                                    ),
                                ),
                              ),
                          ),
                        ),
                    ),
                ),
              ),
          ),
        ),
    )

  private val binaryImage = Faker.buildBinaryResource()
  private val patient = Faker.buildPatient()
  private val resourceData by lazy {
    ResourceData(
      baseResourceId = patient.logicalId,
      baseResourceType = ResourceType.Patient,
      computedValuesMap = mapOf("logicalId" to patient.id, "name" to patient.name),
    )
  }

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    every { navController.context } returns context
  }

  @Test
  fun testLaunchProfileActionOnClick() {
    val resourceConfig = FhirResourceConfig(ResourceConfig(resource = ResourceType.Patient))
    val clickAction =
      ActionConfig(
        id = "profileId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_PROFILE.name,
        resourceConfig = resourceConfig,
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle), null) }
    Assert.assertEquals(MainNavigationScreen.Profile.route, slotInt.captured)
    Assert.assertEquals(4, slotBundle.captured.size())
    Assert.assertEquals("profileId", slotBundle.captured.getString(NavigationArg.PROFILE_ID))
    Assert.assertEquals(patient.logicalId, slotBundle.captured.getString(NavigationArg.RESOURCE_ID))
    Assert.assertEquals(
      resourceConfig,
      slotBundle.captured.getParcelable(NavigationArg.RESOURCE_CONFIG),
    )
  }

  @Test
  fun testLaunchProfileActionOnClickWhenPopBackStackIsTrue() {
    val clickAction =
      ActionConfig(
        id = "profileId",
        trigger = ActionTrigger.ON_QUESTIONNAIRE_SUBMISSION,
        workflow = ApplicationWorkflow.LAUNCH_PROFILE.name,
        popNavigationBackStack = true,
      )
    every { navController.currentDestination } returns
      NavDestination(navigatorName = "navigating").apply { id = 2384 }
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    val slotNavOptions = slot<NavOptions>()
    verify {
      navController.navigate(capture(slotInt), capture(slotBundle), capture(slotNavOptions))
    }
    Assert.assertEquals(MainNavigationScreen.Profile.route, slotInt.captured)
    Assert.assertTrue(slotNavOptions.captured.isPopUpToInclusive())
    Assert.assertEquals(4, slotBundle.captured.size())
    Assert.assertEquals("profileId", slotBundle.captured.getString(NavigationArg.PROFILE_ID))
  }

  @Test
  fun testLaunchProfileActionOnClickWhenPopBackStackIsFalse() {
    val clickAction =
      ActionConfig(
        id = "profileId",
        trigger = ActionTrigger.ON_QUESTIONNAIRE_SUBMISSION,
        workflow = ApplicationWorkflow.LAUNCH_PROFILE.name,
        popNavigationBackStack = false,
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle), null) }
    Assert.assertEquals(MainNavigationScreen.Profile.route, slotInt.captured)
    Assert.assertEquals(4, slotBundle.captured.size())
    Assert.assertEquals("profileId", slotBundle.captured.getString(NavigationArg.PROFILE_ID))
  }

  @Test
  fun testLaunchProfileActionOnClickWhenPopBackStackIsNull() {
    val clickAction =
      ActionConfig(
        id = "profileId",
        trigger = ActionTrigger.ON_QUESTIONNAIRE_SUBMISSION,
        workflow = ApplicationWorkflow.LAUNCH_PROFILE.name,
        popNavigationBackStack = null,
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle), null) }
    Assert.assertEquals(MainNavigationScreen.Profile.route, slotInt.captured)
    Assert.assertEquals(4, slotBundle.captured.size())
    Assert.assertEquals("profileId", slotBundle.captured.getString(NavigationArg.PROFILE_ID))
  }

  @Test
  fun testLaunchProfileWithConfiguredResourceIdActionOnClick() {
    val resourceConfig = FhirResourceConfig(ResourceConfig(resource = ResourceType.Patient))
    val params =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.RESOURCE_ID,
          key = "patientId",
          value = "configured-patient-id",
        ),
      )
    val clickAction =
      ActionConfig(
        id = "profileId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_PROFILE.name,
        resourceConfig = resourceConfig,
        params = params,
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle), null) }
    Assert.assertEquals(MainNavigationScreen.Profile.route, slotInt.captured)
    Assert.assertEquals(4, slotBundle.captured.size())
    Assert.assertEquals("profileId", slotBundle.captured.getString(NavigationArg.PROFILE_ID))
    Assert.assertEquals(
      "configured-patient-id",
      slotBundle.captured.getString(NavigationArg.RESOURCE_ID),
    )
    Assert.assertEquals(
      resourceConfig,
      slotBundle.captured.getParcelable(NavigationArg.RESOURCE_CONFIG),
    )
  }

  @Test
  fun testLaunchRegisterActionOnClick() {
    val clickAction =
      ActionConfig(
        id = "registerId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_REGISTER.name,
        display = "menu",
        toolBarHomeNavigation = ToolBarHomeNavigation.NAVIGATE_BACK,
      )
    every { navController.currentDestination } returns NavDestination(navigatorName = "navigating")
    every { navController.previousBackStackEntry } returns null
    every { navController.currentBackStackEntry } returns null
    every { navController.graph.id } returns 1
    listOf(clickAction)
      .handleClickEvent(
        navController = navController,
        resourceData = resourceData,
        navMenu = navigationMenuConfig,
      )
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    val navOptions = slot<NavOptions>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle), capture(navOptions)) }
    Assert.assertEquals(MainNavigationScreen.Home.route, slotInt.captured)
    Assert.assertNotNull(slotBundle.captured.size())
    Assert.assertEquals("registerId", slotBundle.captured.getString(NavigationArg.REGISTER_ID))
    Assert.assertEquals("menu", slotBundle.captured.getString(NavigationArg.SCREEN_TITLE))
    Assert.assertEquals(
      ToolBarHomeNavigation.NAVIGATE_BACK,
      slotBundle.captured.getSerializable(NavigationArg.TOOL_BAR_HOME_NAVIGATION),
    )
    Assert.assertFalse(navOptions.captured.isPopUpToInclusive())
    Assert.assertTrue(navOptions.captured.shouldLaunchSingleTop())
  }

  @Test
  fun testLaunchSettingsActionOnClick() {
    val clickAction =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_SETTINGS.name,
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    verify { navController.navigate(capture(slotInt)) }
    Assert.assertEquals(MainNavigationScreen.Settings.route, slotInt.captured)
  }

  @Test
  fun testLaunchReportActionOnClick() {
    val clickAction =
      ActionConfig(
        id = "reportId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_REPORT.name,
        params =
          listOf(
            ActionParameter(
              value = "practitioner_id",
              key = "practitionerId",
              paramType = ActionParameterType.RESOURCE_ID,
            ),
          ),
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    verify { navController.navigate(capture(slotInt), capture(slotBundle)) }
    Assert.assertEquals(MainNavigationScreen.Reports.route, slotInt.captured)
    Assert.assertEquals(2, slotBundle.captured.size())
    Assert.assertEquals("reportId", slotBundle.captured.getString(NavigationArg.REPORT_ID))
    Assert.assertEquals("practitioner_id", slotBundle.captured.getString(NavigationArg.RESOURCE_ID))
  }

  @Test
  fun testLaunchGeoWidgetMapActionOnClick() {
    val clickAction =
      ActionConfig(
        id = "geoWidgetId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_MAP.name,
      )
    listOf(clickAction).handleClickEvent(navController = navController, resourceData = resourceData)
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    val slotNavOptions = slot<NavOptions>()
    verify {
      navController.navigate(capture(slotInt), capture(slotBundle), capture(slotNavOptions))
    }
    Assert.assertEquals(MainNavigationScreen.GeoWidgetLauncher.route, slotInt.captured)
    verify {
      navController.navigate(capture(slotInt), capture(slotBundle), capture(slotNavOptions))
    }
    Assert.assertEquals(1, slotBundle.captured.size())
    Assert.assertEquals("geoWidgetId", slotBundle.captured.getString(NavigationArg.GEO_WIDGET_ID))
  }

  @Test
  fun testLaunchDiallerOnClick() {
    val patientWithPhoneNumber = patient.copy()
    patientWithPhoneNumber.apply {
      addTelecom(
        ContactPoint().apply { this.value = "0700000000" },
      )
    }

    val computedValuesWithPhoneNumberMutable = resourceData.computedValuesMap.toMutableMap()
    computedValuesWithPhoneNumberMutable["patientPhoneNumber"] =
      patientWithPhoneNumber.telecom.first().value
    val computedValuesWithPhoneNumber = computedValuesWithPhoneNumberMutable.toMap()

    val resourceDataWithPhoneNumber =
      ResourceData(
        baseResourceId = patient.logicalId,
        baseResourceType = ResourceType.Patient,
        computedValuesMap = computedValuesWithPhoneNumber,
      )

    val clickAction =
      ActionConfig(
        id = "diallerId",
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_DIALLER.name,
        params =
          listOf(
            ActionParameter(
              key = "patientPhoneNumber",
              value = "@{patientPhoneNumber}",
              paramType = ActionParameterType.PARAMDATA,
            ),
          ),
      )

    listOf(clickAction)
      .handleClickEvent(
        navController = navController,
        resourceData = resourceDataWithPhoneNumber,
      ) // make a clicking action

    // make sure no errors thrown when the new activity is started. should return nothing
    every { context.startActivity(any()) } returns Unit

    // make sure correct function with correct signature is called
    verify {
      context.startActivity(
        withArg {
          Assert.assertEquals(it.action, Intent.ACTION_DIAL)
          Assert.assertEquals(it.data, Uri.parse("tel:0700000000"))
        },
        null,
      )
    }
  }

  @Test
  fun testNavigateBackToHomeWhenCurrentAndPreviousDestinationIdsAreNull() {
    val clickAction =
      ActionConfig(
        id = null,
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_REGISTER.name,
        display = null,
        toolBarHomeNavigation = ToolBarHomeNavigation.NAVIGATE_BACK,
      )
    val slotInt = slot<Int>()
    val slotBundle = slot<Bundle>()
    val navOptions = slot<NavOptions>()
    every { navController.currentDestination } returns null
    every { navController.previousBackStackEntry } returns null
    every { navController.currentBackStackEntry } returns null
    listOf(clickAction)
      .handleClickEvent(
        navController = navController,
        resourceData = resourceData,
        navMenu = navigationMenuConfig,
      )
    verify(exactly = 0) {
      navController.navigate(capture(slotInt), capture(slotBundle), capture(navOptions))
    }
  }

  @Test
  fun testDeviceToDeviceSyncActionOnClick() {
    val clickAction =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.DEVICE_TO_DEVICE_SYNC.name,
      )
    listOf(clickAction).handleClickEvent(navController, resourceData)
    verify { context.startActivity(any()) }
  }

  @Test
  fun testLaunchQuestionnaireActionOnClick() {
    val context =
      mockk<Context>(
        moreInterfaces = arrayOf(QuestionnaireHandler::class),
        relaxUnitFun = true,
        relaxed = true,
      )
    val navController = NavController(context)
    val clickAction =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.LAUNCH_QUESTIONNAIRE.name,
        questionnaire = QuestionnaireConfig(id = "qid", title = "Form"),
      )
    listOf(clickAction).handleClickEvent(navController, resourceData)
    verify {
      (navController.context as QuestionnaireHandler).launchQuestionnaire(
        context = any(),
        extraIntentBundle = any(),
        questionnaireConfig = any(),
        actionParams = emptyList(),
      )
    }
  }

  fun testInterpolateValueWithANonNullComputedValuesMapReturnsValues() {
    val actionConfig =
      ActionConfig(
        ActionTrigger.ON_CLICK,
        ApplicationWorkflow.LAUNCH_PROFILE.name,
        params =
          listOf(
            ActionParameter(
              key = "param1",
              value = "@{practitionerId-1}",
              paramType = ActionParameterType.PARAMDATA,
            ),
            ActionParameter(
              key = "param2",
              value = "@{practitionerId-2}",
              paramType = ActionParameterType.PARAMDATA,
            ),
            ActionParameter(
              key = "param3",
              value = "@{practitionerId-3}",
              paramType = ActionParameterType.PARAMDATA,
            ),
            ActionParameter(
              key = "param4",
              value = "@{practitionerId-4}",
              paramType = ActionParameterType.PARAMDATA,
            ),
          ),
      )
    val resourceData =
      ResourceData(
        baseResourceId = "testResourceId",
        ResourceType.CarePlan,
        computedValuesMap =
          mapOf(
            "practitionerId-1" to "1234",
            "practitionerId-2" to "1235",
            "practitionerId-3" to "1236",
            "practitionerId-4" to "1237",
          ),
      )
    val resultOfInterpolatedValues = interpolateActionParamsValue(actionConfig, resourceData)
    Assert.assertEquals(4, resultOfInterpolatedValues.size)
    Assert.assertEquals("param2", resultOfInterpolatedValues[1].key)
    Assert.assertEquals("1235", resultOfInterpolatedValues[1].value)
  }

  @Test
  fun testInterpolateValueWithNullComputedValuesMapReturnsEmptyArray() {
    val actionConfig =
      ActionConfig(
        ActionTrigger.ON_CLICK,
        ApplicationWorkflow.LAUNCH_PROFILE.name,
        params =
          listOf(
            ActionParameter(
              key = "param1",
              value = "@{practitionerId-1}",
              paramType = ActionParameterType.PARAMDATA,
            ),
            ActionParameter(
              key = "param2",
              value = "@{practitionerId-2}",
              paramType = ActionParameterType.PARAMDATA,
            ),
            ActionParameter(
              key = "param3",
              value = "@{practitionerId-3}",
              paramType = ActionParameterType.PARAMDATA,
            ),
            ActionParameter(
              key = "param4",
              value = "@{practitionerId-4}",
              paramType = ActionParameterType.PARAMDATA,
            ),
          ),
      )
    val resourceData =
      ResourceData(baseResourceId = "test", ResourceType.Task, computedValuesMap = emptyMap())
    val resultOfInterpolatedValues = interpolateActionParamsValue(actionConfig, resourceData)
    Assert.assertEquals("@{practitionerId-4}", resultOfInterpolatedValues[3].value)
  }

  fun testConvertActionParameterArrayToMapShouldReturnEmptyMapIfNoParamData() {
    val array = arrayOf(ActionParameter(key = "k", value = "v"))
    Assert.assertEquals(emptyMap<String, String>(), array.toParamDataMap())
  }

  @Test
  fun testConvertActionParameterArrayToMapShouldReturnEmtpyMapIfArrayIsEmpty() {
    val array = emptyArray<ActionParameter>()
    Assert.assertEquals(emptyMap<String, String>(), array.toParamDataMap())
  }

  @Test
  fun testConvertActionParameterArrayToMapShouldReturnEmtpyMapValue() {
    val array =
      arrayOf(ActionParameter(key = "k", value = "", paramType = ActionParameterType.PARAMDATA))
    Assert.assertEquals("", array.toParamDataMap()["k"])
  }

  @Test
  fun testConvertActionParameterArrayToMapShouldReturnMapIfParamData() {
    val array =
      arrayOf(ActionParameter(key = "k", value = "v", paramType = ActionParameterType.PARAMDATA))
    Assert.assertEquals(mapOf("k" to "v"), array.toParamDataMap())
  }

  @Test
  fun testShowToastWhenAnImageWithActionParamsIsPressed() {
    val context = mockk<Context>(relaxed = true)
    val navController = NavController(context)
    val mockClipboardManager = mockk<ClipboardManager>()
    val clickAction =
      ActionConfig(
        trigger = ActionTrigger.ON_CLICK,
        workflow = ApplicationWorkflow.COPY_TEXT.name,
        params =
          listOf(
            ActionParameter(
              key = "copyText",
              paramType = ActionParameterType.PARAMDATA,
              value = "https://my-url",
            ),
          ),
      )
    val text = "Link ${clickAction.params.first().value} copied successfully"
    every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns mockClipboardManager
    every {
      context.getString(R.string.copy_text_success_message, clickAction.params.first().value)
    } returns text
    every { mockClipboardManager.setPrimaryClip(any()) } returns Unit
    listOf(clickAction).handleClickEvent(navController, resourceData, context = context)
    verify { context.showToast(text, Toast.LENGTH_LONG) }
  }

  @Test
  fun decodeBinaryResourcesToBitmapOnNavigationMenuClientRegistersDoneCorrectly(): Unit =
    runBlocking {
      val navigationMenuConfigs =
        sequenceOf(navigationMenuConfig).mapNotNull { it.menuIconConfig?.reference }
      val decodedImageMap = mutableStateMapOf<String, Bitmap>()
      withContext(dispatcherProvider.io()) {
        defaultRepository.create(addResourceTags = true, binaryImage)
        navigationMenuConfigs.resourceReferenceToBitMap(
          fhirEngine = fhirEngine,
          decodedImageMap = decodedImageMap,
        )
      }
      Assert.assertTrue(decodedImageMap.isNotEmpty())
      Assert.assertTrue(decodedImageMap.containsKey("d60ff460-7671-466a-93f4-c93a2ebf2077"))
    }

  @Test
  fun decodeBinaryResourcesToBitmapOnOverflowMenuConfigDoneCorrectly(): Unit = runTest {
    val navigationMenuConfigs = sequenceOf(overflowMenuItemConfig).mapNotNull { it.icon?.reference }
    val decodedImageMap = mutableStateMapOf<String, Bitmap>()
    withContext(Dispatchers.IO) {
      defaultRepository.create(addResourceTags = true, binaryImage)
      navigationMenuConfigs.resourceReferenceToBitMap(
        fhirEngine = fhirEngine,
        decodedImageMap = decodedImageMap,
      )
    }
    Assert.assertTrue(decodedImageMap.isNotEmpty())
    Assert.assertTrue(decodedImageMap.containsKey("d60ff460-7671-466a-93f4-c93a2ebf2077"))
  }

  @Test
  fun testImageBitmapUpdatedCorrectlyGivenProfileConfiguration(): Unit = runTest {
    val decodedImageMap = mutableStateMapOf<String, Bitmap>()
    withContext(Dispatchers.IO) {
      fhirEngine.create(binaryImage)
      profileConfiguration.views.decodeImageResourcesToBitmap(fhirEngine, decodedImageMap)
    }

    Assert.assertTrue(decodedImageMap.isNotEmpty())
    Assert.assertTrue(decodedImageMap.containsKey("d60ff460-7671-466a-93f4-c93a2ebf2077"))
  }

  @Test
  fun testImageBitmapUpdatedCorrectlyGivenCardViewProperties(): Unit = runTest {
    val cardViewProperties = profileConfiguration.views[0] as CardViewProperties
    val decodedImageMap = mutableStateMapOf<String, Bitmap>()
    withContext(Dispatchers.IO) {
      defaultRepository.create(addResourceTags = true, binaryImage)
      listOf(cardViewProperties).decodeImageResourcesToBitmap(fhirEngine, decodedImageMap)
    }
    Assert.assertTrue(decodedImageMap.containsKey("d60ff460-7671-466a-93f4-c93a2ebf2077"))
    Assert.assertTrue(decodedImageMap.isNotEmpty())
  }

  @Test
  fun testImageBitmapUpdatedCorrectlyGivenListViewProperties(): Unit = runTest {
    val cardViewProperties = profileConfiguration.views[0] as CardViewProperties
    val decodedImageMap = mutableStateMapOf<String, Bitmap>()
    withContext(Dispatchers.IO) {
      defaultRepository.create(addResourceTags = true, binaryImage)
      listOf(cardViewProperties.content[0])
        .decodeImageResourcesToBitmap(fhirEngine, decodedImageMap)
    }
    Assert.assertTrue(decodedImageMap.containsKey("d60ff460-7671-466a-93f4-c93a2ebf2077"))
    Assert.assertTrue(decodedImageMap.isNotEmpty())
  }

  @Test
  fun testImageBitmapUpdatedCorrectlyGivenColumnProperties(): Unit = runTest {
    val cardViewProperties = profileConfiguration.views[0] as CardViewProperties
    val listViewProperties = cardViewProperties.content[0] as ListProperties
    val decodedImageMap = mutableStateMapOf<String, Bitmap>()
    withContext(Dispatchers.IO) {
      defaultRepository.create(addResourceTags = true, binaryImage)
      listOf(listViewProperties.registerCard.views[0])
        .decodeImageResourcesToBitmap(fhirEngine, decodedImageMap)
    }
    Assert.assertTrue(decodedImageMap.containsKey("d60ff460-7671-466a-93f4-c93a2ebf2077"))
    Assert.assertTrue(decodedImageMap.isNotEmpty())
  }

  @Test
  fun testImageBitmapUpdatedCorrectlyGivenRowProperties(): Unit = runTest {
    val cardViewProperties = profileConfiguration.views[0] as CardViewProperties
    val listViewProperties = cardViewProperties.content[0] as ListProperties
    val columnProperties = listViewProperties.registerCard.views[0] as ColumnProperties
    val decodedImageMap = mutableStateMapOf<String, Bitmap>()
    withContext(Dispatchers.IO) {
      defaultRepository.create(addResourceTags = true, binaryImage)
      listOf(columnProperties.children[0]).decodeImageResourcesToBitmap(fhirEngine, decodedImageMap)
    }
    Assert.assertTrue(decodedImageMap.containsKey("d60ff460-7671-466a-93f4-c93a2ebf2077"))
    Assert.assertTrue(decodedImageMap.isNotEmpty())
  }

  @Test
  fun testImageMapNotUpdatedWhenReferenceIsNull() = runTest {
    val cardViewProperties = profileConfiguration.views[0] as CardViewProperties
    val listViewProperties = cardViewProperties.content[0] as ListProperties
    val columnProperties = listViewProperties.registerCard.views[0] as ColumnProperties
    val rowProperties =
      (columnProperties.children[0] as RowProperties).copy(
        children =
          listOf(
            ImageProperties(
              imageConfig =
                ImageConfig(
                  type = ICON_TYPE_REMOTE,
                  reference = null,
                ),
            ),
          ),
      )
    val decodedImageMap = mutableStateMapOf<String, Bitmap>()
    withContext(Dispatchers.IO) {
      listOf(rowProperties).decodeImageResourcesToBitmap(fhirEngine, decodedImageMap)
    }
    Assert.assertTrue(decodedImageMap.isEmpty())
    Assert.assertTrue(!decodedImageMap.containsKey("d60ff460-7671-466a-93f4-c93a2ebf2077"))
  }

  fun testExceptionCaughtOnDecodingBitmap() = runTest {
    val cardViewProperties = profileConfiguration.views[0] as CardViewProperties
    val listViewProperties = cardViewProperties.content[0] as ListProperties
    val columnProperties = listViewProperties.registerCard.views[0] as ColumnProperties
    val rowProperties =
      (columnProperties.children[0] as RowProperties).copy(
        children =
          listOf(
            ImageProperties(
              imageConfig =
                ImageConfig(
                  type = ICON_TYPE_REMOTE,
                  reference = "null Reference",
                ),
            ),
          ),
      )
    val decodedImageMap = mutableStateMapOf<String, Bitmap>()

    coEvery { defaultRepository.loadResource<Binary>(anyString()) } returns
      Binary().apply {
        this.id = "null Reference"
        this.contentType = "image/jpeg"
        this.data = "gibberish value".toByteArray()
      }

    withContext(Dispatchers.IO) {
      listOf(rowProperties).decodeImageResourcesToBitmap(fhirEngine, decodedImageMap)
    }
    Assert.assertTrue(decodedImageMap.isEmpty())
    Assert.assertFalse(decodedImageMap.containsKey("null Reference"))
  }
}
