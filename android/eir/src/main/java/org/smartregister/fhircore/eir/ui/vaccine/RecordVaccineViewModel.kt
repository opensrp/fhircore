package org.smartregister.fhircore.eir.ui.vaccine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.hl7.fhir.r4.model.PositiveIntType
import org.smartregister.fhircore.engine.data.local.repository.model.PatientVaccineSummary
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientRepository

class RecordVaccineViewModel(application: Application, val patientRepository: PatientRepository) :
  AndroidViewModel(application) {

  suspend fun getPatientItem(logicalId: String): LiveData<PatientVaccineSummary> {
    val immunizations = patientRepository.getPatientImmunizations(logicalId = logicalId)
    if (immunizations.isNotEmpty()) {
      val immunization = immunizations.first()
      return MutableLiveData(
        PatientVaccineSummary(
          doseNumber = (immunization.protocolApplied[0].doseNumber as PositiveIntType).value,
          initialDose = immunization.vaccineCode.coding.first().code
        )
      )
    }
    return MutableLiveData()
  }
}
