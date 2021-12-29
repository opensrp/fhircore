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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import ca.uhn.fhir.parser.IParser
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.data.report.model.ResultItemPopulation
import org.smartregister.fhircore.anc.ui.anccare.register.AncRowClickListenerIntent
import org.smartregister.fhircore.anc.ui.anccare.register.OpenPatientProfile
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.ListenerIntent
import timber.log.Timber

@HiltViewModel
class ReportViewModel
@Inject
constructor(
  val repository: ReportRepository,
  val dispatcher: DispatcherProvider,
  val patientRepository: PatientRepository,
) : ViewModel() {

  lateinit var patientId: String

  lateinit var registerDataViewModel: RegisterDataViewModel<Anc, PatientItem>

  val backPress: MutableLiveData<Boolean> = MutableLiveData(false)
  val showDatePicker: MutableLiveData<Boolean> = MutableLiveData(false)
  val processGenerateReport: MutableLiveData<Boolean> = MutableLiveData(false)
  val alertSelectPatient: MutableLiveData<Boolean> = MutableLiveData(false)
  val isChangingStartDate: MutableLiveData<Boolean> = MutableLiveData(false)
  val selectedMeasureReportItem: MutableLiveData<ReportItem> = MutableLiveData(null)
  val selectedPatientItem: MutableLiveData<PatientItem> = MutableLiveData(null)
  val simpleDateFormatPattern = "d MMM, yyyy"

  val startDateTimeMillis: MutableLiveData<Long> = MutableLiveData(0L)
  val endDateTimeMillis: MutableLiveData<Long> = MutableLiveData(0L)

  private val _startDate = MutableLiveData("start date")
  val startDate: LiveData<String>
    get() = _startDate

  private val _endDate = MutableLiveData("end date")
  val endDate: LiveData<String>
    get() = _endDate

  private val _patientSelectionType = MutableLiveData("All")
  val patientSelectionType: LiveData<String>
    get() = _patientSelectionType

  var searchTextState = mutableStateOf(TextFieldValue(""))

  fun getSelectedPatient(): MutableLiveData<PatientItem> {
    return if (selectedPatientItem.value != null) selectedPatientItem
    else MutableLiveData(PatientItem(patientIdentifier = "Select Patient", name = "Select Patient"))
  }

  private val _filterValue = MutableLiveData<Pair<RegisterFilterType, Any?>>()
  val filterValue
    get() = _filterValue

  private val _isReadyToGenerateReport = MutableLiveData(true)
  val isReadyToGenerateReport: LiveData<Boolean>
    get() = _isReadyToGenerateReport

  val resultForIndividual: MutableLiveData<ResultItem> =
    MutableLiveData(ResultItem(status = "True", isMatchedIndicator = true))

  val resultForPopulation: MutableLiveData<List<ResultItemPopulation>> =
    MutableLiveData(loadDummyResultForPopulation())

  fun loadDummyResultForPopulation(): List<ResultItemPopulation>? {
    val testResultItem1 = ResultItem(title = "10 - 15 years", percentage = "10%", count = "1/10")
    val testResultItem2 = ResultItem(title = "16 - 20 years", percentage = "50%", count = "30/60")
    return listOf(
      ResultItemPopulation(title = "Age Range", listOf(testResultItem1, testResultItem2)),
      ResultItemPopulation(title = "Education Level", listOf(testResultItem1, testResultItem2))
    )
  }

  var reportState: ReportState = ReportState()

  fun getReportsTypeList(): Flow<PagingData<ReportItem>> {
    return Pager(PagingConfig(pageSize = PaginationUtil.DEFAULT_PAGE_SIZE)) { repository }.flow
  }

  fun onPatientItemClicked(listenerIntent: ListenerIntent, data: PatientItem) {
    if (listenerIntent is AncRowClickListenerIntent) {
      when (listenerIntent) {
        OpenPatientProfile -> updateSelectedPatient(data)
      }
    }
  }

  fun updateSelectedPatient(patient: PatientItem) {
    selectedPatientItem.value = patient
    reportState.currentScreen = ReportScreen.FILTER
  }

  fun onReportMeasureItemClicked(item: ReportItem) {
    setDefaultDates()
    selectedMeasureReportItem.value = item
    reportState.currentScreen = ReportScreen.FILTER
  }

  private fun setDefaultDates() {
    val endDate = Date()
    val cal: Calendar = Calendar.getInstance().apply { add(Calendar.DATE, -30) }
    val startDate = cal.time

    val formattedStartDate =
      SimpleDateFormat(simpleDateFormatPattern, Locale.getDefault()).format(startDate)
    val formattedEndDate =
      SimpleDateFormat(simpleDateFormatPattern, Locale.getDefault()).format(endDate)

    startDateTimeMillis.value = startDate.time
    endDateTimeMillis.value = endDate.time

    _startDate.value = formattedStartDate
    _endDate.value = formattedEndDate
  }

  fun onBackPress() {
    backPress.value = true
  }

  fun fetchCqlLibraryData(
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

  fun onBackPressFromFilter() {
    reportState.currentScreen = ReportScreen.HOME
  }

  fun onBackPressFromPatientSearch() {
    reportState.currentScreen = ReportScreen.FILTER
  }

  fun onBackPressFromResult() {
    reportState.currentScreen = ReportScreen.FILTER
  }

  fun getSelectionDate(): Long {
    val sdf = SimpleDateFormat(simpleDateFormatPattern, Locale.ENGLISH)
    try {
      var mDate = sdf.parse(_endDate.value!!)
      if (isChangingStartDate.value != false) {
        mDate = sdf.parse(_startDate.value!!)
      }
      val timeInMilliseconds = mDate?.time
      println("Date in milli :: $timeInMilliseconds")
      return timeInMilliseconds!!
    } catch (e: ParseException) {
      e.printStackTrace()
      return Date().time
    }
  }

  fun onStartDatePress() {
    isChangingStartDate.value = true
    showDatePicker.value = true
  }

  fun onEndDatePress() {
    isChangingStartDate.value = false
    showDatePicker.value = true
  }

  fun onPatientSelectionTypeChanged(newType: String) {
    _patientSelectionType.value = newType
  }

  fun onGenerateReportPress() {
    reportState.currentScreen = ReportScreen.RESULT
  }

  fun auxGenerateReport() {
    if (patientSelectionType.value.equals("All", true) || selectedPatientItem.value != null) {
      processGenerateReport.setValue(true)
    } else {
      alertSelectPatient.setValue(true)
    }
  }

  fun onDatePicked(selection: Long) {
    showDatePicker.value = false
    if (isChangingStartDate.value != false) {
      val startDate = Date().apply { time = selection }
      val formattedStartDate =
        SimpleDateFormat(simpleDateFormatPattern, Locale.getDefault()).format(startDate)
      startDateTimeMillis.value = startDate.time
      _startDate.value = formattedStartDate
    } else {
      val endDate = Date().apply { time = selection }
      val formattedEndDate =
        SimpleDateFormat(simpleDateFormatPattern, Locale.getDefault()).format(endDate)
      endDateTimeMillis.value = endDate.time
      _endDate.value = formattedEndDate
    }
    _isReadyToGenerateReport.value = true
    reportState.currentScreen = ReportScreen.FILTER
  }

  fun fetchCqlFhirHelperData(
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

  fun fetchCqlValueSetData(
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

  fun fetchCqlPatientData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    patientURL: String
  ): LiveData<String> {
    val patientData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      try {
        val dataFromUrl = fhirResourceDataSource.loadData(patientURL)
        val auxCQLPatientData = parser.encodeResourceToString(dataFromUrl)
        patientData.postValue(auxCQLPatientData)
      } catch (e: Exception) {
        Timber.e(e)
        patientData.postValue("")
      }
    }
    return patientData
  }

  fun fetchCqlMeasureEvaluateLibraryAndValueSets(
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
    val fullResourceString = StringBuilder(cqlMeasureReportLibInitialString)

    viewModelScope.launch(dispatcher.io()) {
      val measureObject =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(measureURL).entry[0].resource)

      fullResourceString.append("{\"resource\":")
      fullResourceString.append(measureObject)
      fullResourceString.append("}")

      var auxCQLValueSetData: String
      for (lib in libList) {
        auxCQLValueSetData =
          parser.encodeResourceToString(
            fhirResourceDataSource.loadData(libURLStrBeforeEquals + lib).entry[0].resource
          )

        fullResourceString.append(",")
        fullResourceString.append("{\"resource\":")
        fullResourceString.append(auxCQLValueSetData)
        fullResourceString.append("}")
      }
      fullResourceString.deleteCharAt(fullResourceString.length - 1)
      fullResourceString.append("}]}")
      valueSetData.postValue(fullResourceString.toString())
    }
    return valueSetData
  }

  class ReportState {
    var currentScreen by mutableStateOf(ReportScreen.PREHOMElOADING)
  }

  enum class ReportScreen {
    HOME,
    FILTER,
    PICK_PATIENT,
    RESULT,
    PREHOMElOADING
  }

  object PatientSelectionType {
    const val ALL = "All"
    const val INDIVIDUAL = "Individual"
  }
}
