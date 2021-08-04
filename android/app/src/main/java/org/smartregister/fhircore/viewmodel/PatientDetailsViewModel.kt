package org.smartregister.fhircore.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient

class PatientDetailsViewModel(val fhirEngine: FhirEngine, val patientId: String) : ViewModel() {

    val patientDemographics = MutableLiveData<Patient>()

    val patientImmunizations = MutableLiveData<List<Immunization>>()

    fun fetchDemographics() {
        if (patientId.isNotEmpty())
            viewModelScope.launch(Dispatchers.IO) {
                patientDemographics.postValue(fhirEngine.load(Patient::class.java, patientId))
            }
    }

    fun fetchImmunizations() {
        if (patientId.isNotEmpty())
            viewModelScope.launch(Dispatchers.IO) {
                patientImmunizations.postValue(
                    fhirEngine.search {
                        filter(Immunization.PATIENT) {
                            value = "Patient/$patientId"
                        }
                    }
                )
            }
    }
}