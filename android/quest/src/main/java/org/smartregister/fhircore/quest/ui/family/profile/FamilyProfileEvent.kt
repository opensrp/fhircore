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

package org.smartregister.fhircore.quest.ui.family.profile

import android.content.Context
import androidx.navigation.NavHostController

sealed class FamilyProfileEvent {

    object RoutineVisit : FamilyProfileEvent()

    data class AddMember(val context: Context, val familyHeadId: String?) : FamilyProfileEvent()

    data class OpenTaskForm(val context: Context, val taskFormId: String) : FamilyProfileEvent()

    data class OpenMemberProfile(val patientId: String, val navController: NavHostController) :
        FamilyProfileEvent()

    data class OverflowMenuClick(val context: Context, val familyHeadId: String?, val menuId: Int) :
        FamilyProfileEvent()

    data class FetchFamilyProfileData(val familyHeadId: String?) : FamilyProfileEvent()

    data class FetchMemberTasks(val patientId: String?) : FamilyProfileEvent()
}
