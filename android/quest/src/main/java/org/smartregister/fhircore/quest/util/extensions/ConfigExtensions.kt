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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Binary
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.StackViewProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItemConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeToBitmap
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.pdf.PdfLauncherFragment
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.p2p.utils.startP2PScreen
import timber.log.Timber

const val PRACTITIONER_ID = "practitionerId"

fun List<ActionConfig>.handleClickEvent(
  navController: NavController,
  resourceData: ResourceData? = null,
  navMenu: NavigationMenuConfig? = null,
  context: Context? = null,
) {
  val onClickAction =
    this.find {
      it.trigger.isIn(
        ActionTrigger.ON_SEARCH_SINGLE_RESULT,
        ActionTrigger.ON_CLICK,
        ActionTrigger.ON_QUESTIONNAIRE_SUBMISSION,
      )
    }

  onClickAction?.handleClickEvent(navController, resourceData, navMenu, context)
}

fun ActionConfig.handleClickEvent(
  navController: NavController,
  resourceData: ResourceData? = null,
  navMenu: NavigationMenuConfig? = null,
  context: Context? = null,
) {
  val computedValuesMap = resourceData?.computedValuesMap ?: emptyMap()
  val actionConfig = interpolate(computedValuesMap)
  val interpolatedParams = interpolateActionParamsValue(actionConfig, resourceData)
  val practitionerId =
    interpolatedParams
      .find { it.paramType == ActionParameterType.RESOURCE_ID && it.key == PRACTITIONER_ID }
      ?.value
  val resourceId =
    interpolatedParams.find { it.paramType == ActionParameterType.RESOURCE_ID }?.value
      ?: resourceData?.baseResourceId
  when (actionConfig.workflow?.let { ApplicationWorkflow.valueOf(it) }) {
    ApplicationWorkflow.LAUNCH_QUESTIONNAIRE -> {
      actionConfig.questionnaire?.let { questionnaireConfig ->
        val questionnaireConfigInterpolated = questionnaireConfig.interpolate(computedValuesMap)

        // Questionnaire is NOT launched via navigation component. It is started for result.
        if (navController.context is QuestionnaireHandler) {
          (navController.context as QuestionnaireHandler).launchQuestionnaire(
            context = navController.context,
            questionnaireConfig = questionnaireConfigInterpolated,
            actionParams = interpolatedParams,
          )
        }
      }
    }
    ApplicationWorkflow.LAUNCH_PROFILE -> {
      actionConfig.id?.let { id ->
        val args =
          bundleOf(
            NavigationArg.PROFILE_ID to id,
            NavigationArg.RESOURCE_ID to resourceId,
            NavigationArg.RESOURCE_CONFIG to actionConfig.resourceConfig,
            NavigationArg.PARAMS to interpolatedParams.toTypedArray(),
          )
        val navOptions =
          when (actionConfig.popNavigationBackStack) {
            false,
            null, -> null
            true ->
              navController.currentDestination?.id?.let { currentDestId ->
                navOptions(resId = currentDestId, inclusive = true)
              }
          }
        navController.navigate(
          resId = MainNavigationScreen.Profile.route,
          args = args,
          navOptions = navOptions,
        )
      }
    }
    ApplicationWorkflow.LAUNCH_REGISTER -> {
      val args =
        bundleOf(
          Pair(NavigationArg.REGISTER_ID, actionConfig.id ?: navMenu?.id),
          Pair(NavigationArg.SCREEN_TITLE, actionConfig.display ?: navMenu?.display ?: ""),
          Pair(NavigationArg.TOOL_BAR_HOME_NAVIGATION, actionConfig.toolBarHomeNavigation),
          Pair(NavigationArg.PARAMS, interpolatedParams.toTypedArray()),
        )

      // If value != null, we are navigating FROM a register; disallow same register navigation
      val currentRegisterId =
        navController.currentBackStackEntry?.arguments?.getString(NavigationArg.REGISTER_ID)
      val sameRegisterNavigation =
        args.getString(NavigationArg.REGISTER_ID) ==
          navController.previousBackStackEntry?.arguments?.getString(NavigationArg.REGISTER_ID)

      if (!currentRegisterId.isNullOrEmpty() && sameRegisterNavigation) {
        return
      } else {
        navController.navigate(
          resId = MainNavigationScreen.Home.route,
          args = args,
          navOptions =
            navController.currentDestination?.id?.let {
              navOptions(resId = it, inclusive = actionConfig.popNavigationBackStack == true)
            },
        )
      }
    }
    ApplicationWorkflow.LAUNCH_REPORT -> {
      val args =
        bundleOf(
          Pair(NavigationArg.REPORT_ID, actionConfig.id),
          Pair(NavigationArg.RESOURCE_ID, practitionerId?.extractLogicalIdUuid() ?: ""),
        )

      navController.navigate(MainNavigationScreen.Reports.route, args)
    }
    ApplicationWorkflow.LAUNCH_SETTINGS ->
      navController.navigate(MainNavigationScreen.Settings.route)
    ApplicationWorkflow.LAUNCH_INSIGHT_SCREEN ->
      navController.navigate(MainNavigationScreen.Insight.route)
    ApplicationWorkflow.DEVICE_TO_DEVICE_SYNC -> startP2PScreen(navController.context)
    ApplicationWorkflow.LAUNCH_MAP -> {
      val mapFragmentDestination = MainNavigationScreen.GeoWidgetLauncher.route

      val isMapFragmentExists = navController.currentDestination?.id == mapFragmentDestination
      if (isMapFragmentExists) {
        navController.popBackStack(mapFragmentDestination, false)
      } else {
        navController.navigate(
          resId = mapFragmentDestination,
          args = bundleOf(NavigationArg.GEO_WIDGET_ID to actionConfig.id),
          navOptions = navOptions(mapFragmentDestination, inclusive = true, singleOnTop = true),
        )
      }
    }
    ApplicationWorkflow.LAUNCH_DIALLER -> {
      val actionParameter = interpolatedParams.first()
      val phoneNumber = actionParameter.value
      val intent = Intent(Intent.ACTION_DIAL)
      intent.data = Uri.parse("tel:$phoneNumber")
      ContextCompat.startActivity(navController.context, intent, null)
    }
    ApplicationWorkflow.COPY_TEXT -> {
      val copyTextActionParameter = interpolatedParams.first()
      val clipboardManager =
        context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clipData = ClipData.newPlainText(null, copyTextActionParameter.value)
      clipboardManager.setPrimaryClip(clipData)
      context.showToast(
        context.getString(R.string.copy_text_success_message, copyTextActionParameter.value),
        Toast.LENGTH_LONG,
      )
    }
    ApplicationWorkflow.LAUNCH_LOCATION_SELECTOR -> {
      val args =
        bundleOf(
          NavigationArg.SCREEN_TITLE to (actionConfig.display ?: navMenu?.display ?: ""),
          NavigationArg.MULTI_SELECT_VIEW_CONFIG to actionConfig.multiSelectViewConfig,
        )
      navController.navigate(MainNavigationScreen.LocationSelector.route, args)
    }
    ApplicationWorkflow.LAUNCH_PDF_GENERATION -> {
        val pdfConfig = actionConfig.pdfConfig ?: return
        val interpolatedPdfConfig = pdfConfig.interpolate(computedValuesMap)
        val appCompatActivity = (navController.context as AppCompatActivity)
        PdfLauncherFragment.launch(appCompatActivity, interpolatedPdfConfig.encodeJson())
    }
    else -> return
  }
}

