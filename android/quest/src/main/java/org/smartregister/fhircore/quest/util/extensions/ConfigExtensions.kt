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

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID
import kotlin.collections.set
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hl7.fhir.r4.model.Binary
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.StackViewProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.task.NamedEventInterventionService
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeToBitmap
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.di.NamedEventInterventionEntryPoint
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.pdf.PdfLauncherFragment
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.fhircore.quest.util.openExternalApp
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
      }

      navController.navigate(
        resId = MainNavigationScreen.Home.route,
        args = args,
        navOptions = createRegisterNavigationOptions(actionConfig, navController),
      )
    }
    ApplicationWorkflow.LAUNCH_REPORT -> {
      val args =
        bundleOf(
          Pair(NavigationArg.REPORT_ID, actionConfig.id),
          Pair(NavigationArg.RESOURCE_ID, practitionerId?.extractLogicalIdUuid() ?: ""),
        )

      navController.navigate(MainNavigationScreen.Reports.route, args)
    }
    ApplicationWorkflow.LAUNCH_REPORT_INDICATORS -> {
      val args =
        bundleOf(
          Pair(NavigationArg.REPORT_ID, actionConfig.id),
        )

      navController.navigate(MainNavigationScreen.ReportIndicators.route, args)
    }
    ApplicationWorkflow.LAUNCH_SETTINGS ->
      navController.navigate(MainNavigationScreen.Settings.route)
    ApplicationWorkflow.LAUNCH_INSIGHT_SCREEN ->
      navController.navigate(MainNavigationScreen.Insight.route)
    ApplicationWorkflow.DEVICE_TO_DEVICE_SYNC -> startP2PScreen(navController.context)
    ApplicationWorkflow.LAUNCH_MAP -> {
      val args = bundleOf(NavigationArg.GEO_WIDGET_ID to actionConfig.id)
      // If value != null, we are navigating FROM a map; disallow same map navigation
      val currentGeoWidgetId =
        navController.currentBackStackEntry?.arguments?.getString(NavigationArg.GEO_WIDGET_ID)
      val sameGeoWidgetNavigation =
        args.getString(NavigationArg.GEO_WIDGET_ID) ==
          navController.previousBackStackEntry?.arguments?.getString(NavigationArg.GEO_WIDGET_ID)
      if (!currentGeoWidgetId.isNullOrEmpty() && sameGeoWidgetNavigation) {
        return
      } else {
        navController.navigate(
          resId = MainNavigationScreen.GeoWidgetLauncher.route,
          args = args,
          navOptions =
            navController.currentDestination?.id?.let {
              navOptions(resId = it, inclusive = actionConfig.popNavigationBackStack != false)
            },
        )
      }
    }
    ApplicationWorkflow.LAUNCH_DIALLER -> {
      val actionParameter = interpolatedParams.first()
      val phoneNumber = actionParameter.value
      val intent = Intent(Intent.ACTION_DIAL)
      intent.data = "tel:$phoneNumber".toUri()
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
    ApplicationWorkflow.LAUNCH_EXTERNAL_APP -> {
      actionConfig.externalAppConfig?.let { config ->
        openExternalApp(navController.context, config)
      }
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
    ApplicationWorkflow.DELETE_DRAFT_QUESTIONNAIRE -> {
      val questionnaireConfigInterpolated =
        actionConfig.questionnaire?.interpolate(computedValuesMap)
      val args =
        bundleOf(
          NavigationArg.QUESTIONNAIRE_CONFIG to questionnaireConfigInterpolated,
        )
      navController.navigate(MainNavigationScreen.AlertDialogFragment.route, args)
    }
    ApplicationWorkflow.APPLY_NAMED_EVENT -> {
      handleApplyNamedEvent(
        navController = navController,
        interpolatedParams = interpolatedParams,
        resourceId = resourceId,
        computedValuesMap = computedValuesMap,
      )
    }
    else -> return
  }
}

