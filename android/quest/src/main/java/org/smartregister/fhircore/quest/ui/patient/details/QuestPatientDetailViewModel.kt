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
  private val patientId: String): AndroidViewModel(application), QuestPatientDetailDataProvider {

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