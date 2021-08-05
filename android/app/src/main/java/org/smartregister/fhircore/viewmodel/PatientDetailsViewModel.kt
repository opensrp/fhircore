package org.smartregister.fhircore.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.util.DefaultDispatcherProvider
import org.smartregister.fhircore.util.DispatcherProvider

class PatientDetailsViewModel(
    var dispatcher: DispatcherProvider = DefaultDispatcherProvider,
    val fhirEngine: FhirEngine,
    val patientId: String
) : ViewModel() {

    val patientDemographics = MutableLiveData<Patient>()

    val patientImmunizations = MutableLiveData<List<Immunization>>()

    fun fetchDemographics() {
        if (patientId.isNotEmpty())
            viewModelScope.launch(dispatcher.io()) {
                val patient = fhirEngine.load(Patient::class.java, patientId)
                patientDemographics.postValue(patient)
            }
    }

    fun fetchImmunizations() {
        if (patientId.isNotEmpty())
            viewModelScope.launch(dispatcher.io()) {
                val immunizations: List<Immunization> = fhirEngine.search {
                    filter(Immunization.PATIENT) { value = "Patient/$patientId" }
                }
                patientImmunizations.postValue(immunizations)
            }
    }
}