private fun handleApplyNamedEvent(
  navController: NavController,
  interpolatedParams: List<ActionParameter>,
  resourceId: String?,
  computedValuesMap: Map<String, Any>,
) {
  val context = navController.context
  val namedEvent =
    interpolatedParams.find { it.key == "namedEvent" }?.value?.takeIf { it.isNotBlank() }
      ?: "available-care"
  val subjectId =
    interpolatedParams.find { it.key == "subjectId" }?.value?.extractLogicalIdUuid()
      ?: resourceId?.extractLogicalIdUuid()
  if (subjectId.isNullOrBlank()) {
    context.showToast("No client selected for care", Toast.LENGTH_SHORT)
    return
  }

  val lifecycleOwner = context as? LifecycleOwner
  if (lifecycleOwner == null) {
    Timber.e("APPLY_NAMED_EVENT requires a LifecycleOwner context")
    context.showToast("Unable to start care", Toast.LENGTH_SHORT)
    return
  }

  val entryPoint =
    EntryPointAccessors.fromApplication(
      context.applicationContext,
      NamedEventInterventionEntryPoint::class.java,
    )
  val service = entryPoint.namedEventInterventionService()

  lifecycleOwner.lifecycleScope.launch {
    val plans =
      runCatching { service.listAvailableCarePlans(namedEvent, subjectId) }
        .onFailure { Timber.e(it, "Failed to list available care plans for event=$namedEvent") }
        .getOrDefault(emptyList())

    if (plans.isEmpty()) {
      context.showToast("No care available for this client", Toast.LENGTH_LONG)
      return@launch
    }

    showAvailableCarePicker(
      context = context,
      navController = navController,
      service = service,
      eventBus = entryPoint.eventBus(),
      namedEvent = namedEvent,
      subjectId = subjectId,
      plans = plans,
      title = actionDisplayOrDefault(computedValuesMap),
    )
  }
}

private fun actionDisplayOrDefault(computedValuesMap: Map<String, Any>): String {
  val fromMap = computedValuesMap["actionDisplay"] as? String
  return fromMap?.takeIf { it.isNotBlank() } ?: "Start care"
}

/**
 * One checkbox per PlanDefinition that carries the named-event trigger and has at least one
 * valid ($apply-resolved) action, plus a Start button — per
 * `feature/20260812-intervention-order-and-dedup.md`'s companion Android spec. All rows default
 * checked (everything listed is already eligible); Start begins the ordered launch sequence for
 * whichever rows remain checked.
 */
private fun showAvailableCarePicker(
  context: Context,
  navController: NavController,
  service: NamedEventInterventionService,
  eventBus: EventBus,
  namedEvent: String,
  subjectId: String,
  plans: List<NamedEventInterventionService.AvailableCarePlan>,
  title: String,
) {
  val labels = plans.map { it.title }.toTypedArray()
  val checked = BooleanArray(plans.size) { true }
  val selectedIndices = plans.indices.toMutableSet()
  AlertDialog.Builder(context)
    .setTitle(title)
    .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
      if (isChecked) selectedIndices.add(which) else selectedIndices.remove(which)
    }
    .setPositiveButton("Start") { _, _ ->
      val selectedPlans = selectedIndices.mapNotNull { plans.getOrNull(it) }
      if (selectedPlans.isEmpty()) return@setPositiveButton
      val session =
        AvailableCareSession(
          namedEvent = namedEvent,
          subjectId = subjectId,
          selectedPlanIds = selectedPlans.map { it.planDefinitionId }.toSet(),
          initialPlans = selectedPlans,
        )
      advanceAvailableCareSession(context, navController, service, eventBus, session)
    }
    .setNegativeButton(android.R.string.cancel, null)
    .show()
}

/**
 * Tracks one "select available care" run across its whole sequence of launches: which PDs the
 * user checked, which questionnaires are already submitted this session (so a re-`$apply` never
 * re-shows something just completed — no PD-level applicability condition exists yet, see
 * `feature/careplan-intervention-plandefinition.md` §26), and the current-visit Encounter id
 * once known (learned from the first submission that produced one).
 */
private class AvailableCareSession(
  val namedEvent: String,
  val subjectId: String,
  val selectedPlanIds: Set<String>,
  initialPlans: List<NamedEventInterventionService.AvailableCarePlan>,
) {
  /** Unique per session so [EventBus]'s one-time-per-consumer delivery doesn't cross sessions. */
  val consumerId: String = UUID.randomUUID().toString()
  val submittedQuestionnaireIds: MutableSet<String> = mutableSetOf()
  var encounterId: String? = null

  /**
   * The picker's own already-computed `$apply` result, reused for exactly the first batch
   * ("should have been saved, no need to run it again") — cleared after first use so every
   * subsequent batch re-runs `$apply` for real (an earlier submission may have unlocked a
   * lower-order action).
   */
  var cachedPlans: List<NamedEventInterventionService.AvailableCarePlan>? = initialPlans
}

