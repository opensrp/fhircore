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

package org.smartregister.fhircore.quest.util.mappers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.Enumerations
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.patient.profile.model.ProfileViewData

class ProfileViewDataMapper @Inject constructor(@ApplicationContext val context: Context) :
  DataMapper<ProfileData, ProfileViewData> {
  override fun transformInputToOutputModel(inputModel: ProfileData): ProfileViewData {
    return when (inputModel) {
      is ProfileData.AncProfileData ->
        ProfileViewData.PatientProfileViewData(
          name = inputModel.name,
          age = inputModel.age,
          sex = retrieveGender(inputModel.gender)
        )
      is ProfileData.DefaultProfileData ->
        ProfileViewData.PatientProfileViewData(
          name = inputModel.name,
          age = inputModel.age,
          sex = retrieveGender(inputModel.gender)
        )
      is ProfileData.FamilyProfileData ->
        ProfileViewData.FamilyProfileViewData(name = inputModel.name, address = inputModel.address)
    }
  }

  private fun retrieveGender(gender: Enumerations.AdministrativeGender) =
    when (gender) {
      Enumerations.AdministrativeGender.MALE -> context.getString(R.string.male)
      Enumerations.AdministrativeGender.FEMALE -> context.getString(R.string.female)
      else -> context.getString(R.string.unknown)
    }
}
