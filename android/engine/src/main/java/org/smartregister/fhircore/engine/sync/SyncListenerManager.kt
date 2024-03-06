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
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.FhirConfiguration
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.register.dao.organisationCode
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
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

  private val syncConfig by lazy {
    configurationRegistry.retrieveConfiguration<FhirConfiguration<Parameters>>(
      AppConfigClassification.SYNC,
    )
  }

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
    val authenticatedUserInfo =
      sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()
    val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()

    val appConfig =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
        AppConfigClassification.APPLICATION,
      )

    // TODO Does not support nested parameters i.e. parameters.parameters...
    // TODO: expressionValue supports for Organization and Publisher literals for now
    syncConfig.resource.parameter
      .map { it.resource as SearchParameter }
      .forEach { sp ->
        val paramName = sp.name // e.g. organization
        val paramLiteral = "#$paramName" // e.g. #organization in expression for replacement
        val paramExpression = sp.expression
        val expressionValue =
          when (paramName) {
            ConfigurationRegistry.ORGANIZATION -> authenticatedUserInfo?.organization
            ConfigurationRegistry.PUBLISHER -> authenticatedUserInfo?.questionnairePublisher
            ConfigurationRegistry.ID -> paramExpression
            ConfigurationRegistry.COUNT -> appConfig.count
            else -> null
          }?.let {
            // replace the evaluated value into expression for complex expressions
            // e.g. #organization -> 123
            // e.g. patient.organization eq #organization -> patient.organization eq 123
            paramExpression.replace(paramLiteral, it)
          }

        // for each entity in base create and add param map
        // [Patient=[ name=Abc, organization=111 ], Encounter=[ type=MyType, location=MyHospital
        // ],..]
        sp.base.forEach { base ->
          val resourceType = ResourceType.fromCode(base.code)
          val pair = pairs.find { it.first == resourceType }
          if (pair == null) {
            pairs.add(
              Pair(
                resourceType,
                expressionValue?.let { mapOf(sp.code to expressionValue) } ?: mapOf(),
              ),
            )
          } else {
            expressionValue?.let {
              // add another parameter if there is a matching resource type
              // e.g. [(Patient, {organization=105})] to [(Patient, {organization=105, _count=100})]
              val updatedPair = pair.second.toMutableMap().apply { put(sp.code, expressionValue) }
              val index = pairs.indexOfFirst { it.first == resourceType }
              resourceType.filterBasedOnPerResourceType(pairs)
              pairs.set(index, Pair(resourceType, updatedPair))
            }
          }
        }
      }

    val syncConfigParams = sharedPreferencesHelper.filterByResourceLocation(pairs)
    Timber.i("SYNC CONFIG $syncConfigParams")

    return mapOf(*syncConfigParams.toTypedArray())
  }
}

private fun ResourceType.filterBasedOnPerResourceType(
  pairs: MutableList<Pair<ResourceType, Map<String, String>>>,
) =
  when (this) {
    ResourceType.RelatedPerson ->
      pairs.addParam(resourceType = this, param = RelatedPerson.SP_ACTIVE, value = true.toString())
    ResourceType.Patient ->
      pairs.addParam(resourceType = this, param = Patient.SP_ACTIVE, value = true.toString())
    ResourceType.CarePlan ->
      pairs.addParam(
        resourceType = this,
        param = CarePlan.SP_STATUS,
        value = CarePlan.CarePlanStatus.ACTIVE.toString().lowercase(),
      )
    ResourceType.Observation ->
      pairs.addParam(
        resourceType = this,
        param = Observation.SP_STATUS,
        value = Observation.ObservationStatus.FINAL.toString().lowercase(),
      )

    //    ResourceType.Task ->
    //      pairs.addParam(
    //        resourceType = this,
    //        param = Task.SP_STATUS,
    //        value =
    //          String.format(
    //            "%s,%s",
    //            Task.TaskStatus.FAILED.toString().lowercase(),
    //            Task.TaskStatus.INPROGRESS.toString().lowercase()
    //          )
    //      )

    ResourceType.Appointment ->
      pairs.addParam(
        resourceType = this,
        param = Appointment.SP_STATUS,
        value = Appointment.AppointmentStatus.BOOKED.toString().lowercase(),
      )
    ResourceType.Encounter ->
      pairs.addParam(
        resourceType = this,
        param = Encounter.SP_STATUS,
        value = Encounter.EncounterStatus.INPROGRESS.toString().lowercase(),
      )
    ResourceType.List ->
      pairs.addParam(
        resourceType = this,
        param = ListResource.SP_STATUS,
        value = ListResource.ListStatus.CURRENT.toString().lowercase(),
      )
    else -> Unit
  }

private fun SharedPreferencesHelper.filterByResourceLocation(
  pairs: MutableList<Pair<ResourceType, Map<String, String>>>,
): MutableList<Pair<ResourceType, Map<String, String>>> {
  val resourcesTemp = mutableListOf<Pair<ResourceType, Map<String, String>>>()
  val results = mutableListOf<Pair<ResourceType, Map<String, String>>>()
  resourcesTemp.addAll(pairs)

  val organisationSystem = context.getString(R.string.sync_strategy_organization_system)
  val organisationTag = "$organisationSystem|${organisationCode()}"

  resourcesTemp.forEach {
    val resourceType = it.first
    if (
      resourceType != ResourceType.Practitioner &&
        resourceType != ResourceType.Questionnaire &&
        resourceType != ResourceType.StructureMap
    ) {
      val tags = mutableMapOf("_tag" to organisationTag)
      it.second.entries.forEach { entry -> tags[entry.key] = entry.value }
      results.add(Pair(resourceType, tags))
    } else results.add(it)
  }
  return results
}

private fun MutableList<Pair<ResourceType, Map<String, String>>>.addParam(
  resourceType: ResourceType,
  param: String,
  value: String,
) {
  add(Pair(resourceType, mapOf(param to value)))
}
