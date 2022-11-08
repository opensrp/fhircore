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

package org.smartregister.fhircore.engine.data.local.register.dao

import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Search
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@Singleton
class HomeTracingRegisterDao
@Inject
constructor(
  fhirEngine: FhirEngine,
  defaultRepository: DefaultRepository,
  configurationRegistry: ConfigurationRegistry,
  dispatcherProvider: DefaultDispatcherProvider
) : TracingRegisterDao(fhirEngine, defaultRepository, configurationRegistry, dispatcherProvider) {
  override fun Search.registerFilters() {
    val coding = Coding("https://d-tree.org", "home-tracing", "Home Tracing")
    filter(TokenClientParam("_tag"), { value = of(coding) })
    filter(
      Task.STATUS,
      { value = of(Task.TaskStatus.READY.toCode()) },
      { value = of(Task.TaskStatus.INPROGRESS.toCode()) },
      operation = Operation.OR
    )
  }
}
