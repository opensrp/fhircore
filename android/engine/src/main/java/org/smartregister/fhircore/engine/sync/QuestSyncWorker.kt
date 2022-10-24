package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class QuestSyncWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val syncListenerManager: SyncListenerManager,
  val questFhirEngine: FhirEngine
) : FhirSyncWorker(appContext, workerParams) {

  override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver

  override fun getDownloadWorkManager(): DownloadWorkManager =
    ResourceParamsBasedDownloadWorkManager(syncParams = syncListenerManager.loadSyncParams())

  override fun getFhirEngine(): FhirEngine = questFhirEngine
}
