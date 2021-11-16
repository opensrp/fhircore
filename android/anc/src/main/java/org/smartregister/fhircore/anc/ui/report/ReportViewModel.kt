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

package org.smartregister.fhircore.anc.ui.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import ca.uhn.fhir.parser.IParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DispatcherProvider

class ReportViewModel(
  private val repository: ReportRepository,
  var dispatcher: DispatcherProvider,
) : ViewModel() {

  val backPress: MutableLiveData<Boolean> = MutableLiveData(false)

  fun getReportsTypeList(): Flow<PagingData<ReportItem>> {
    return Pager(PagingConfig(pageSize = PaginationUtil.DEFAULT_PAGE_SIZE)) { repository }.flow
  }

  fun onBackPress() {
    backPress.value = true
  }

  fun fetchCQLLibraryData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    libraryURL: String
  ): LiveData<String> {
    val libraryData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLLibraryData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(libraryURL).entry[0].resource)
      libraryData.postValue(auxCQLLibraryData)
    }
    return libraryData
  }

  fun fetchCQLFhirHelperData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    helperURL: String
  ): LiveData<String> {
    val helperData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLHelperData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(helperURL).entry[0].resource)
      helperData.postValue(auxCQLHelperData)
    }
    return helperData
  }

  fun fetchCQLValueSetData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    valueSetURL: String
  ): LiveData<String> {
    val valueSetData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLValueSetData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(valueSetURL))
      valueSetData.postValue(auxCQLValueSetData)
    }
    return valueSetData
  }

  fun fetchCQLPatientData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    patientURL: String
  ): LiveData<String> {
    val patientData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLPatientData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(patientURL))
      patientData.postValue(auxCQLPatientData)
    }
    return patientData
  }

  fun fetchCQLMeasureEvaluateLibraryAndValueSets(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    libAndValueSetURL: String,
    measureURL: String,
    cqlMeasureReportLibInitialString: String
  ): LiveData<String> {
    val valueSetData = MutableLiveData<String>()
    val equalsIndexUrl: Int = libAndValueSetURL.indexOf("=")

    val libStrAfterEquals = libAndValueSetURL.substring(libAndValueSetURL.lastIndexOf("=") + 1)
    val libList = libStrAfterEquals.split(",").map { it.trim() }

    val libURLStrBeforeEquals = libAndValueSetURL.substring(0, equalsIndexUrl) + "="
    val initialStr = StringBuilder(cqlMeasureReportLibInitialString)

    viewModelScope.launch(dispatcher.io()) {
      val measureObject =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(measureURL).entry[0].resource)

      initialStr.append("{\"resource\":")
      initialStr.append(measureObject)
      initialStr.append("}")

      var auxCQLValueSetData: String
      for (lib in libList) {
        auxCQLValueSetData =
          parser.encodeResourceToString(
            fhirResourceDataSource.loadData(libURLStrBeforeEquals + lib).entry[0].resource
          )

        initialStr.append(",")
        initialStr.append("{\"resource\":")
        initialStr.append(auxCQLValueSetData)
        initialStr.append("}")
      }
      initialStr.deleteCharAt(initialStr.length - 1)
      initialStr.append("}]}")
      valueSetData.postValue(initialStr.toString())
    }
    return valueSetData
  }
}
