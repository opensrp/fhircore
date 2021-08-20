package org.smartregister.fhircore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.model.FamilyItem
import org.smartregister.fhircore.model.FamilyMemberItem
import org.smartregister.fhircore.sdk.PatientExtended
import org.smartregister.fhircore.util.Utils

class FamilyListViewModel(application: Application, private val fhirEngine: FhirEngine) :
  AndroidViewModel(application) {

  var loader = MutableLiveData(-1)
  val paginatedDataList by lazy { MutableLiveData<Pair<List<FamilyItem>, Pagination>>() }

  fun searchResults(query: String? = null, page: Int = 0, pageSize: Int = 10) {
    viewModelScope.launch(Dispatchers.IO) {
      loader.postValue(1)
      var totalCount = count(query).toInt()

      val searchResults: List<Patient> =
        fhirEngine.search {
          Utils.addBaseFamilyFilter(this, PatientExtended.TAG)

          apply {
            if (query?.isNotBlank() == true) {
              filter(Patient.NAME) {
                modifier = StringFilterModifier.CONTAINS
                value = query.trim()
              }
            }
          }

          sort(Patient.GIVEN, Order.ASCENDING)
          count = totalCount
          from = (page * pageSize)
        }

      var families = searchResults.map {
        val members = fhirEngine.search<Patient>{
          filter(Patient.LINK){
            this.value = it.id
          }
        }
        it.toFamilyItem(members)
      }
      totalCount = families.size + (page * pageSize)
      families = families.take(pageSize)

      loader.postValue(0)
      paginatedDataList.postValue(
        Pair(families, Pagination(totalItems = totalCount, pageSize = pageSize, currentPage = page))
      )
    }
  }

  suspend fun count(query: String?): Long {
    return fhirEngine.count<Patient> {
      Utils.addBaseFamilyFilter(this, PatientExtended.TAG)
      apply {
        if (query?.isNotBlank() == true) {
          filter(Patient.NAME) {
            modifier = StringFilterModifier.CONTAINS
            value = query.trim()
          }
        }
      }
    }
  }

  fun Patient.toFamilyItem(familyMembers: List<Patient>): FamilyItem {
    val name = this.name[0].nameAsSingleString
    val gender = if (this.hasGenderElement()) this.genderElement.valueAsString else ""
    val dob = if (this.hasBirthDateElement()) this.birthDateElement.valueAsString else ""
    val phone: String =
      if (this.hasTelecom() && this.telecom[0].hasValue()) this.telecom[0].value else ""
    val logicalId: String = this.logicalId
    val members = familyMembers.map { it.toFamilyMemberItem() }
    val area = this.addressFirstRep.city

    return FamilyItem(this.id, name, gender, dob, phone, logicalId, area, members)
  }

  fun Patient.toFamilyMemberItem(): FamilyMemberItem {
    val name = this.name[0].nameAsSingleString
    val gender = if (this.hasGenderElement()) this.genderElement.valueAsString else ""
    val dob = if (this.hasBirthDateElement()) this.birthDateElement.valueAsString else ""
    val phone: String =
      if (this.hasTelecom() && this.telecom[0].hasValue()) this.telecom[0].value else ""
    val logicalId: String = this.logicalId
    val ext = this.extension.firstOrNull { it.value.toString().contains("pregnant", true) }
    val pregnant = ext?.value?.toString() ?: ""

    return FamilyMemberItem(this.id, name, gender, dob, phone, logicalId, pregnant)
  }
}
