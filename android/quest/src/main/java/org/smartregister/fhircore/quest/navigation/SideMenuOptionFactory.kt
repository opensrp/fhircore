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

package org.smartregister.fhircore.quest.navigation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.domain.model.SideMenuOption
import org.smartregister.fhircore.quest.R

@Singleton
class SideMenuOptionFactory
@Inject
constructor(
  val appFeatureManager: AppFeatureManager,
  val registerRepository: AppRegisterRepository
) {
  val defaultSideMenu =
    SideMenuOption(
      appFeatureName = AppFeature.PatientManagement.name,
      healthModule = HealthModule.DEFAULT,
      iconResource = R.drawable.ic_baby_mother,
      titleResource = R.string.all_clients,
      showCount = true,
      count =
        runBlocking {
          registerRepository.countRegisterData(
            appFeatureName = AppFeature.PatientManagement.name,
            healthModule = HealthModule.DEFAULT
          )
        }
    )

  fun retrieveSideMenuOptions(): List<SideMenuOption> {
    val sideMenuOptions =
      appFeatureManager.activeRegisterFeatures().map {
        SideMenuOption(
          appFeatureName = it.feature,
          healthModule = it.healthModule!!,
          iconResource =
            when (it.healthModule) {
              HealthModule.FAMILY -> R.drawable.ic_households
              HealthModule.ANC -> R.drawable.ic_baby_mother
              HealthModule.HOME_TRACING -> R.drawable.ic_home_tracings
              HealthModule.PHONE_TRACING -> R.drawable.ic_phone_tracings
              HealthModule.APPOINTMENT -> R.drawable.ic_appointments
              else -> R.drawable.ic_user
            },
          titleResource =
            when (it.healthModule) {
              HealthModule.DEFAULT -> R.string.all_clients
              HealthModule.ANC -> R.string.anc_clients
              HealthModule.RDT -> R.string.all_clients
              HealthModule.PNC -> R.string.pnc_clients
              HealthModule.FAMILY -> R.string.households
              HealthModule.CHILD -> R.string.children
              HealthModule.HIV -> R.string.hiv_clients
              HealthModule.HOME_TRACING -> R.string.home_tracing_clients
              HealthModule.PHONE_TRACING -> R.string.phone_tracing_clients
              HealthModule.APPOINTMENT -> R.string.appointment_clients
              HealthModule.FAMILY_PLANNING -> R.string.family_planning_clients
              else -> 0
            },
          showCount = true,
          count =
            runBlocking {
              registerRepository.countRegisterData(
                appFeatureName = it.feature,
                healthModule = it.healthModule!!
              )
            }
        )
      }
    return sideMenuOptions.ifEmpty { listOf(defaultSideMenu) }
  }
}
