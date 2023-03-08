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

package org.smartregister.fhircore.quest.ui.main

import android.app.Activity
import android.database.SQLException
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.SearchQuery
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.performance.Timer
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.geowidget.model.GeoWidgetEvent
import org.smartregister.fhircore.geowidget.screens.GeoWidgetViewModel
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalMaterialApi
open class AppMainActivity : BaseMultiLanguageActivity(), QuestionnaireHandler, OnSyncListener {

  @Inject lateinit var dispatcherProvider: DefaultDispatcherProvider
  @Inject lateinit var configService: ConfigService
  @Inject lateinit var syncListenerManager: SyncListenerManager
  @Inject lateinit var syncBroadcaster: SyncBroadcaster
  @Inject lateinit var fhirEngine: FhirEngine

  val appMainViewModel by viewModels<AppMainViewModel>()

  val geoWidgetViewModel by viewModels<GeoWidgetViewModel>()

  lateinit var navHostFragment: NavHostFragment

  override val startForResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
      if (activityResult.resultCode == Activity.RESULT_OK) onSubmitQuestionnaire(activityResult)
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(FragmentContainerView(this).apply { id = R.id.nav_host })
    val topMenuConfig = appMainViewModel.navigationConfiguration.clientRegisters.first()
    val topMenuConfigId =
      topMenuConfig.actions?.find { it.trigger == ActionTrigger.ON_CLICK }?.id ?: topMenuConfig.id
    navHostFragment =
      NavHostFragment.create(
        R.navigation.application_nav_graph,
        bundleOf(
          NavigationArg.SCREEN_TITLE to topMenuConfig.display,
          NavigationArg.REGISTER_ID to topMenuConfigId
        )
      )

    supportFragmentManager
      .beginTransaction()
      .replace(R.id.nav_host, navHostFragment)
      .setPrimaryNavigationFragment(navHostFragment)
      .commit()

    geoWidgetViewModel.geoWidgetEventLiveData.observe(this) { geoWidgetEvent ->
      when (geoWidgetEvent) {
        is GeoWidgetEvent.OpenProfile ->
          appMainViewModel.launchProfileFromGeoWidget(
            navHostFragment.navController,
            geoWidgetEvent.geoWidgetConfiguration.id,
            geoWidgetEvent.data
          )
        is GeoWidgetEvent.RegisterClient ->
          appMainViewModel.launchFamilyRegistrationWithLocationId(
            context = this,
            locationId = geoWidgetEvent.data,
            questionnaireConfig = geoWidgetEvent.questionnaire
          )
      }
    }

    // Register sync listener then run sync in that order
    syncListenerManager.registerSyncListener(this, lifecycle)

    // Setup the drawer and schedule jobs
    appMainViewModel.run {
      retrieveAppMainUiState()
      // schedulePeriodicJobs()
      workManager.cancelAllWork()
    }

