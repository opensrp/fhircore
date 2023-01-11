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

import com.google.android.fhir.FhirEngine
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.TracingUtil

@Singleton
class PhoneTracingRegisterDao
@Inject
constructor(
  fhirEngine: FhirEngine,
  defaultRepository: DefaultRepository,
  configurationRegistry: ConfigurationRegistry,
  dispatcherProvider: DefaultDispatcherProvider,
  tracingUtil: TracingUtil
) : TracingRegisterDao(fhirEngine, defaultRepository, configurationRegistry, dispatcherProvider,tracingUtil) {

  override val tracingCoding: Coding =
    Coding("https://d-tree.org", "phone-tracing", "Phone Tracing")
}
