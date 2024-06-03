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

package org.smartregister.fhircore.engine.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.fhir.sync.SyncJobStatus
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.model.response.UserClaimInfo
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

/**
 * A singleton class that maintains a list of [OnSyncListener] that have been registered to listen
 * to [SyncJobStatus] emitted to indicate sync progress.
 */
@Singleton
class SyncListenerManager
@Inject
constructor(
  val configService: ConfigService,
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
) {

  private val syncConfig by lazy { configurationRegistry.getSyncConfigs() }

  private val _onSyncListeners = mutableListOf<WeakReference<OnSyncListener>>()
  val onSyncListeners: List<OnSyncListener>
    get() = _onSyncListeners.mapNotNull { it.get() }

  /**
   * Register [OnSyncListener] for [SyncJobStatus]. Typically the [OnSyncListener] will be
   * implemented in a [Lifecycle](an Activity/Fragment). This function ensures the [OnSyncListener]
   * is removed for the [_onSyncListeners] list when the [Lifecycle] changes to
   * [Lifecycle.State.DESTROYED]
   */
  fun registerSyncListener(onSyncListener: OnSyncListener, lifecycle: Lifecycle) {
    _onSyncListeners.add(WeakReference(onSyncListener))
    Timber.w("${onSyncListener::class.simpleName} registered to receive sync state events")
    lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
          super.onStop(owner)
          deregisterSyncListener(onSyncListener)
        }
      },
    )
  }

  /**
   * This function removes [onSyncListener] from the list of registered [OnSyncListener]'s to stop
   * receiving sync state events.
   */
  fun deregisterSyncListener(onSyncListener: OnSyncListener) {
    val removed = _onSyncListeners.removeIf { it.get() == onSyncListener }
    if (removed) {
      Timber.w("De-registered ${onSyncListener::class.simpleName} from receiving sync state...")
    }
  }

  /** Retrieve registry sync params */
  fun loadSyncParams(): Map<ResourceType, Map<String, String>> {
    val userInfo =
      sharedPreferencesHelper.read<UserClaimInfo>(SharedPreferenceKey.USER_CLAIM_INFO.name)
    val resourceTypeParamsMap = linkedMapOf<ResourceType, List<Pair<String, String>>>()

    val appConfig = configurationRegistry.getAppConfigs()

    // TODO Does not support nested parameters i.e. parameters.parameters...
    // TODO: expressionValue supports for Organization and Publisher literals for now
    syncConfig
      ?.parameter
      ?.map { it.resource }
      ?.forEach { sp ->
        val paramName = sp.name // e.g. organization
        val paramLiteral = "#$paramName" // e.g. #organization in expression for replacement
        val paramExpression = sp.expression
        val expressionValue =
          when (paramName) {
            ConfigurationRegistry.ORGANIZATION -> userInfo?.organization
            ConfigurationRegistry.PUBLISHER -> userInfo?.questionnairePublisher
            ConfigurationRegistry.ID -> paramExpression
            ConfigurationRegistry.COUNT -> appConfig.count
            else -> null
          }?.let {
            // replace the evaluated value into expression for complex expressions
            // e.g. #organization -> 123
            // e.g. patient.organization eq #organization -> patient.organization eq 123
            paramExpression?.replace(paramLiteral, it)
          }

        // for each entity in base create and add param map
        // [Patient=[ name=Abc, organization=111 ], Encounter=[ type=MyType, location=MyHospital
        // ],..]
        sp.base.forEach { base ->
          val resourceType = ResourceType.fromCode(base)
          expressionValue?.let { value ->
            resourceTypeParamsMap.merge(resourceType, listOf(sp.code to value)) { list1, list2 ->
              //                resourceType.filterBasedOnPerResourceType(this)
              return@merge list1.toMutableList().apply { addAll(list2) }
            }
          }
        }
      }

    filterBasedOnPerResourceType().forEach { (type, filters) ->
      resourceTypeParamsMap.merge(type, filters) { list1, list2 ->
        return@merge list1.toMutableList().apply { addAll(list2) }
      }
    }
    val filterByLocationParams =
      sharedPreferencesHelper.filterByResourceLocation(resourceTypeParamsMap)

    val mergeSyncConfigParams =
      resourceTypeParamsMap
        //        .filter { it.key == ResourceType.Patient }
        .map {
          val resourceType = it.key
          val paramsMap = linkedMapOf<String, String>("_total" to "none")
          paramsMap.putAll(filterByLocationParams.getOrDefault(resourceType, emptyList()))
          paramsMap.putAll(it.value)
          resourceType to paramsMap
        }
        .toMap()

    val orderedSyncConfigParams =
      linkedMapOf<ResourceType, Map<String, String>>().apply {
        put(
          ResourceType.Binary,
          mergeSyncConfigParams.getOrDefault(ResourceType.Binary, emptyMap()),
        )
        put(
          ResourceType.StructureMap,
          mergeSyncConfigParams.getOrDefault(ResourceType.StructureMap, emptyMap()),
        )
        put(
          ResourceType.Questionnaire,
          mergeSyncConfigParams.getOrDefault(ResourceType.Questionnaire, emptyMap()),
        )
        putAll(mergeSyncConfigParams)
      }

    Timber.i("SYNC CONFIG $orderedSyncConfigParams")
    return orderedSyncConfigParams
  }
}

