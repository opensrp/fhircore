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
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.upload.UploadStrategy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import java.io.File


@HiltWorker
class AppSyncWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val syncListenerManager: SyncListenerManager,
  private val openSrpFhirEngine: FhirEngine,
  private val appTimeStampContext: AppTimeStampContext,
  private val fhirResourceService: FhirResourceService
) : FhirSyncWorker(appContext, workerParams) {

  override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver

  override fun getDownloadWorkManager(): DownloadWorkManager =
    OpenSrpDownloadManager(
      syncParams = syncListenerManager.loadSyncParams(),
      context = appTimeStampContext,
    )

  override suspend fun doWork(): Result {
    val result = super.doWork()

    //val search = Search(type = ResourceType.DocumentReference, count = 10 )
    //val data = openSrpFhirEngine.search<Search>(search)
    //openSrpFhirEngine.search<DocumentReference> {  }


    val file: File = File("filePath")
    val requestBody: RequestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
    val filePart: MultipartBody.Part = MultipartBody.Part.createFormData("file", file.getName(), requestBody)
    //fhirResourceService.uploadDocumentReference("", requestBody)

    return result
  }

  override fun getFhirEngine(): FhirEngine = openSrpFhirEngine

  override fun getUploadStrategy(): UploadStrategy = UploadStrategy.AllChangesSquashedBundlePut
}
