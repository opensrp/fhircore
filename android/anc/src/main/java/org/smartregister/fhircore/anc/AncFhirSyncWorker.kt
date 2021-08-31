package org.smartregister.fhircore.anc

import android.content.Context
import androidx.work.WorkerParameters
import com.google.android.fhir.sync.FhirSyncWorker
import org.smartregister.fhircore.engine.util.extension.buildDatasource

class AncFhirSyncWorker(appContext: Context, workerParams: WorkerParameters) :
  FhirSyncWorker(appContext, workerParams) {

  override fun getSyncData() = AncApplication.getContext().resourceSyncParams

  override fun getDataSource() =
    AncApplication.getContext()
      .buildDatasource(AncApplication.getContext().applicationConfiguration)

  override fun getFhirEngine() = AncApplication.getContext().fhirEngine
}
