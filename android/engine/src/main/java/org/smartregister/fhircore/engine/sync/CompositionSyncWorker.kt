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

package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.upload.UploadStrategy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

@HiltWorker
class CompositionSyncWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val openSrpFhirEngine: FhirEngine,
  private val appTimeStampContext: AppTimeStampContext,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  private val syncParamSource: SyncParamSource
) : FhirSyncWorker(appContext, workerParams) {

  override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver

  override fun getDownloadWorkManager(): DownloadWorkManager =
    OpenSrpDownloadManager(
      syncParams = loadConfigSyncParams(),
      context = appTimeStampContext,
    )

  override fun getFhirEngine(): FhirEngine = openSrpFhirEngine

  override fun getUploadStrategy(): UploadStrategy = UploadStrategy.AllChangesSquashedBundlePut

  private fun loadConfigSyncParams(): Map<ResourceType, Map<String, String>> {

    Timber.d("#### loadCompositionSyncParams")
//    return syncParamSource.configSyncParameters
    val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()

      // val urlPath =
      // "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId&_count=${ConfigurationRegistry.DEFAULT_COUNT}"
    sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.let { appId ->
      val parsedAppId = appId.substringBefore(ConfigurationRegistry.TYPE_REFERENCE_DELIMITER).trim()
      pairs.add(
        Pair(
          ResourceType.Composition,
          mapOf(Composition.SP_IDENTIFIER to parsedAppId),
        ),
      )
      pairs.add(
        Pair(
          ResourceType.Composition,
          mapOf("_count" to ConfigurationRegistry.DEFAULT_COUNT.toString()),
        ),
      )
    }

    // GET /StructureMap?_count=37
    return mapOf(*pairs.toTypedArray())
  }
}
