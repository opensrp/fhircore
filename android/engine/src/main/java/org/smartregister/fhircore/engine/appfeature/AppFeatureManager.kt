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

package org.smartregister.fhircore.engine.appfeature

import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.appfeature.model.AppFeatureConfig
import org.smartregister.fhircore.engine.appfeature.model.FeatureConfig
import org.smartregister.fhircore.engine.util.extension.decodeJson

@Singleton
class AppFeatureManager @Inject constructor() {

  private var _appFeatureConfig: AppFeatureConfig? = null

  // TODO read from saved binary resource
  fun loadAndActivateFeatures() {
    _appFeatureConfig =
      """{"appId":"ecbis","appFeatures":[{"feature":"HouseholdManagement","active":true,"settings":{},"target":"CHW","healthModule":"FAMILY","useCases":["HOUSEHOLD_REGISTRATION","REMOVE_HOUSEHOLD","HOUSEHOLD_VISITS","REMOVE_HOUSEHOLD_MEMBER"]},{"feature":"PatientManagement","active":true,"settings":{},"target":"CHW","healthModule":"ANC","useCases":["PATIENT_REGISTRATION","ANC_VISITS","PREGNANCY_OUTCOME"]},{"feature":"PatientManagement","active":true,"settings":{},"target":"HF","healthModule":"CHILD","useCases":["PATIENT_REGISTRATION","CHILD_IMMUNIZATION"]}]}"""
        .trimIndent()
        .decodeJson()
  }

  fun activatedFeatures(): List<FeatureConfig> =
    _appFeatureConfig?.appFeatures?.filter { it.active } ?: emptyList()

  fun activeRegisterFeatures() =
    activatedFeatures().filter {
      it.feature.equals(AppFeature.PatientManagement.name, true) ||
        it.feature.equals(AppFeature.HouseholdManagement.name, true)
    }
}
