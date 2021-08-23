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

package org.smartregister.fhircore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.model.AncItem
import org.smartregister.fhircore.sdk.PatientExtended
import org.smartregister.fhircore.util.Utils

class AncListViewModel(application: Application, private val fhirEngine: FhirEngine) :
  AndroidViewModel(application) {

  var loader = MutableLiveData(-1)
  val paginatedDataList = MutableLiveData<Pair<List<AncItem>, Pagination>>()

  fun searchResults(query: String? = null, page: Int = 0, pageSize: Int = 10) {
    viewModelScope.launch(Dispatchers.IO) {
      loader.postValue(1)
      var totalCount = count(query).toInt()

      val searchResults: List<Patient> =
        fhirEngine.search {
          Utils.addBaseAncFilter(this, PatientExtended.TAG)
          Utils.addSearchQueryFilter(this, query)

          sort(Patient.GIVEN, Order.ASCENDING)
          count = totalCount
          from = (page * pageSize)
        }

      var patients = searchResults.map { it.toAncItem() }
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
      Utils.addSearchQueryFilter(this, query)
    }
  }

  fun Patient.toAncItem(): AncItem {
    val name = this.name[0].nameAsSingleString
    val dob = if (this.hasBirthDateElement()) this.birthDateElement.valueAsString else ""
    val logicalId: String = this.logicalId
    val area = if (this.hasAddress()) this.addressFirstRep.city else ""
    return AncItem(id = logicalId, name = name, dob = dob, logicalId = logicalId, area = area)
  }
}
