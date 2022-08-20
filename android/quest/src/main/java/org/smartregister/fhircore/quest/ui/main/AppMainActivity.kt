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

package org.smartregister.fhircore.quest.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.sync.State
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_BACK_REFERENCE_KEY
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.geowidget.screens.GeowidgetActivity
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalMaterialApi
open class AppMainActivity : BaseMultiLanguageActivity(), OnSyncListener {

  @Inject lateinit var syncBroadcaster: SyncBroadcaster

  @Inject lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator

  @Inject lateinit var configService: ConfigService

  @Inject lateinit var defaultRepository: DefaultRepository

  val appMainViewModel by viewModels<AppMainViewModel>()

  lateinit var getLocationPos: ActivityResultLauncher<Intent>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { AppTheme { MainScreen(appMainViewModel = appMainViewModel) } }
    syncBroadcaster.registerSyncListener(this, lifecycleScope)

    val getLocationPos = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val intent = result.data ?: run {
        Timber.e(Exception("Data back from GeowidgetActivity is null"))
        return@registerForActivityResult
      }
      intent.getStringExtra(GeowidgetActivity.FAMILY_ID)?.also { familyId ->
        launchFamilyProfile(familyId)
      } ?: also {
        Timber.i(Exception("FAMILY-ID from GeowidgetActivity is null"))
      }

