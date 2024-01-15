/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.data

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.search.Search
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.migration.DataMigrationConfiguration
import org.smartregister.fhircore.engine.configuration.migration.MigrationConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.datastore.PreferencesDataStore
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import timber.log.Timber

/**
 * The main functionalities of the DataMigration class are:
 * - Migrating data by updating resources based on a set of data queries and key-value
 *   configurations.
 * - Retrieving the latest migration version from shared preferences.
 * - Performing the migration only if the new version is greater than or equal to the latest
 *   migration version.
 */
@Singleton
class DataMigration
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val preferencesDataStore: PreferencesDataStore,
  val parser: IParser,
  val dispatcherProvider: DispatcherProvider,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
  val eventBus: EventBus,
) {

  private var conf: Configuration =
    Configuration.defaultConfiguration().apply { addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL) }

  fun migrate() {
    val migrations =
      try {
        configurationRegistry
          .retrieveConfiguration<DataMigrationConfiguration>(configType = ConfigType.DataMigration)
          .migrations
      } catch (exception: NoSuchElementException) {
        emptyList()
      }

    runBlocking {
      val previousVersion = preferencesDataStore.readOnce(PreferencesDataStore.MIGRATION_VERSION)
      val newMigrations = migrations?.filter { it.version > previousVersion }
      migrate(newMigrations, previousVersion)
    }
  }

  /**
   * This function is responsible for migrating data by updating the resources based on the provided
   * data queries and key-value configurations. It retrieves the latest migration version from
   * shared preferences and performs the migration only if the new version is greater than or equal
   * to the latest migration version.
   *
   * This function uses JSONPath instead of FHIRPath because JSONPath allows setting of values for a
   * given path. The syntax is almost similar to FHIRPath with a few exceptions. JSONPath uses the
   * '$' to refer to the root of the JSON unlike FHIRPath that uses the [ResourceType] .The
   * implementation uses both '$' and '[ResourceType]', the [ResourceType] will be replaced with a
   * dollar sign at runtime.
   *
   * Examples: Given this Patient object:
   * ```
   * {
   *   "resourceType": "Patient",
   *   "id": "example",
   *   "name": [
   *     {
   *       "use": "official",
   *       "given": [
   *         "Peter",
   *         "James"
   *       ],
   *       "family": "Chalmers"
   *     }
   *   ],
   *   "birthDate": "1974-12-25",
   *   "deceased": {
   *     "boolean": false
   *   },
   *   "active": true,
   *   "gender": "male"
   * }
   * ```
   *
   * Valid expressions (format in key, value -> description) include:
   * ```
   * "$.name[0].use", "casual" -> Update the first name usage from "official" to "casual"
   * "Patient.gender", "male" -> Update the gender from "female" to "male"
   * "$.birthDate", "1996-12-32" -> Update the birth date from "1974-12-25" to "1996-12-32"
   * ```
   */
  suspend fun migrate(migrationConfigs: List<MigrationConfig>?, previousVersion: Int) {
    eventBus.triggerEvent(AppEvent.OnMigrateData(true))
    val maxVersion = migrationConfigs?.maxOfOrNull { it.version } ?: previousVersion
    migrationConfigs?.forEach { migrationConfig ->
      try {
        val search: Search =
          Search(type = migrationConfig.resourceType).apply {
            migrationConfig.dataQueries?.forEach { dataQuery ->
              filterBy(dataQuery = dataQuery, configComputedRuleValues = emptyMap())
            }
          }
        val resources =
          withContext(dispatcherProvider.io()) { defaultRepository.search<Resource>(search) }
        resources.forEach { resource ->
          val jsonParse = JsonPath.using(conf).parse(resource.encodeResourceToString())

          val updatedResourceDocument =
            jsonParse.apply {
              migrationConfig.updateValues.forEach { updateExpression ->
                // Expression stars with '$' (JSONPath) or ResourceType like in FHIRPath
                val value = computeValueRule(updateExpression.valueRule, resource)
                if (updateExpression.jsonPathExpression.startsWith("\$") && value != null) {
                  set(updateExpression.jsonPathExpression, value)
                }
                if (
                  updateExpression.jsonPathExpression.startsWith(
                    resource.resourceType.name,
                    ignoreCase = true,
                  ) && value != null
                ) {
                  set(
                    updateExpression.jsonPathExpression.replace(resource.resourceType.name, "\$"),
                    value,
                  )
                }
              }
            }

          val resourceDefinition: Class<out IBaseResource>? =
            FhirContext.forR4Cached().getResourceDefinition(resource).implementingClass

          val updatedResource =
            parser.parseResource(resourceDefinition, updatedResourceDocument.jsonString())
          withContext(dispatcherProvider.io()) {
            defaultRepository.addOrUpdate(resource = updatedResource as Resource)
          }
        }
        eventBus.triggerEvent(AppEvent.OnMigrateData(false))
      } catch (throwable: Throwable) {
        Timber.e(throwable)
      }
    }
    preferencesDataStore.write(PreferencesDataStore.MIGRATION_VERSION, maxVersion.plus(1))
  }

  private fun computeValueRule(valueRule: RuleConfig, resource: Resource): Any? {
    return resourceDataRulesExecutor
      .computeResourceDataRules(
        ruleConfigs = listOf(valueRule),
        repositoryResourceData = RepositoryResourceData(resource = resource),
        params = emptyMap(),
      )[valueRule.name]
  }
}
