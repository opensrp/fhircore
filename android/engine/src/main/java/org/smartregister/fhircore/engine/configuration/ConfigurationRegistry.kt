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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeJson

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
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configService: ConfigService
) {

  val configurationsMap = mutableMapOf<String, Configuration>()

  val workflowPointsMap = mutableMapOf<String, WorkflowPoint>()

  val authConfiguration by lazy { configService.provideAuthConfiguration() }

  lateinit var appId: String

  /**
   * Retrieve configuration for the provided [ConfigClassification]. Populate the map when the
   * config is loaded for the first time. File name containing configs MUST start with the workflow
   * resource in snake_case
   *
   * E.g. for a workflow resource RegisterViewConfiguration, the name of the file containing configs
   * becomes register_view_configurations.json
   */
  inline fun <reified C : Configuration> retrieveConfiguration(
    configClassification: ConfigClassification,
    jsonSerializer: Json? = null
  ): C {

    val workflowPointName = workflowPointName(configClassification.classification)
    val isApplicationConfig =
      configClassification is AppConfigClassification &&
        configClassification.name == AppConfigClassification.APPLICATION.name
    val viewConfigDir = "configurations/view"

    return configurationsMap.getOrPut(workflowPointName) {
      context.assets.run {
        val configurationFilePath =
          if (isApplicationConfig) {
            APP_CONFIG_FILE
          } else {
            val workflowPoint = workflowPointsMap.getValue(workflowPointName)
            val viewConfigurationPaths = list(viewConfigDir)
            viewConfigurationPaths?.find {
              it.replace("_", "").startsWith(workflowPoint.resource, ignoreCase = true)
            }
              ?: throw Error(
                """
                Provide configurations file for resource ${workflowPoint.resource}. 
                File name MUST start with the resource name in snake_case
                E.g for RegisterViewConfiguration -> register_view_configurations.json
               """
              )
          }

        val content =
          open(
            if (isApplicationConfig) configurationFilePath
            else "$viewConfigDir/$configurationFilePath"
          )
            .bufferedReader()
            .use { it.readText() }

        val configuration =
          content.decodeJson<List<C>>(jsonSerializer).first {
            it.appId.equals(other = appId, ignoreCase = true) &&
              it.classification.equals(
                other = configClassification.classification,
                ignoreCase = true
              )
          }
        configuration
      }
    } as
      C
  }

  fun loadAppConfigurations(
    appId: String,
    accountAuthenticator: AccountAuthenticator,
    configsLoadedCallback: (Boolean) -> Unit
  ) {
    // TODO Download configurations that do not require login at this point. Default to assets
    this.appId = appId
    val applicationWorkflowsMap =
      context
        .assets
        .open(APP_WORKFLOW_CONFIG_FILE)
        .bufferedReader()
        .use { it.readText() }
        .decodeJson<List<ApplicationWorkflow>>()
        .associateBy { it.appId }

    if (applicationWorkflowsMap.containsKey(appId)) {
      applicationWorkflowsMap[appId]?.mapping
        ?.associateBy { workflowPointName(it.classification) }
        ?.also {
          workflowPointsMap.clear()
          workflowPointsMap.putAll(it)
        }

      retrieveConfiguration<ApplicationConfiguration>(AppConfigClassification.APPLICATION)
      accountAuthenticator.launchLoginScreen()
      configsLoadedCallback(true)
    } else {
      configsLoadedCallback(false)
    }
  }

  fun workflowPointName(key: String) = "$appId|$key"

  fun isAppIdInitialized() = this::appId.isInitialized

  companion object {
    private const val APP_WORKFLOW_CONFIG_FILE = "configurations/app/application_workflow.json"
    const val APP_CONFIG_FILE = "configurations/app/application_configurations.json"
    const val APP_SYNC_CONFIG = "configurations/app/sync_config.json"
    const val ORGANIZATION = "organization"
    const val PUBLISHER = "publisher"
    const val ID = "_id"
  }
}
