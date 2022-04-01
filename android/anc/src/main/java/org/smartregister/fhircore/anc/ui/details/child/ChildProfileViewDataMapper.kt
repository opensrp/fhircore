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

package org.smartregister.fhircore.anc.ui.details.child

import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.ui.details.child.model.ChildProfileRowItem
import org.smartregister.fhircore.anc.ui.details.child.model.ChildProfileViewData
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.DateUtils.isToday
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.hasStarted
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.makeItReadable

data class Child(val child: Patient, val tasks: List<Task>)

class ChildProfileViewDataMapper
@Inject
constructor(
  @ApplicationContext val context: Context,
) : DomainMapper<Child, ChildProfileViewData> {

  override fun mapToDomainModel(dto: Child): ChildProfileViewData {
    val child = dto.child
    val tasks = dto.tasks

    return ChildProfileViewData(
      id = child.logicalId,
      identifier = child.identifierFirstRep.value ?: child.logicalId,
      name = child.extractName(),
      sex = child.extractGender(context) ?: "",
      status = "",
      age = child.extractAge(),
      dob = child.birthDate.makeItReadable(),
      tasks =
        tasks.map {
          ChildProfileRowItem(
            id = it.logicalId,
            title = context.getString(R.string.child_routine_visit),
            subtitle =
              context.getString(R.string.due_on, it.executionPeriod.start.makeItReadable()),
            actionButtonText = context.getString(R.string.child_visit_button_title),
            actionButtonColor =
              if (it.status.isIn(Task.TaskStatus.READY, Task.TaskStatus.REQUESTED) &&
                  it.hasStarted() &&
                  it.executionPeriod.start.isToday()
              )
                BlueTextColor
              else if (it.status.isIn(Task.TaskStatus.READY, Task.TaskStatus.REQUESTED))
                GreyTextColor
              else if (it.status == Task.TaskStatus.COMPLETED) SuccessColor
              else if (it.status == Task.TaskStatus.FAILED) OverdueColor else GreyTextColor
          )
        }
    )
  }
}
