/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.configuration

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.appfeature.model.AppFeatureConfig
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.AppConfigService
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.view.DataFiltersConfiguration
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A configuration store used to store all the application configurations. Application
 * configurations are to be downloaded and synced from the server. This registry provides a map with
 * different [Configuration] implementations. The ensures that all the application configurations
 * are accessible from one place. If no configurations are retrieved from the server, then the
 * defaults are used.
 */
@Singleton
class ConfigurationRegistry
@Inject
constructor(
    @ApplicationContext val context: Context,
    val fhirEngine: FhirEngine,
    val fhirResourceDataSource: FhirResourceDataSource,
    val sharedPreferencesHelper: SharedPreferencesHelper,
    val dispatcherProvider: DispatcherProvider,
    private val appConfigService: AppConfigService
) {
    private var applicationConfiguration = MutableStateFlow<AppConfiguration?>(null)

    suspend fun loadConfigurations(jsonSerializer: Json? = null): Boolean {
        return try {
            val binary = getBinary(appConfigService.getAppId()).content.decodeToString()
            val config = binary.decodeJson<AppConfiguration>(jsonSerializer)
            applicationConfiguration.value = config
            true
        } catch (ex: ResourceNotFoundException) {
            false
        }
    }

    fun getAppConfigs(): ApplicationConfiguration = applicationConfiguration.value?.appConfig!!
    fun getAppFeatureConfigs() : AppFeatureConfig? = applicationConfiguration.value?.appFeatures
    fun getSyncConfigs() : SyncConfig? = applicationConfiguration.value?.syncConfig
    fun getFormConfigs() : List<QuestionnaireConfig>? = applicationConfiguration.value?.formConfigs
    private suspend fun getBinary(id: String): Binary = fhirEngine.get(id)

    companion object {
        const val ORGANIZATION = "organization"
        const val PUBLISHER = "publisher"
        const val ID = "_id"
        const val COUNT = "count"
        const val DEFAULT_COUNT = "100"
        const val DEFAULT_TASK_FILTER_TAG_META_CODING_SYSTEM =
            "https://d-tree.org/fhir/task-filter-tag"
        const val DEFAULT_TASK_ORDER_FILTER_TAG_META_CODING_SYSTEM = "https://d-tree.org"
    }
}
