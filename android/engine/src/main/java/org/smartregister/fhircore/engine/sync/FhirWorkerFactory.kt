package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import javax.inject.Inject
import javax.inject.Singleton

class FhirWorkerFactory(
    val fhirEngine: FhirEngine,
    val fhirResourceDataSource: FhirResourceDataSource,
    val configService: ConfigService
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker = PeriodicSyncWorker(appContext, workerParameters, )
}