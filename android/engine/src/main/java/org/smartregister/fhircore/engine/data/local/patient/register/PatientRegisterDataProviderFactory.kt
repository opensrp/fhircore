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

package org.smartregister.fhircore.engine.data.local.patient.register

import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.data.local.patient.register.dataprovider.AncRegisterDataProvider
import org.smartregister.fhircore.engine.data.local.patient.register.dataprovider.DefaultRegisterDataProvider
import org.smartregister.fhircore.engine.data.local.patient.register.dataprovider.EirRegisterDataProvider
import org.smartregister.fhircore.engine.data.local.patient.register.dataprovider.FamilyRegisterDataProvider

@Singleton
data class PatientRegisterDataProviderFactory
@Inject
constructor(
  val ancRegisterDataProvider: AncRegisterDataProvider,
  val defaultRegisterDataProvider: DefaultRegisterDataProvider,
  val eirRegisterDataProvider: EirRegisterDataProvider,
  val familyRegisterDataProvider: FamilyRegisterDataProvider
)