/** Consolidates the selected PDs' current options, finds the lowest order still due. */
private suspend fun resolveNextBatch(
  service: NamedEventInterventionService,
  session: AvailableCareSession,
): List<NamedEventInterventionService.InterventionOption> {
  val plans =
    session.cachedPlans
      ?: runCatching { service.listAvailableCarePlans(session.namedEvent, session.subjectId) }
        .onFailure { Timber.e(it, "Failed to re-apply available-care PlanDefinitions") }
        .getOrDefault(emptyList())
  session.cachedPlans = null

  val consolidated =
    plans
      .filter { it.planDefinitionId in session.selectedPlanIds }
      .flatMap { it.options }
      .filterNot { session.submittedQuestionnaireIds.contains(it.questionnaireId) }
      .distinctBy { it.questionnaireId }
  val lowestOrder = consolidated.minOfOrNull { it.order } ?: return emptyList()
  return consolidated.filter { it.order == lowestOrder }
}

/**
 * Launches the next lowest-order due questionnaire(s) from [session]'s selected PDs, then waits
 * for its submission (via [EventBus]) to advance again. Ends silently once nothing is left due.
 *
 * Note: if the user backs out of the launched Questionnaire without submitting, no event fires
 * (matches today's `AppMainActivity.onSubmitQuestionnaire`, which only triggers on `RESULT_OK`)
 * and this session simply stops advancing — bounded by [LifecycleOwner]'s own scope cancellation,
 * not a leak, but the user would need to re-open the picker to resume.
 */
private fun advanceAvailableCareSession(
  context: Context,
  navController: NavController,
  service: NamedEventInterventionService,
  eventBus: EventBus,
  session: AvailableCareSession,
) {
  val lifecycleOwner = context as? LifecycleOwner ?: return
  lifecycleOwner.lifecycleScope.launch {
    val batch = resolveNextBatch(service, session)
    if (batch.isEmpty()) {
      Timber.i("Available-care session for subject=${session.subjectId} complete")
      return@launch
    }
    val chosen = if (batch.size == 1) batch.first() else awaitTieBreakChoice(context, batch)
    val chosenQuestionnaireId = chosen?.questionnaireId
    if (chosen == null || chosenQuestionnaireId.isNullOrBlank()) return@launch

    launchInterventionOption(navController, chosen, session.encounterId)

    val submission =
      eventBus.events
        .getFor(session.consumerId)
        .filterIsInstance<AppEvent.OnSubmitQuestionnaire>()
        .first { it.questionnaireSubmission.questionnaireConfig.id == chosenQuestionnaireId }
        .questionnaireSubmission

    session.submittedQuestionnaireIds.add(chosenQuestionnaireId)
    if (session.encounterId == null && submission.questionnaireResponse.hasEncounter()) {
      session.encounterId =
        submission.questionnaireResponse.encounter.reference?.extractLogicalIdUuid()
    }
    advanceAvailableCareSession(context, navController, service, eventBus, session)
  }
}

/** Single-choice fallback when more than one option ties at the lowest order. */
private suspend fun awaitTieBreakChoice(
  context: Context,
  batch: List<NamedEventInterventionService.InterventionOption>,
): NamedEventInterventionService.InterventionOption? = suspendCancellableCoroutine { cont ->
  val labels = batch.map { it.title }.toTypedArray()
  val dialog =
    AlertDialog.Builder(context)
      .setTitle("Choose one")
      .setItems(labels) { _, which -> cont.resume(batch.getOrNull(which)) {} }
      .setOnCancelListener { cont.resume(null) {} }
      .show()
  cont.invokeOnCancellation { dialog.dismiss() }
}

