package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import retrofit2.HttpException
import java.net.UnknownHostException

@HiltWorker
class CustomResourceSyncWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    val configurationRegistry: ConfigurationRegistry,
    val dispatcherProvider: DispatcherProvider,
    val defaultRepository: DefaultRepository,
    val fhirResourceDataSource: FhirResourceDataSource
) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(dispatcherProvider.io()) {
            try {
                val urlList = configurationRegistry.fetchURLForCustomResource()
                val bundle = fhirResourceDataSource.getResource(urlList.first())
                //defaultRepository.addOrUpdate()
                Result.success()
            } catch (httpException: HttpException) {
                Result.failure()
            } catch (unknownHostException: UnknownHostException) {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_ID = "CustomResourceSyncWorker"
    }
}