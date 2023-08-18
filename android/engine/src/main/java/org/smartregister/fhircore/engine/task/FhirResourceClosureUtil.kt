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

package org.smartregister.fhircore.engine.task

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.event.EventType
import org.smartregister.fhircore.engine.data.local.DefaultRepository

@Singleton
class FhirResourceClosureUtil
@Inject
constructor(
  @ApplicationContext val appContext: Context,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
) {

  suspend fun closeRelatedResources(resource: Resource) {
    val appRegistry =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)

    appRegistry.eventWorkflows
      .filter { it.eventType == EventType.RESOURCE_CLOSURE }
      .forEach { eventWorkFlow ->
        eventWorkFlow.eventResources.forEach { eventResource ->
          defaultRepository.updateResourcesRecursively(eventResource, resource)
        }
      }
  }
}
