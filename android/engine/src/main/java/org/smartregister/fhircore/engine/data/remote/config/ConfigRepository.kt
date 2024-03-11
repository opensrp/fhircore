package org.smartregister.fhircore.engine.data.remote.config

import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.AppConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import javax.inject.Inject

class ConfigRepository @Inject constructor(
    private val appConfigService: AppConfigService,
    private val fhirResourceDataSource: FhirResourceDataSource,
    private val defaultRepository: DefaultRepository
) {
    suspend fun fetchConfigFromRemote() {
        fhirResourceDataSource.search(
            ResourceType.Binary.name, mapOf(
                Pair("_id", appConfigService.getAppId())
            )
        ).entry.forEach {
            defaultRepository.save(it.resource)
        }
    }
}