package org.smartregister.fhircore.quest

import org.smartregister.fhircore.engine.configuration.app.AppConfigService
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class QuestAppConfigService @Inject constructor(): AppConfigService {
    override fun getAppId(): String {
        return BuildConfig.APP_ID
    }

    override fun getBaseFhirUrl(): String {
        return BuildConfig.FHIR_BASE_URL
    }
}