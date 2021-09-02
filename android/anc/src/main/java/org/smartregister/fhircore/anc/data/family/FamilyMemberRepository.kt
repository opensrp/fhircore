package org.smartregister.fhircore.anc.data.family

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class FamilyMemberRepository(private val familyId: String,
                             private val fhirEngine: FhirEngine,
                             private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider) {

  fun fetchDemographics(): LiveData<Patient> {
    val data = MutableLiveData<Patient>()
    CoroutineScope(dispatcherProvider.io()).launch {
      data.postValue(fhirEngine.load(Patient::class.java, familyId))
    }
    return data
  }

  fun fetchFamilyMembers(): LiveData<List<FamilyMemberItem>> {
    val data = MutableLiveData<List<FamilyMemberItem>>()
     CoroutineScope(dispatcherProvider.io()).launch {
      val members =
        fhirEngine.search<Patient> { filter(Patient.LINK) { this.value = familyId } }.map {
          it.toFamilyMemberItem()
        }
      data.postValue(members)
    }
    return data
  }

  fun fetchEncounters(): LiveData<List<Encounter>> {
    val data = MutableLiveData<List<Encounter>>()
    CoroutineScope(dispatcherProvider.io()).launch {
      val encounters =
        fhirEngine.search<Encounter> { filter(Encounter.SUBJECT) { value = "Patient/$familyId" } }
      data.postValue(encounters)
    }
    return data
  }

  private fun Patient.toFamilyMemberItem(): FamilyMemberItem {
    val name = this.name?.first()?.nameAsSingleString ?: ""
    val gender = if (this.hasGenderElement()) this.genderElement.valueAsString else ""
    val age = if (this.hasBirthDateElement()) this.birthDateElement.valueAsString else ""
    val pregnant = extension.any { it.value.toString().contains("pregnant", true) }

    return FamilyMemberItem(name, id, age, gender, pregnant)
  }
}