fun interpolateActionParamsValue(actionConfig: ActionConfig, resourceData: ResourceData?) =
  actionConfig.params
    .encodeJson()
    .interpolate(resourceData?.computedValuesMap ?: emptyMap())
    .decodeJson<List<ActionParameter>>()

/**
 * Apply navigation options. Restrict destination to only use a single instance in the back stack.
 */
fun navOptions(resId: Int, inclusive: Boolean = false, singleOnTop: Boolean = true) =
  NavOptions.Builder().setPopUpTo(resId, inclusive, true).setLaunchSingleTop(singleOnTop).build()

/**
 * Function to convert the elements of an array that have paramType [ActionParameterType.PARAMDATA]
 * to a map of [ActionParameter.key] against [ActionParameter](value).
 */
fun Array<ActionParameter>?.toParamDataMap(): Map<String, String> =
  this?.asSequence()
    ?.filter { it.paramType == ActionParameterType.PARAMDATA }
    ?.associate { it.key to it.value } ?: emptyMap()

fun List<OverflowMenuItemConfig>.decodeBinaryResourcesToBitmap(
  coroutineScope: CoroutineScope,
  registerRepository: RegisterRepository,
  decodedImageMap: SnapshotStateMap<String, Bitmap>,
) {
  this.forEach {
    val resourceId = it.icon!!.reference!!.extractLogicalIdUuid()
    coroutineScope.launch() {
      registerRepository.loadResource<Binary>(resourceId)?.let { binary ->
        decodedImageMap[resourceId] = binary.data.decodeToBitmap()
      }
    }
  }
}

fun Sequence<NavigationMenuConfig>.decodeBinaryResourcesToBitmap(
  coroutineScope: CoroutineScope,
  registerRepository: RegisterRepository,
  decodedImageMap: SnapshotStateMap<String, Bitmap>,
) {
  this.forEach {
    val resourceId = it.menuIconConfig!!.reference!!.extractLogicalIdUuid()
    coroutineScope.launch() {
      registerRepository.loadResource<Binary>(resourceId)?.let { binary ->
        decodedImageMap[resourceId] = binary.data.decodeToBitmap()
      }
    }
  }
}

suspend fun loadRemoteImagesBitmaps(
  views: List<ViewProperties>,
  registerRepository: RegisterRepository,
  computedValuesMap: Map<String, Any>,
  decodedImageMap: MutableMap<String, Bitmap>,
) {
  suspend fun ViewProperties.loadIcons() {
    when (this.viewType) {
      ViewType.IMAGE -> {
        val imageProps = this as ImageProperties
        if (
          !imageProps.imageConfig?.reference.isNullOrEmpty() &&
            imageProps.imageConfig?.type == ICON_TYPE_REMOTE
        ) {
          try {
            val resourceId =
              imageProps.imageConfig
                ?.reference
                ?.interpolate(computedValuesMap)
                ?.extractLogicalIdUuid()

            if (resourceId != null) {
              registerRepository.loadResource<Binary>(resourceId)?.let { binary ->
                decodedImageMap[resourceId] = binary.data.decodeToBitmap()
              }
            }
          } catch (exception: Exception) {
            Timber.e("Failed to decode image with error: ${exception.message}")
            throw exception
          }
        }
      }
      ViewType.ROW -> {
        val container = this as RowProperties
        container.children.forEach { it.loadIcons() }
      }
      ViewType.COLUMN -> {
        val container = this as ColumnProperties
        container.children.forEach { it.loadIcons() }
      }
      ViewType.CARD -> {
        val card = this as CardViewProperties
        card.content.forEach { it.loadIcons() }
      }
      ViewType.LIST -> {
        val list = this as ListProperties
        list.registerCard.views.forEach { it.loadIcons() }
      }
      ViewType.STACK -> {
        val stack = this as StackViewProperties
        stack.children.forEach { it.loadIcons() }
      }
      else -> {
        // Handle any other view types if needed
      }
    }
  }
  views.forEach { it.loadIcons() }
}