      intent.getStringExtra(GeowidgetActivity.LOCATION_ID)?.also { locationId ->
        launchFamilyRegistrationWithLocationId(locationId)
        return@registerForActivityResult
      } ?: also {
        Timber.i(Exception("LOCATION-ID from GeowidgetActivity is null"))
      }

    }

    appMainViewModel.mapLauncher = getLocationPos
  }

  private fun launchFamilyRegistrationWithLocationId(locationId: String) {
    lifecycleScope.launch(Dispatchers.IO) {
      val fhirEngine = FhirEngineProvider.getInstance(this@AppMainActivity)

      val location = fhirEngine.get(ResourceType.Location, locationId)
      val locationString =
        FhirContext.forR4Cached().newJsonParser().encodeResourceToString(location)

      val bundle = bundleOf(
        Pair(QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES, arrayListOf(locationString))
      )

      lifecycleScope.launch(Dispatchers.Main) {
        launchQuestionnaire<QuestionnaireActivity>("82952-geowidget", intentBundle = bundle)
      }
    }
  }



  private fun launchFamilyProfile(familyId: String) {
    Timber.i("Launching family profile for : $familyId")
  }

  override fun onResume() {
    super.onResume()
    /*val locationJson = """{"resourceType":"Location","id":"136702","meta":{"versionId":"3","lastUpdated":"2022-07-28T18:21:39.739+00:00","source":"#18c074df71ca7366"},"status":"active","name":"Kenyatta Hospital Visitors Parking","description":"Parking Lobby","telecom":[{"system":"phone","value":"020 2726300"},{"system":"email","value":"knhadmin@knh.or.ke"}],"address":{"line":["P.O. Box 20723"],"city":"Nairobi","postalCode":"00202","country":"Kenya"},"physicalType":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/location-physical-type","code":"area","display":"Area"}]},"position":{"longitude":36.80826008319855,"latitude":-1.301070677485388}}"""
    val questionnaireJson = """{"resourceType":"Questionnaire","id":"82952-geowidget","meta":{"versionId":"13","lastUpdated":"2022-07-22T03:29:31.585+00:00","source":"#3f0c44886711e1d5","profile":["http://ehelse.no/fhir/StructureDefinition/sdf-Questionnaire"],"tag":[{"system":"urn:ietf:bcp:47","code":"en-GB","display":"English"},{"system":"urn:ietf:bcp:47","code":"nb-NO","display":"Norsk Bokmål"}]},"extension":[{"url":"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-targetStructureMap","valueCanonical":"https://fhir.labs.smartregister.org/fhir/StructureMap/5667cfbd-13c4-4111-b952-7cee58bdb9d5-geowidget"}],"version":"0.0.2","name":"eCBIS Family Registration","title":"eCBIS Family Registration","subjectType":["Group"],"date":"2022-02-14T00:00:00.000Z","publisher":"ona","contact":[{"name":"http://www.ona.io"}],"description":"eCBIS Family Registration","useContext":[{"code":{"system":"http://hl7.org/fhir/ValueSet/usage-context-type","code":"focus","display":"Clinical Focus"},"valueCodeableConcept":{"coding":[{"system":"urn:oid:2.16.578.1.12.4.1.1.8655"}]}}],"purpose":"eCBIS Family Registration","code":[{"system":"https://www.snomed.org","code":"35359004","display":"Family"}],"item":[{"extension":[{"url":"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression","valueExpression":{"language":"text/fhirpath","expression":"Group.name"}}],"linkId":"64712707-d6eb-4166-8d6e-06fa2b9fcf3e","definition":"http://hl7.org/fhir/StructureDefinition/Group#Group.name","text":"Family Name","type":"string","required":true},{"extension":[{"url":"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression","valueExpression":{"language":"text/fhirpath","expression":"Group.identifier[0].value"}}],"linkId":"ce49bde4-6ef9-423e-c747-efab250cd770","definition":"http://hl7.org/fhir/StructureDefinition/Group#Group.identifier.value","text":"Household ID","type":"string","required":true},{"linkId":"7d9dba4c-7407-4eb2-d791-2d1834b6afcc","text":"Village or Town","type":"string","required":true},{"linkId":"household-location-reference","text":"Household Location Reference","type":"string","required":true,"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/questionnaire-hidden","valueBoolean":true},{"url":"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression","valueExpression":{"language":"text/fhirpath","expression":"Location.id"}}]}]}"""
    val structureMapJson = """{"resourceType":"StructureMap","id":"5667cfbd-13c4-4111-b952-7cee58bdb9d5-geowidget","url":"https://fhir.labs.smartregister.org/fhir/StructureMap/5667cfbd-13c4-4111-b952-7cee58bdb9d5-geowidget","name":"eCBIS Family Registration","structure":[{"url":"http://hl7.org/fhir/StructureDefinition/QuestionnaireReponse","mode":"source"},{"url":"http://hl7.org/fhir/StructureDefinition/Bundle","mode":"target"},{"url":"http://hl7.org/fhir/StructureDefinition/Group","mode":"target"}],"group":[{"name":"eCBISFamilyRegistration","typeMode":"none","input":[{"name":"src","type":"QuestionnaireResponse","mode":"source"},{"name":"bundle","type":"Bundle","mode":"target"}],"rule":[{"name":"rule_bundle_id","source":[{"context":"src"}],"target":[{"context":"bundle","contextType":"variable","element":"id","transform":"uuid"}]},{"name":"rule_bundle_type","source":[{"context":"src"}],"target":[{"context":"bundle","contextType":"variable","element":"type","transform":"copy","parameter":[{"valueString":"collection"}]}]},{"name":"rule_bundle_entries","source":[{"context":"src"}],"target":[{"context":"bundle","contextType":"variable","element":"entry","variable":"entry"},{"context":"entry","contextType":"variable","element":"resource","variable":"group","transform":"create","parameter":[{"valueString":"Group"}]}],"dependent":[{"name":"ExtractGroup","variable":["src","group"]},{"name":"ExtractEncounter","variable":["src","bundle"]}]}]},{"name":"ExtractGroup","typeMode":"none","input":[{"name":"src","type":"QuestionnaireResponse","mode":"source"},{"name":"group","type":"Group","mode":"target"}],"rule":[{"name":"rule_group_id_generation","source":[{"context":"src"}],"target":[{"context":"group","contextType":"variable","element":"id","transform":"uuid"}]},{"name":"rule_group_name","source":[{"context":"src"}],"target":[{"context":"group","contextType":"variable","element":"name","transform":"evaluate","parameter":[{"valueId":"src"},{"valueString":"${'$'}this.item.where(linkId = '64712707-d6eb-4166-8d6e-06fa2b9fcf3e').answer.value"}]}]},{"name":"rule_group_identifier_national_id","source":[{"context":"src"}],"target":[{"context":"group","contextType":"variable","element":"identifier","variable":"groupIdentifierHouseholdId","transform":"create","parameter":[{"valueString":"Identifier"}]}],"rule":[{"name":"rule_group_identifier_national_id_value","source":[{"context":"src"}],"target":[{"context":"groupIdentifierHouseholdId","contextType":"variable","element":"value","transform":"evaluate","parameter":[{"valueId":"src"},{"valueString":"${'$'}this.item.where(linkId = 'ce49bde4-6ef9-423e-c747-efab250cd770').answer.value"}]}]},{"name":"rule_group_identifier_national_id_use","source":[{"context":"src"}],"target":[{"context":"groupIdentifierHouseholdId","contextType":"variable","element":"use","transform":"copy","parameter":[{"valueString":"official"}]}]},{"name":"rule_group_identifier_period","source":[{"context":"src"}],"target":[{"context":"groupIdentifierHouseholdId","contextType":"variable","element":"period","variable":"period","transform":"create","parameter":[{"valueString":"Period"}]},{"context":"period","contextType":"variable","element":"start","transform":"evaluate","parameter":[{"valueId":"src"},{"valueString":"${'$'}this.authored"}]}]}]},{"name":"rule_group_identifier","source":[{"context":"src"}],"target":[{"context":"group","contextType":"variable","element":"identifier","variable":"groupIdentifier","transform":"create","parameter":[{"valueString":"Identifier"}]}],"rule":[{"name":"rule_group_identifier_value","source":[{"context":"src"}],"target":[{"context":"groupIdentifier","contextType":"variable","element":"value","transform":"uuid"}]},{"name":"rule_group_identifier_use","source":[{"context":"src"}],"target":[{"context":"groupIdentifier","contextType":"variable","element":"use","transform":"copy","parameter":[{"valueString":"secondary"}]}]},{"name":"rule_group_identifier_period","source":[{"context":"src"}],"target":[{"context":"groupIdentifier","contextType":"variable","element":"period","variable":"period","transform":"create","parameter":[{"valueString":"Period"}]},{"context":"period","contextType":"variable","element":"start","transform":"evaluate","parameter":[{"valueId":"src"},{"valueString":"${'$'}this.authored"}]}]}]},{"name":"group_characteristic","source":[{"context":"src"}],"target":[{"context":"group","contextType":"variable","element":"characteristic","variable":"groupCharacteristic","transform":"create","parameter":[{"valueString":"Group_Characteristic"}]}],"rule":[{"name":"group_characteristic_cc","source":[{"context":"src"}],"target":[{"context":"groupCharacteristic","contextType":"variable","element":"code","variable":"concept","transform":"create","parameter":[{"valueString":"CodeableConcept"}]}],"rule":[{"name":"group_characteristic_cc_cod","source":[{"context":"src"}],"target":[{"context":"concept","contextType":"variable","element":"coding","transform":"c","parameter":[{"valueString":"http://fhir.labs.smartregister.org/codes/family-location"},{"valueString":"location"}]}]},{"name":"group_characteristic_cc_text","source":[{"context":"src"}],"target":[{"context":"concept","contextType":"variable","element":"text","transform":"copy","parameter":[{"valueString":"Household Location"}]}]}]},{"name":"group_characteristic_gc","source":[{"context":"src"}],"target":[{"context":"groupCharacteristic","contextType":"variable","element":"value","variable":"ref","transform":"create","parameter":[{"valueString":"Reference"}]}],"rule":[{"name":"group_characteristic_gc_1","source":[{"context":"src"}],"target":[{"context":"ref","contextType":"variable","element":"reference","transform":"evaluate","parameter":[{"valueId":"src"},{"valueString":"'Location/' + ${'$'}this.item.where(linkId = 'household-location-reference').answer.value"}]}]}]}]},{"name":"r_grp_status_data","source":[{"context":"src"}],"target":[{"context":"group","contextType":"variable","element":"active","transform":"copy","parameter":[{"valueBoolean":true}]}]},{"name":"r_grp_type_data","source":[{"context":"src"}],"target":[{"context":"group","contextType":"variable","element":"type","transform":"copy","parameter":[{"valueString":"person"}]}]},{"name":"r_grp_code_data","source":[{"context":"src"}],"target":[{"context":"group","contextType":"variable","element":"code","variable":"concept","transform":"create","parameter":[{"valueString":"CodeableConcept"}]}],"dependent":[{"name":"ExtractFamilyCode","variable":["src","concept"]}]}]},{"name":"ExtractEncounter","typeMode":"none","input":[{"name":"src","type":"QuestionnaireResponse","mode":"source"},{"name":"bundle","type":"Bundle","mode":"target"}],"rule":[{"name":"r_en","source":[{"context":"src"}],"target":[{"context":"bundle","contextType":"variable","element":"entry","variable":"entry"},{"context":"entry","contextType":"variable","element":"resource","variable":"encounter","transform":"create","parameter":[{"valueString":"Encounter"}]}],"rule":[{"name":"r_en_id","source":[{"context":"src"}],"target":[{"context":"encounter","contextType":"variable","element":"id","transform":"uuid"}]},{"name":"r_en_st","source":[{"context":"src"}],"target":[{"context":"encounter","contextType":"variable","element":"status","transform":"copy","parameter":[{"valueString":"finished"}]}]},{"name":"r_en_cls","source":[{"context":"src"}],"target":[{"context":"encounter","contextType":"variable","element":"class","transform":"c","parameter":[{"valueString":"http://terminology.hl7.org/CodeSystem/v3-ActCode"},{"valueString":"HH"},{"valueString":"home health"}]}]},{"name":"r_en_typ","source":[{"context":"src"}],"target":[{"context":"encounter","contextType":"variable","element":"type","variable":"concept","transform":"create","parameter":[{"valueString":"CodeableConcept"}]}],"rule":[{"name":"r_en_cc_cod","source":[{"context":"src"}],"target":[{"context":"concept","contextType":"variable","element":"coding","variable":"coding","transform":"c","parameter":[{"valueString":"http://snomed.info/sct"},{"valueString":"184048005"}]}],"rule":[{"name":"r_en_cod_disp","source":[{"context":"src"}],"target":[{"context":"coding","contextType":"variable","element":"display","transform":"copy","parameter":[{"valueString":"Registration"}]}]}]},{"name":"r_en_typ_text","source":[{"context":"src"}],"target":[{"context":"concept","contextType":"variable","element":"text","transform":"copy","parameter":[{"valueString":"Registration"}]}]}]},{"name":"r_en_prio","source":[{"context":"src"}],"target":[{"context":"encounter","contextType":"variable","element":"priority","variable":"concept","transform":"create","parameter":[{"valueString":"CodeableConcept"}]}],"rule":[{"name":"r_en_cc_cod","source":[{"context":"src"}],"target":[{"context":"concept","contextType":"variable","element":"coding","variable":"coding","transform":"c","parameter":[{"valueString":"http://terminology.hl7.org/ValueSet/v3-ActPriority"},{"valueString":"EL"}]}],"rule":[{"name":"r_en_cod_disp","source":[{"context":"src"}],"target":[{"context":"coding","contextType":"variable","element":"display","transform":"copy","parameter":[{"valueString":"elective"}]}]}]},{"name":"r_en_prio_text","source":[{"context":"src"}],"target":[{"context":"concept","contextType":"variable","element":"text","transform":"copy","parameter":[{"valueString":"elective"}]}]}]},{"name":"r_en_sub","source":[{"context":"src","element":"subject","variable":"subject"}],"target":[{"context":"encounter","contextType":"variable","element":"subject","transform":"copy","parameter":[{"valueId":"subject"}]}]},{"name":"r_en_per","source":[{"context":"src"}],"target":[{"context":"encounter","contextType":"variable","element":"period","variable":"enPeriod","transform":"create","parameter":[{"valueString":"Period"}]}],"rule":[{"name":"r_en_per_start","source":[{"context":"src"}],"target":[{"context":"enPeriod","contextType":"variable","element":"start","transform":"evaluate","parameter":[{"valueId":"src"},{"valueString":"now()"}]}]},{"name":"r_en_per_end","source":[{"context":"src"}],"target":[{"context":"enPeriod","contextType":"variable","element":"end","transform":"evaluate","parameter":[{"valueId":"src"},{"valueString":"now()"}]}]}]},{"name":"r_en_reason","source":[{"context":"src"}],"target":[{"context":"encounter","contextType":"variable","element":"reasonCode","variable":"concept","transform":"create","parameter":[{"valueString":"CodeableConcept"}]}],"rule":[{"name":"r_en_text","source":[{"context":"src"}],"target":[{"context":"concept","contextType":"variable","element":"text","transform":"copy","parameter":[{"valueString":"Family Registration Task"}]}]}]}]}]},{"name":"ExtractFamilyCode","typeMode":"none","input":[{"name":"src","type":"Group","mode":"source"},{"name":"concept","type":"CodeableConcept","mode":"target"}],"rule":[{"name":"r_cp_cc_cod","source":[{"context":"src"}],"target":[{"context":"concept","contextType":"variable","element":"coding","variable":"coding","transform":"c","parameter":[{"valueString":"https://www.snomed.org"},{"valueString":"35359004"}]}],"rule":[{"name":"r_cp_cod_disp","source":[{"context":"src"}],"target":[{"context":"coding","contextType":"variable","element":"display","transform":"copy","parameter":[{"valueString":"Family"}]}]}]}]}]}"""

    val location = FhirContext.forR4Cached().newJsonParser().parseResource(Location::class.java, locationJson)
    val questionnaire = FhirContext.forR4Cached().newJsonParser().parseResource(Questionnaire::class.java, questionnaireJson)
    val structureMap = FhirContext.forR4Cached().newJsonParser().parseResource(StructureMap::class.java, structureMapJson)

    lifecycleScope.launch(defaultRepository.dispatcherProvider.io()) {
      defaultRepository.addOrUpdate(location)
      defaultRepository.addOrUpdate(questionnaire)
      defaultRepository.addOrUpdate(structureMap)
      Timber.e("Location added")
    }*/

    appMainViewModel.run {
      refreshDataState.value = true
      retrieveAppMainUiState()
    }
  }

  override fun onSync(state: State) {
    Timber.i("Sync state received is $state")
    when (state) {
      is State.Started -> {
        showToast(getString(R.string.syncing))
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, getString(R.string.syncing_initiated))
        )
      }
      is State.InProgress -> {
        Timber.d("Syncing in progress: Resource type ${state.resourceType?.name}")
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, getString(R.string.syncing_in_progress))
        )
      }
      is State.Glitch -> {
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, appMainViewModel.retrieveLastSyncTimestamp())
        )
        Timber.w(state.exceptions.joinToString { it.exception.message.toString() })
      }
      is State.Failed -> {
        showToast(getString(R.string.sync_failed))
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(
            state,
            if (!appMainViewModel.retrieveLastSyncTimestamp().isNullOrEmpty())
              getString(R.string.last_sync_timestamp, appMainViewModel.retrieveLastSyncTimestamp())
            else getString(R.string.syncing_failed)
          )
        )
        Timber.e(state.result.exceptions.joinToString { it.exception.message.toString() })
        scheduleFhirTaskStatusUpdater()
      }
      is State.Finished -> {
        showToast(getString(R.string.sync_completed))
        appMainViewModel.run {
          onEvent(
            AppMainEvent.UpdateSyncState(
              state,
              getString(
                R.string.last_sync_timestamp,
                formatLastSyncTimestamp(state.result.timestamp)
              )
            )
          )
          updateLastSyncTimestamp(state.result.timestamp)
        }
        scheduleFhirTaskStatusUpdater()
      }
    }
  }

  private fun scheduleFhirTaskStatusUpdater() {
    // TODO use sharedpref to save the state
    with(configService) {
      if (true /*registerViewModel.applicationConfiguration.scheduleDefaultPlanWorker*/)
        this.schedulePlan(this@AppMainActivity)
      else this.unschedulePlan(this@AppMainActivity)
    }
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK)
      data?.getStringExtra(QUESTIONNAIRE_BACK_REFERENCE_KEY)?.let {
        lifecycleScope.launch(Dispatchers.IO) {
          when {
            it.startsWith(ResourceType.Task.name) ->
              fhirCarePlanGenerator.completeTask(it.asReference(ResourceType.Task).extractId())
          }
        }
      }
  }
}
