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

package org.smartregister.fhircore.engine.data.local.patient.dao.register

import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.patient.dao.register.family.FamilyRegisterDao
import org.smartregister.fhircore.engine.domain.repository.RegisterDao

@Singleton
class RegisterDaoFactory
@Inject
constructor(
  val ancPatientRegisterDao: AncPatientRegisterDao,
  val defaultPatientRegisterDao: DefaultPatientRegisterDao,
  val familyRegisterDao: FamilyRegisterDao
) {

  var registerDaoMap = mutableMapOf<HealthModule, RegisterDao>()
    private set

  init {
    registerDaoMap[HealthModule.ANC] = ancPatientRegisterDao
    registerDaoMap[HealthModule.FAMILY] = familyRegisterDao
    registerDaoMap[HealthModule.DEFAULT] = defaultPatientRegisterDao
  }
}
