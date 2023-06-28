package org.dtree.fhircore.dataclerk

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.fhir.datacapture.DataCaptureConfig
import dagger.hilt.android.HiltAndroidApp
import org.dtree.fhircore.dataclerk.data.QuestXFhirQueryResolver
import org.smartregister.fhircore.engine.data.remote.fhir.resource.ReferenceUrlResolver
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireItemViewHolderFactoryMatchersProviderFactoryImpl
import javax.inject.Inject

@HiltAndroidApp
class DataClerkApplication : Application(), DataCaptureConfig.Provider, Configuration.Provider {
    private var configuration: DataCaptureConfig? = null

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var referenceUrlResolver: ReferenceUrlResolver

    @Inject
    lateinit var xFhirQueryResolver: QuestXFhirQueryResolver

    override fun getDataCaptureConfig(): DataCaptureConfig {
        configuration =
                configuration
                        ?: DataCaptureConfig(
                                urlResolver = referenceUrlResolver,
                                xFhirQueryResolver = xFhirQueryResolver,
                                questionnaireItemViewHolderFactoryMatchersProviderFactory =
                                QuestionnaireItemViewHolderFactoryMatchersProviderFactoryImpl
                        )
        return configuration as DataCaptureConfig
    }

    override fun getWorkManagerConfiguration(): Configuration =
            Configuration.Builder()
                    .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.VERBOSE else Log.INFO)
                    .setWorkerFactory(workerFactory)
                    .build()
}