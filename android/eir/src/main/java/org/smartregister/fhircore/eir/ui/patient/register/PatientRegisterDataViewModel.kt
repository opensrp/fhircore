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

package org.smartregister.fhircore.eir.ui.patient.register

import android.app.Application
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.data.local.repository.model.PatientItem
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientPaginatedDataSource
import org.smartregister.fhircore.engine.ui.register.BaseRegisterDataViewModel

class PatientRegisterDataViewModel(
  application: Application,
  paginatedDataSource: PatientPaginatedDataSource,
  pageSize: Int = 50
) :
  BaseRegisterDataViewModel<Pair<Patient, List<Immunization>>, PatientItem>(
    application = application,
    paginatedDataSource = paginatedDataSource,
    pageSize = pageSize
  )
