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
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification

@Singleton
class AppFeatureManager @Inject constructor(val configurationRegistry: ConfigurationRegistry) {

  private var _appFeatureConfig: AppFeatureConfig? = null

  fun loadAndActivateFeatures() {
    // TODO do we want to handle missing this config for any app
    _appFeatureConfig =
      configurationRegistry.retrieveConfiguration(AppConfigClassification.APP_FEATURE)
  }

  fun activatedFeatures(): List<FeatureConfig> =
    _appFeatureConfig?.appFeatures?.filter { it.active } ?: emptyList()

  fun activeRegisterFeatures() =
    activatedFeatures().filter {
      it.feature == AppFeature.PatientManagement.name ||
        it.feature == AppFeature.HouseholdManagement.name
    }
}
