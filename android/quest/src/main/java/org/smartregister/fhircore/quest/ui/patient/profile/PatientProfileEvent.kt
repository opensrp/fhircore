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

package org.smartregister.fhircore.quest.ui.patient.profile

import android.content.Context
import org.smartregister.fhircore.quest.ui.patient.profile.model.PatientProfileViewSection

sealed class PatientProfileEvent {

  data class SeeAll(val patientProfileViewSection: PatientProfileViewSection) :
    PatientProfileEvent()

  data class LoadQuestionnaire(val questionnaireId: String, val context: Context) :
    PatientProfileEvent()

  data class OverflowMenuClick(val context: Context, val menuId: Int, val patientId: String) :
    PatientProfileEvent()
}