    runSync(syncBroadcaster)
  }

  override fun onResume() {
    super.onResume()
    syncListenerManager.registerSyncListener(this, lifecycle)

    appMainViewModel.viewModelScope.launch(dispatcherProvider.io()) {
      var total = 0
      var fetches = 0
      var offset = 0

      // do {
      Timber.e("loadResources starting search")
      val startTime = System.currentTimeMillis()
      /*
      val search =
        Search(type = ResourceType.Task).apply {
          filter(
            DateClientParam(SyncDataParams.LAST_UPDATED_KEY),
            {
              value = of(DateTimeType(Date(0)))
              prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS
            }
          )

          sort(DateClientParam(SyncDataParams.LAST_UPDATED_KEY), Order.ASCENDING)
          from = offset
          count = 25
        }

      val tasks = fhirEngine.search<Task>(search)*/

      /*val addDateTimeIndexEntityIndexFromIndexQuery = SearchQuery("CREATE INDEX `index_DateTimeIndexEntity_index_from` ON `DateTimeIndexEntity` (`index_from`)", emptyList())
      fhirEngine.search<Task>(addDateTimeIndexEntityIndexFromIndexQuery)*/

      /*val searchQuery =
        SearchQuery(
          """
            SELECT a.serializedResource, a.resourceUuid
            FROM ResourceEntity a
            JOIN ReferenceIndexEntity b ON a.resourceUuid = b.resourceUuid
            WHERE b.resourceType = 'Task'
            AND b.index_name = 'subject'
            AND b.index_value = 'Patient/29af5cd1-bed0-46b7-b968-92dcf80b6098'
          """.trimIndent(),
          emptyList()
        )

      val tasks = fhirEngine.search<Task>(searchQuery)

      val stopTime = System.currentTimeMillis()
      val timeTaken = stopTime - startTime
      Timber.e("Time taken = ${timeTaken/1000} s | $timeTaken ms")
      offset += 25*/
      // } while (tasks.isNotEmpty())

      // Add index for DateTimeIndexEntity (index_from)
      /*try {
        val addDateTimeIndexEntityIndexFromIndexQuery =
          SearchQuery(
            "CREATE INDEX `index_DateTimeIndexEntity_index_from` ON `DateTimeIndexEntity` (`index_from`)",
            emptyList()
          )
        fhirEngine.search<Task>(addDateTimeIndexEntityIndexFromIndexQuery)
      } catch (ex: SQLException) {
        Timber.e(ex)
      }

      // Add index for ResourceEntity (resourceId)
      try {
        val addDateTimeIndexEntityIndexFromIndexQuery =
          SearchQuery(
            "CREATE INDEX `index_ResourceEntity_resourceId` ON `ResourceEntity` (`resourceId`)",
            emptyList()
          )
        fhirEngine.search<Task>(addDateTimeIndexEntityIndexFromIndexQuery)
      } catch (ex: SQLException) {
        Timber.e(ex)
      }*/
      runQuery<Task>("""
        DROP INDEX IF EXISTS index_DateTimeIndexEntity_index_from
      """.trimIndent(),
        emptyList()
      )
      runQuery<Task>("""
        DROP INDEX IF EXISTS index_ResourceEntity_resourceId
      """.trimIndent(),
        emptyList()
      )

      //

      createAndUpdateRelationalTable()
    }
  }

  suspend fun <T : Resource> runQuery(query: String, args: List<Any>): List<T> {
    val timer = Timer(methodName ="runQuery -> $query")
    return try {
      val searchQuery = SearchQuery(query, args)
        //Timber.e("Running Query $searchQuery")
      val result = fhirEngine.search<T>(searchQuery)
      timer.stop()
      result
    } catch (ex: SQLException) {
      Timber.e(ex)
      timer.stop()
      emptyList()
    }
  }

  suspend fun createAndUpdateRelationalTable() {
      val timer = Timer(methodName = "createAndUpdateRelationalTable")

    runQuery<Task>(
      """
        CREATE TABLE IF NOT EXISTS "RegisterFamilies" (
        	"resourceUuid"	BLOB UNIQUE,
        	"resourceId"	TEXT,
        	"lastUpdated"	INTEGER,
        	"childCount" INTEGER,
        	"taskCount" INTEGER,
        	"taskStatus" TEXT,
        	"pregnantWomenCount" INTEGER,
        	"familyName" TEXT,
        	"householdNo" TEXT,
        	"householdLocation" TEXT,
        	PRIMARY KEY("resourceUuid")
        );
      """.trimIndent(),
      emptyList()
    )
    runQuery<Task>(
      """
        CREATE INDEX "index_RegisterFamilies_lastUpdated" ON "RegisterFamilies" (
        	"lastUpdated"
        );
      """.trimIndent(),
      emptyList()
    )
    runQuery<Task>(
      """
        INSERT INTO RegisterFamilies (resourceUuid, resourceId, lastUpdated)
        SELECT a.resourceUuid, a.resourceId, c.index_from
        FROM ResourceEntity a
        LEFT JOIN DateIndexEntity b
        ON a.resourceType = b.resourceType AND a.resourceUuid = b.resourceUuid AND b.index_name = "_lastUpdated"
        LEFT JOIN DateTimeIndexEntity c
        ON a.resourceType = c.resourceType AND a.resourceUuid = c.resourceUuid AND c.index_name = "_lastUpdated"
        WHERE a.resourceType = "Group"
        AND a.resourceUuid IN (
        SELECT resourceUuid FROM TokenIndexEntity
        WHERE resourceType = "Group" AND index_name = "type" AND (index_value = "person" AND (index_system = "http://hl7.org/fhir/group-type"))
        )
        AND a.resourceUuid IN (
        SELECT resourceUuid FROM TokenIndexEntity
        WHERE resourceType = "Group" AND index_name = "code" AND (index_value = "35359004" AND IFNULL(index_system,'') = "https://www.snomed.org")
        )
        ORDER BY b.index_from DESC, c.index_from DESC
      """.trimIndent(),
      emptyList()
    )
    // Get the families
    val families =
      runQuery<Group>(
        """
      SELECT a.serializedResource
      FROM ResourceEntity a
      LEFT JOIN DateIndexEntity b
      ON a.resourceType = b.resourceType AND a.resourceUuid = b.resourceUuid AND b.index_name = ?
      LEFT JOIN DateTimeIndexEntity c
      ON a.resourceType = c.resourceType AND a.resourceUuid = c.resourceUuid AND c.index_name = ?
      WHERE a.resourceType = ?
      AND a.resourceUuid IN (
      SELECT resourceUuid FROM TokenIndexEntity
      WHERE resourceType = ? AND index_name = ? AND (index_value = ? AND IFNULL(index_system,'') = ?)
      )
      AND a.resourceUuid IN (
      SELECT resourceUuid FROM TokenIndexEntity
      WHERE resourceType = ? AND index_name = ? AND (index_value = ? AND IFNULL(index_system,'') = ?)
      )
    """.trimIndent(),
        listOf(
          "_lastUpdated",
          "_lastUpdated",
          "Group",
          "Group",
          "type",
          "person",
          "http://hl7.org/fhir/group-type",
          "Group",
          "code",
          "35359004",
          "https://www.snomed.org"
        )
      )

    families.forEach { baseResource ->
      val memberUUIDs = mutableListOf<String>()
      var searchQuery =
        SearchQuery(
          """
          SELECT resourceUuid FROM ResourceEntity WHERE resourceType = "Patient" AND resourceId IN (
          SELECT SUBSTR(index_value, 9) FROM ReferenceIndexEntity WHERE index_name = "member" 
          AND resourceUuid = (SELECT resourceUuid FROM ResourceEntity WHERE resourceId = ?)
          )
        """.trimIndent(),
          listOf(baseResource.logicalId)
        )

      memberUUIDs.addAll(fhirEngine.getUUIDs(searchQuery))
      Timber.e("Member UUIDs -> $memberUUIDs")

      searchQuery =
        SearchQuery(
          """
          SELECT resourceUuid FROM ResourceEntity WHERE resourceId = ?
        """.trimIndent(),
          listOf(baseResource.logicalId)
        )

      val groupUUID = fhirEngine.getUUIDs(searchQuery).first()
      searchQuery =
        SearchQuery(
          """
            SELECT COUNT(*) FROM TokenIndexEntity WHERE resourceType = "Task" AND index_name = "status" 
            AND resourceUuid IN (
            SELECT resourceUuid FROM ReferenceIndexEntity WHERE resourceType = "Task" AND index_name = "subject" 
            AND index_value IN (SELECT index_value FROM ReferenceIndexEntity WHERE resourceUuid = x'$groupUUID' AND index_name = "member")
            ) 
            AND (index_value = "failed" OR index_value = "completed" OR index_value = "cancelled")

          """.trimIndent(),
          emptyList()
        )

      val taskCount = fhirEngine.count(searchQuery)
      searchQuery =
        SearchQuery(
          """
            SELECT COUNT(*) FROM TokenIndexEntity WHERE resourceType = "Condition" 
            AND index_name = "code" AND index_system = "http://snomed.info/sct" AND index_value = "77386006" 
            AND resourceUuid IN (SELECT resourceUuid FROM ReferenceIndexEntity WHERE resourceType = "Condition" 
            AND index_name = "subject" AND index_value IN (SELECT index_value FROM ReferenceIndexEntity WHERE index_name = "member" AND resourceUuid = x'$groupUUID') )
          """.trimIndent(),
          emptyList()
        )

      val pregnantWomenCount = fhirEngine.count(searchQuery)
      val birthDate = LocalDate.now().minusYears(5).toEpochDay()
      val memberSelector = genMemberUuidsSelector(memberUUIDs)
      searchQuery =
        SearchQuery(
          """
            SELECT COUNT(*) FROM TokenIndexEntity a JOIN DateIndexEntity b ON a.resourceUuid = b.resourceUuid  
            WHERE a.resourceUuid IN ($memberSelector) AND a.index_name = "active" AND a.index_value = "true" 
            AND b.index_name = "birthdate" AND b.index_from >= ?
          """.trimIndent(),
          listOf(birthDate)
        )
      val childrenCount = fhirEngine.count(searchQuery)

      runQuery<Task>(
        """
        UPDATE RegisterFamilies SET childCount = ?, taskCount = ?, pregnantWomenCount = ?, familyName = ?, householdNo = ?, householdLocation = ?
        WHERE resourceUuid = x'$groupUUID'
      """.trimIndent(),
        listOf(childrenCount, taskCount, pregnantWomenCount, baseResource.name, baseResource.identifier[0].value, baseResource.characteristic[0].code.text)
      )
    }

      timer.stop()
  }

  fun genMemberUuidsSelector(memberUuids: MutableList<String>): String {
    return memberUuids.map { "x'$it'" }.joinToString(separator = ",")
  }

  override fun onSubmitQuestionnaire(activityResult: ActivityResult) {
    if (activityResult.resultCode == RESULT_OK) {
      val questionnaireResponse: QuestionnaireResponse? =
        activityResult.data?.getSerializableExtra(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE) as
          QuestionnaireResponse?
      val questionnaireConfig =
        activityResult.data?.getSerializableExtra(QuestionnaireActivity.QUESTIONNAIRE_CONFIG) as
          QuestionnaireConfig?

      if (questionnaireConfig != null && questionnaireResponse != null) {
        appMainViewModel.questionnaireSubmissionLiveData.postValue(
          QuestionnaireSubmission(questionnaireConfig, questionnaireResponse)
        )
      }
    }
  }

  override fun onSync(syncJobStatus: SyncJobStatus) {
    when (syncJobStatus) {
      is SyncJobStatus.InProgress -> {
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(syncJobStatus, getString(R.string.syncing_in_progress))
        )
      }
      is SyncJobStatus.Glitch -> {
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(syncJobStatus, appMainViewModel.retrieveLastSyncTimestamp())
        )
        // syncJobStatus.exceptions may be null when worker fails; hence the null safety usage
        Timber.w(syncJobStatus?.exceptions?.joinToString { it.exception.message.toString() })
      }
      is SyncJobStatus.Finished, is SyncJobStatus.Failed -> {
        appMainViewModel.run {
          onEvent(
            AppMainEvent.UpdateSyncState(
              syncJobStatus,
              formatLastSyncTimestamp(syncJobStatus.timestamp)
            )
          )
        }
      }
      else -> {
        /*Do nothing */
      }
    }
  }

  private fun runSync(syncBroadcaster: SyncBroadcaster) {
    syncBroadcaster.run {
      if (isDeviceOnline()) {
        with(appMainViewModel.syncSharedFlow) {
          runSync(this)
          schedulePeriodicSync(this)
        }
      }
    }
  }
}
