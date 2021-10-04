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

package org.smartregister.fhircore.quest.ui.patient.details

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.data.QuestPatientRepository

class QuestPatientDetailViewModel(
  application: QuestApplication,
  private val repository: QuestPatientRepository,
  private val patientId: String
) : AndroidViewModel(application), QuestPatientDetailDataProvider {

  private var mOnBackPressListener: () -> Unit = {}
  private var mOnMenuItemClickListener: (menuItem: String) -> Unit = {}

  override fun getDemographics(): LiveData<Patient> {
    return repository.fetchDemographics(patientId)
  }

  override fun onBackPressListener(): () -> Unit {
    return mOnBackPressListener
  }

  override fun onMenuItemClickListener(): (menuItem: String) -> Unit {
    return mOnMenuItemClickListener
  }

  fun setOnBackPressListener(onBackPressListener: () -> Unit) {
    this.mOnBackPressListener = onBackPressListener
  }

  fun setOnMenuItemClickListener(onMenuItemClickListener: (menuItem: String) -> Unit) {
    this.mOnMenuItemClickListener = onMenuItemClickListener
  }

  companion object {
    fun get(
      owner: ViewModelStoreOwner,
      application: QuestApplication,
      repository: QuestPatientRepository,
      patientId: String
    ): QuestPatientDetailViewModel {
      return ViewModelProvider(
        owner,
        QuestPatientDetailViewModel(application, repository, patientId).createFactory()
      )[QuestPatientDetailViewModel::class.java]
    }
  }
}