private fun filterBasedOnPerResourceType(): Map<ResourceType, List<Pair<String, String>>> {
  return mutableMapOf<ResourceType, List<Pair<String, String>>>().apply {
    put(ResourceType.RelatedPerson, listOf(RelatedPerson.SP_ACTIVE to true.toString()))
    put(ResourceType.Patient, listOf(Patient.SP_ACTIVE to true.toString()))
    put(
      ResourceType.Observation,
      listOf(
        Observation.SP_STATUS to Observation.ObservationStatus.PRELIMINARY.toString().lowercase(),
      ),
    )
    put(
      ResourceType.Appointment,
      listOf(
        Appointment.SP_STATUS to
          "${Appointment.AppointmentStatus.BOOKED.toString().lowercase()},${Appointment.AppointmentStatus.PROPOSED.toString().lowercase()}",
      ),
    )
    put(
      ResourceType.Encounter,
      listOf(Encounter.SP_STATUS to Encounter.EncounterStatus.INPROGRESS.toString().lowercase()),
    )
    put(
      ResourceType.List,
      listOf(ListResource.SP_STATUS to ListResource.ListStatus.CURRENT.toString().lowercase()),
    )
    //    put(ResourceType.CarePlan, mapOf(CarePlan.SP_STATUS to
    // CarePlan.CarePlanStatus.ACTIVE.toString().lowercase()))
    //    put(ResourceType.Task, mapOf(Task.SP_STATUS to String.format(
    //      "%s,%s",
    //      Task.TaskStatus.FAILED.toString().lowercase(),
    //      Task.TaskStatus.INPROGRESS.toString().lowercase()
    //    )))
  }
}

private fun SharedPreferencesHelper.filterByResourceLocation(
  resourceTypePairsMap: Map<ResourceType, List<Pair<String, String>>>,
): Map<ResourceType, List<Pair<String, String>>> {
  val organisationSystem = context.getString(R.string.sync_strategy_organization_system)
  val organisationTag = "$organisationSystem|${organisationCode()}"

  return resourceTypePairsMap
    .filter {
      it.key !in
        arrayOf(
          ResourceType.Practitioner,
          ResourceType.Questionnaire,
          ResourceType.StructureMap,
          ResourceType.Binary,
        )
    }
    .map { it.key to listOf("_tag" to organisationTag) }
    .toMap()
}

private fun MutableList<Pair<ResourceType, Map<String, String>>>.addParam(
  resourceType: ResourceType,
  param: String,
  value: String,
) {
  add(Pair(resourceType, mapOf(param to value)))
}
