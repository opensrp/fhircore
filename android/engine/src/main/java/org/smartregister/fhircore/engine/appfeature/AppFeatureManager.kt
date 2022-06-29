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
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry

@Singleton
class AppFeatureManager @Inject constructor(val configurationRegistry: ConfigurationRegistry) {

  private var _appFeatureConfig: AppFeatureConfig? = null

  fun loadAndActivateFeatures() {
    // TODO remove this feature it will be no longer needed. Or refactor it accordingly
    _appFeatureConfig = configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  fun activatedFeatures(): List<FeatureConfig> =
    _appFeatureConfig?.appFeatures?.filter { it.active } ?: emptyList()

  fun activeRegisterFeatures(): List<FeatureConfig> {
    loadAndActivateFeatures()
    return activatedFeatures().filter {
      it.feature.equals(AppFeature.PatientManagement.name, true) ||
        it.feature.equals(AppFeature.HouseholdManagement.name, true)
    }
  }

  fun isFeatureActive(appFeature: AppFeature) =
    activatedFeatures().find { appFeature.name.equals(it.feature, true) } != null

  fun appFeatureSettings(appFeature: AppFeature) =
    activatedFeatures().find { appFeature.name.equals(it.feature, true) }?.settings ?: mapOf()

  fun appFeatureSettings(appFeatureName: String) =
    activatedFeatures().find { appFeatureName.equals(it.feature, true) }?.settings ?: mapOf()

  fun appFeatureHasSetting(settingName: String) =
    activatedFeatures().any { appFeatureSettings(it.feature).contains(settingName) }
}
