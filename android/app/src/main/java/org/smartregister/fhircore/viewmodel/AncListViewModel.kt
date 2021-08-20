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
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.sdk.PatientExtended
import org.smartregister.fhircore.util.Utils

class AncListViewModel(application: Application, private val fhirEngine: FhirEngine) :
  AndroidViewModel(application) {

  var loader = MutableLiveData(-1)
  val paginatedDataList by lazy { MutableLiveData<Pair<List<PatientItem>, Pagination>>() }

  fun searchResults(query: String? = null, page: Int = 0, pageSize: Int = 10) {
    viewModelScope.launch(Dispatchers.IO) {
      loader.postValue(1)
      var totalCount = count(query).toInt()

      val searchResults: List<Patient> =
        fhirEngine.search {
          Utils.addBaseAncFilter(this, PatientExtended.TAG)

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

      var patients = searchResults.map {
        it.toPatientItem()
      }
      totalCount = patients.size + (page * pageSize)
      patients = patients.take(pageSize)

      loader.postValue(0)
      paginatedDataList.postValue(
        Pair(patients, Pagination(totalItems = totalCount, pageSize = pageSize, currentPage = page))
      )
    }
  }

  suspend fun count(query: String?): Long {
    return fhirEngine.count<Patient> {
      Utils.addBaseAncFilter(this, PatientExtended.TAG)
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
}
