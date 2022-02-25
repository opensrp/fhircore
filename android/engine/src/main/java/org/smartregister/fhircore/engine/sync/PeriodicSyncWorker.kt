package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.sync.DataSource
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.ResourceSyncParams
import com.google.gson.GsonBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirConverterFactory
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.di.CommonModule
import org.smartregister.fhircore.engine.di.EngineModule
import org.smartregister.fhircore.engine.di.NetworkModule
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

class PeriodicSyncWorker(val context: Context, workerParams: WorkerParameters):
    FhirSyncWorker(context, workerParams) {
    val networkModule = NetworkModule()
    val engineModule = EngineModule()
    val commonModule = CommonModule()

    val parser = networkModule.provideParser()
    val accountManager = networkModule.provideApplicationManager(context)
    val secureSharedPreference = commonModule.provideSecureSharedPreference(context)
    val tokenManagerService = networkModule.provideTokenManagerService(context, accountManager, secureSharedPreference)
    val interceptor = networkModule.provideOAuthInterceptor(context, tokenManagerService)
    val okHttpClient = networkModule.provideOkHttpClient(interceptor)

    override fun getDataSource(): DataSource =
        FhirResourceDataSource(networkModule.provideFhirResourceService(
            parser = parser, okHttpClient = okHttpClient, gson = networkModule.provideGson()
        ))

    override fun getFhirEngine(): FhirEngine = engineModule.provideFhirEngine(context)

    //TODO????????????????????
    override fun getSyncData(): ResourceSyncParams = mapOf(
        ResourceType.Patient to emptyMap(),
        ResourceType.Immunization to emptyMap(),
        ResourceType.Questionnaire to emptyMap(),
    )
}