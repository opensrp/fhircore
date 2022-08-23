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

package org.smartregister.fhircore.quest.ui.patient.profile.childcontact

import androidx.navigation.NavHostController
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.quest.ui.shared.models.PatientProfileViewSection

sealed class ChildContactEvent {

  data class SeeAll(val patientProfileViewSection: PatientProfileViewSection) : ChildContactEvent()

  data class OpenProfile(
    val appFeatureName: String?,
    val healthModule: HealthModule,
    val patientId: String,
    val navController: NavHostController
  ) : ChildContactEvent()
}
