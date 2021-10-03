package org.smartregister.fhircore.quest.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.fhir.FhirEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

class QuestPatientRepository(
  private val fhirEngine: FhirEngine,
  private val dispatcherProvider: DefaultDispatcherProvider = DefaultDispatcherProvider
  ) {

  fun fetchDemographics(patientId: String): LiveData<Patient> {
    val data = MutableLiveData<Patient>()
    CoroutineScope(dispatcherProvider.io()).launch {
      data.postValue(fhirEngine.load(Patient::class.java, patientId))
    }
    return data
  }
}