private fun launchInterventionOption(
  navController: NavController,
  option: NamedEventInterventionService.InterventionOption,
  encounterId: String? = null,
) {
  val questionnaireId = option.questionnaireId
  if (!questionnaireId.isNullOrBlank() && navController.context is QuestionnaireHandler) {
    Timber.i(
      "APPLY_NAMED_EVENT launching Questionnaire/$questionnaireId title=${option.title} " +
        "order=${option.order} encounter=$encounterId",
    )
    val actionParams =
      if (encounterId.isNullOrBlank()) {
        emptyList()
      } else {
        // Makes the current-visit Encounter id available to CQL as the `encounterid` library
        // parameter (see `feature/20260812-intervention-order-and-dedup.md`, tricc) so the
        // dedup `initialExpression`s it wires up can actually resolve.
        listOf(
          ActionParameter(
            key = "encounter",
            paramType = ActionParameterType.QUESTIONNAIRE_RESPONSE_POPULATION_RESOURCE,
            value = encounterId,
            resourceType = org.hl7.fhir.r4.model.ResourceType.Encounter,
          ),
        )
      }
    (navController.context as QuestionnaireHandler).launchQuestionnaire(
      context = navController.context,
      questionnaireConfig =
        QuestionnaireConfig(
          id = questionnaireId,
          title = option.title,
          resourceType = org.hl7.fhir.r4.model.ResourceType.Patient,
          saveButtonText = "Save",
        ),
      actionParams = actionParams,
    )
    return
  }

  // Should not happen once listAvailableCarePlans filters to Questionnaire-only options.
  navController.context.showToast(
    "No questionnaire for: ${option.title}",
    Toast.LENGTH_LONG,
  )
  Timber.e(
    "APPLY_NAMED_EVENT option has no Questionnaire id=${option.id} definition=${option.definitionCanonical}",
  )
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

suspend fun String.referenceToBitmap(
  fhirEngine: FhirEngine,
  decodedImageMap: SnapshotStateMap<String, Bitmap>,
  forceRefresh: Boolean = false,
): Bitmap? {
  val resourceId = this.extractLogicalIdUuid()
  if (!decodedImageMap.containsKey(resourceId) || forceRefresh) {
    fhirEngine.loadResource<Binary>(resourceId)?.let { binary ->
      binary.data.decodeToBitmap()?.let { bitmap -> decodedImageMap[resourceId] = bitmap }
    }
  }
  return decodedImageMap[resourceId]
}

suspend fun List<ViewProperties>.decodeImageResourcesToBitmap(
  fhirEngine: FhirEngine,
  decodedImageMap: MutableMap<String, Bitmap>,
) {
  val queue = ArrayDeque(this)
  while (queue.isNotEmpty()) {
    val viewProperty = queue.removeFirst()
    when (viewProperty.viewType) {
      ViewType.IMAGE -> {
        val imageProperties = (viewProperty as ImageProperties)
        if (imageProperties.imageConfig != null) {
          val imageConfig = imageProperties.imageConfig
          if (
            ICON_TYPE_REMOTE.equals(imageConfig?.type, ignoreCase = true) &&
              !imageConfig?.reference.isNullOrBlank()
          ) {
            val resourceId = imageConfig.reference!!
            fhirEngine.loadResource<Binary>(resourceId)?.let { binary: Binary ->
              binary.data.decodeToBitmap()?.let { bitmap -> decodedImageMap[resourceId] = bitmap }
            }
          }
        }
      }
      ViewType.COLUMN -> (viewProperty as ColumnProperties).children.forEach(queue::addLast)
      ViewType.ROW -> (viewProperty as RowProperties).children.forEach(queue::addLast)
      ViewType.SERVICE_CARD ->
        (viewProperty as ServiceCardProperties).details.forEach(queue::addLast)
      ViewType.CARD -> (viewProperty as CardViewProperties).content.forEach(queue::addLast)
      ViewType.LIST -> (viewProperty as ListProperties).registerCard.views.forEach(queue::addLast)
      ViewType.STACK -> (viewProperty as StackViewProperties).children.forEach(queue::addLast)
      else -> {
        /** Ignore other views that cannot display images* */
      }
    }
  }
}

internal fun createRegisterNavigationOptions(
  actionConfig: ActionConfig,
  navController: NavController,
): NavOptions {
  val navOptionsBuilder = NavOptions.Builder().setLaunchSingleTop(true)

  if (
    actionConfig.popNavigationBackStack == true &&
      navController.currentBackStackEntry?.destination?.id != MainNavigationScreen.Home.route
  ) {
    navController.currentBackStackEntry?.destination?.id?.let {
      navOptionsBuilder.setPopUpTo(it, inclusive = true)
    }
  }

  return navOptionsBuilder.build()
}
