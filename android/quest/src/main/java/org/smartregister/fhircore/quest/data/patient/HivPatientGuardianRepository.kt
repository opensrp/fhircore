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

package org.smartregister.fhircore.quest.data.patient

import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.di.HivPatient
import org.smartregister.fhircore.engine.domain.repository.PatientDao
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

class HivPatientGuardianRepository
@Inject
constructor(
  @HivPatient private val patientDao: PatientDao,
  private val dispatcherProvider: DefaultDispatcherProvider
) {

  suspend fun loadPatient(patientId: String) =
    withContext(dispatcherProvider.io()) { patientDao.loadPatient(patientId) }

  suspend fun loadGuardianRegisterData(patient: Patient) =
    withContext(dispatcherProvider.io()) { patientDao.loadGuardiansRegisterData(patient) }
}
