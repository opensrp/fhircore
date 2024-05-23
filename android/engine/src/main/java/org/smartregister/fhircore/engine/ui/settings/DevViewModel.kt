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

package org.smartregister.fhircore.engine.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.appointment.MissedFHIRAppointmentsWorker
import org.smartregister.fhircore.engine.appointment.ProposedWelcomeServiceAppointmentsWorker
import org.smartregister.fhircore.engine.auditEvent.AuditEventWorker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.purger.ResourcePurgerWorker
import org.smartregister.fhircore.engine.domain.util.DataLoadState
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirTaskPlanWorker
import org.smartregister.fhircore.engine.task.WelcomeServiceBackToCarePlanWorker
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import timber.log.Timber

@ExcludeFromJacocoGeneratedReport
@HiltViewModel
class DevViewModel
@Inject
constructor(
  val syncBroadcaster: SyncBroadcaster,
  val accountAuthenticator: AccountAuthenticator,
  val secureSharedPreference: SecureSharedPreference,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configurationRegistry: ConfigurationRegistry,
  val fhirEngine: FhirEngine,
  @ApplicationContext val appContext: Context,
) : ViewModel() {

  val resourceSaveState = MutableStateFlow<DataLoadState<Boolean>>(DataLoadState.Idle)

  suspend fun createResourceReport(context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        resourceSaveState.value = DataLoadState.Loading
        val generalReport =
          File(context.filesDir, "general.txt").also {
            generateReport(it, generateGeneralResource())
          }
        val resourceReport =
          File(context.filesDir, "resource.txt").also { generateReport(it, getResourcesToReport()) }
        val localChanges =
          File(context.filesDir, "changes.txt").also {
            generateReport(it, getLocalResourcesReport())
          }

        val zipFile = File(context.filesDir, "report.zip")

        zipReports(zipFile, listOf(generalReport, resourceReport, localChanges))
        val zipFileUri =
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/x-zip"
        shareIntent.putExtra(Intent.EXTRA_STREAM, zipFileUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val chooser = Intent.createChooser(shareIntent, "Share Log Data")

        if (shareIntent.resolveActivity(context.packageManager) != null) {
          context.startActivity(chooser)
        }
        resourceSaveState.value = DataLoadState.Success(true)
      } catch (e: Exception) {
        Timber.e(e)
        resourceSaveState.value = DataLoadState.Error(e)
      }
    }
  }

  private suspend fun zipReports(zipFile: File, files: List<File>) {
    withContext(Dispatchers.IO) {
      ZipOutputStream(BufferedOutputStream(zipFile.outputStream())).use { out ->
        for (file in files) {
          FileInputStream(file).use { fi ->
            BufferedInputStream(fi).use { origin ->
              val entry = ZipEntry(file.name)
              out.putNextEntry(entry)
              origin.copyTo(out, 1024)
            }
          }
        }
      }
    }
  }

  private suspend fun generateReport(file: File, value: String) {
    withContext(Dispatchers.IO) {
      val fileWriter = FileWriter(file)
      fileWriter.write(value)
      fileWriter.close()
    }
  }

  private suspend fun getResourcesToReport(): String {
    val data = getResourcesToVersions()
    var log = ""

    data.entries.forEach { group ->
      log =
        "${log}Id,${group.key},Date" +
          "\n" +
          group.value.joinToString(separator = "\n") @ExcludeFromJacocoGeneratedReport {
            "${it.id},${it.version},${it.date}"
          } +
          "" +
          "\n-----------------------------------------------------\n"
    }
    return log
  }

  private suspend fun getLocalResourcesReport(): String {
    val changes = fhirEngine.getUnsyncedLocalChanges()

    val raw =
      changes.joinToString {
        """
        |_______________________________
        |resourceType: ${it.resourceType},
        |resourceId: ${it.resourceId},
        |versionId: ${it.versionId},
        |timestamp: ${it.timestamp},
        |type: ${it.resourceType},
        |token: "${it.token.ids}",
        |payload: "${it.payload}",
        |_______________________________
        |
            """
          .trimMargin()
      }

    return "\nChanges: ${changes.size}\n$raw"
  }

  private suspend fun generateGeneralResource(): String {
    val all = sharedPreferencesHelper.prefs.all

    val pref =
      all.entries.joinToString {
        """
        ----------Prefs-------
        ${it.key}: "${it.value}"
        ----------------------
            """
          .trimIndent()
      }

    val workers =
      listOf(
          FhirTaskPlanWorker.WORK_ID,
          MissedFHIRAppointmentsWorker.NAME,
          ProposedWelcomeServiceAppointmentsWorker.NAME,
          ResourcePurgerWorker.NAME,
          AuditEventWorker.NAME,
          WelcomeServiceBackToCarePlanWorker.NAME,
          "${SyncBroadcaster::class.java.name}-oneTimeSync",
          "${SyncBroadcaster::class.java.name}-periodicSync",
          SyncBroadcaster::class.java.name,
        )
        .mapNotNull { tag ->
          WorkManager.getInstance(appContext)
            .getWorkInfosByTag(tag)
            .get()
            ?.let { list ->
              list.joinToString { info ->
                """-----$tag-----
              |Name: ${info.state.name}, 
              |Data: {${info.outputData.keyValueMap.entries.joinToString { "${it.key}: ${it.value}" }}}
              |Tag: ${info.tags}
              |Attempts: ${info.runAttemptCount}
              |
                    """
                  .trimMargin()
              }
            }
            ?.ifBlank { null }
        }

    return """
      |$pref
      |-------- workers --------
      |$workers
      |-------------------------
      |
            """
      .trimMargin()
  }

  suspend fun getResourcesToVersions(): Map<String, List<ResourceField>> {
    return mapOf(
      Pair("Questionnaire", getAll<Questionnaire>()),
      Pair("StructureMap", getAll<StructureMap>()),
      Pair("Binary", getAll<Binary>()),
    )
  }

  private suspend inline fun <reified T : Resource> getAll(): List<ResourceField> {
    return fhirEngine
      .search<T> {}
      .map { it.resource }
      .map { ResourceField(it.logicalId, it.meta.versionId, it.meta.lastUpdated.asDdMmmYyyy()) }
  }

  fun missedTask(context: Context) {
    viewModelScope.launch {
      WorkManager.getInstance(context)
        .enqueueUniqueWork(
          FhirTaskPlanWorker.WORK_ID,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<FhirTaskPlanWorker>().build(),
        )
    }
  }

  fun missedAppointment(context: Context) {
    viewModelScope.launch {
      WorkManager.getInstance(context)
        .enqueueUniqueWork(
          MissedFHIRAppointmentsWorker.NAME,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<MissedFHIRAppointmentsWorker>().build(),
        )
    }
  }

  fun interruptedResource(context: Context) {
    viewModelScope.launch {
      WorkManager.getInstance(context)
        .enqueueUniqueWork(
          ProposedWelcomeServiceAppointmentsWorker.NAME,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<ProposedWelcomeServiceAppointmentsWorker>().build(),
        )
    }
  }

  fun resourcePurger(context: Context) {
    viewModelScope.launch {
      WorkManager.getInstance(context)
        .enqueueUniqueWork(
          ResourcePurgerWorker.NAME,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<ResourcePurgerWorker>()
            .setInputData(
              Data.Builder().putBoolean(ResourcePurgerWorker.ONE_TIME_SYNC_KEY, true).build(),
            )
            .build(),
        )
    }
  }

  fun observeMissedAppointment(context: Context): Flow<List<WorkInfo.State>> {
    return WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkFlow(MissedFHIRAppointmentsWorker.NAME)
      .map { list -> list.map { it.state } }
  }

  fun observeMissedTask(context: Context): Flow<List<WorkInfo.State>> {
    return WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkFlow(FhirTaskPlanWorker.WORK_ID)
      .map { list -> list.map { it.state } }
  }

  fun observeInterrupted(context: Context): Flow<List<WorkInfo.State>> {
    return WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkFlow(ProposedWelcomeServiceAppointmentsWorker.NAME)
      .map { list -> list.map { it.state } }
  }

  fun observeResourcePurgerWorker(context: Context): Flow<List<WorkInfo.State>> {
    return WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkFlow(ResourcePurgerWorker.NAME)
      .map { list -> list.map { it.state } }
  }
}

@ExcludeFromJacocoGeneratedReport
data class ResourceField(val id: String, val version: String, val date: String)
