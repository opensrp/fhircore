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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import kotlinx.android.synthetic.main.fragment_anc_details.*
import kotlinx.android.synthetic.main.fragment_anc_details.button_CQLEvaluate
import org.json.JSONObject
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.AncOverviewItem
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.databinding.FragmentAncDetailsBinding
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.cql.MeasureEvaluator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.FileUtil
import org.smartregister.fhircore.engine.util.extension.createFactory
import timber.log.Timber

class AncDetailsFragment : Fragment() {

  lateinit var patientId: String
  private lateinit var fhirEngine: FhirEngine

  lateinit var ancDetailsViewModel: AncDetailsViewModel

  private lateinit var patientRepository: PatientRepository

  private var carePlanAdapter = CarePlanAdapter()

  private val upcomingServicesAdapter = UpcomingServicesAdapter()

  private val lastSeen = EncounterAdapter()

  lateinit var binding: FragmentAncDetailsBinding

  lateinit var parser: IParser

  lateinit var fhirResourceDataSource: FhirResourceDataSource

  lateinit var libraryEvaluator: LibraryEvaluator

  lateinit var measureEvaluator: MeasureEvaluator

  lateinit var fileUtil: FileUtil

  var libraryData = ""
  var measureEvaluateLibraryData = ""
  var helperData = ""
  var valueSetData = ""
  var testData = ""
  val evaluatorId = "ANCRecommendationA2"
  val contextCQL = "patient"
  val contextLabel = "mom-with-anemia"

  var cqlBaseURL = ""
  var libraryURL = ""
  var measureEvaluateLibraryURL = ""
  var measureTypeURL = ""

  var cqlMasureReportURL = ""
  var cqlMeasureReportStartDate = ""
  var cqlMeasureReportEndDate = ""
  var cqlMeasureReportReportType = ""
  var cqlMeasureReportSubject = ""
  var cqlMeasureReportLibInitialString = ""
  var cqlHelperURL = ""
  var valueSetURL = ""
  var patientURL = ""
  var cqlConfigFileName = "configs/cql_configs.properties"

  var parametersEvaluate = ""
  var parametersMeasure = ""

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_anc_details, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    patientId = arguments?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    libraryEvaluator = LibraryEvaluator()
    measureEvaluator = MeasureEvaluator()
    parser = FhirContext.forR4().newJsonParser()

    fhirResourceDataSource = FhirResourceDataSource.getInstance(AncApplication.getContext())

    fhirEngine = AncApplication.getContext().fhirEngine

    setupViews()

    patientRepository = getAncPatientRepository()

    ancDetailsViewModel =
      ViewModelProvider(
        viewModelStore,
        AncDetailsViewModel(patientRepository, patientId = patientId).createFactory()
      )[AncDetailsViewModel::class.java]

    binding.txtViewPatientId.text = patientId

    Timber.d(patientId)

    ancDetailsViewModel
      .fetchDemographics()
      .observe(viewLifecycleOwner, this::handlePatientDemographics)

    ancDetailsViewModel.fetchCarePlan().observe(viewLifecycleOwner, this::handleCarePlan)

    ancDetailsViewModel.fetchObservation().observe(viewLifecycleOwner, this::handleObservation)

    ancDetailsViewModel
      .fetchUpcomingServices()
      .observe(viewLifecycleOwner, this::handleUpcomingServices)
    ancDetailsViewModel.fetchCarePlan().observe(viewLifecycleOwner, this::handleCarePlan)

    fileUtil = FileUtil()
    cqlBaseURL =
      context?.let { fileUtil.getProperty("smart_register_base_url", it, cqlConfigFileName) }!!
    libraryURL =
      cqlBaseURL + context?.let { fileUtil.getProperty("cql_library_url", it, cqlConfigFileName) }
    cqlHelperURL =
      cqlBaseURL +
        context?.let { fileUtil.getProperty("cql_helper_library_url", it, cqlConfigFileName) }
    valueSetURL =
      cqlBaseURL + context?.let { fileUtil.getProperty("cql_value_set_url", it, cqlConfigFileName) }
    patientURL =
      cqlBaseURL + context?.let { fileUtil.getProperty("cql_patient_url", it, cqlConfigFileName) }

    measureEvaluateLibraryURL =
      context?.let {
        fileUtil.getProperty("cql_measure_report_library_value_sets_url", it, cqlConfigFileName)
      }!!

    measureTypeURL =
      context?.let {
        fileUtil.getProperty("cql_measure_report_resource_url", it, cqlConfigFileName)
      }!!

    cqlMasureReportURL =
      context?.let { fileUtil.getProperty("cql_measure_report_url", it, cqlConfigFileName) }!!

    cqlMeasureReportStartDate =
      context?.let {
        fileUtil.getProperty("cql_measure_report_start_date", it, cqlConfigFileName)
      }!!

    cqlMeasureReportEndDate =
      context?.let { fileUtil.getProperty("cql_measure_report_end_date", it, cqlConfigFileName) }!!

    cqlMeasureReportReportType =
      context?.let {
        fileUtil.getProperty("cql_measure_report_report_type", it, cqlConfigFileName)
      }!!

    cqlMeasureReportSubject =
      context?.let { fileUtil.getProperty("cql_measure_report_subject", it, cqlConfigFileName) }!!

    cqlMeasureReportLibInitialString =
      context?.let {
        fileUtil.getProperty("cql_measure_report_lib_initial_string", it, cqlConfigFileName)
      }!!

    showCQLCard()

    ancDetailsViewModel.fetchLastSeen().observe(viewLifecycleOwner, this::handleLastSeen)
  }

  private fun handleObservation(ancOverviewItem: AncOverviewItem) {
    binding.txtViewEDDDoseDate.text = ancOverviewItem.edd
    binding.txtViewGAPeriod.text = ancOverviewItem.ga
    binding.txtViewFetusesCount.text = ancOverviewItem.noOfFetuses
    binding.txtViewRiskValue.text = ancOverviewItem.risk
  }

  private fun handleUpcomingServices(listEncounters: List<UpcomingServiceItem>) {
    when {
      listEncounters.isEmpty() -> {
        binding.txtViewNoUpcomingServices.visibility = View.VISIBLE
        binding.upcomingServicesListView.visibility = View.GONE
        binding.txtViewUpcomingServicesSeeAllHeading.visibility = View.GONE
        binding.imageViewUpcomingServicesSeeAllArrow.visibility = View.GONE
      }
      else -> {
        binding.txtViewNoUpcomingServices.visibility = View.GONE
        binding.upcomingServicesListView.visibility = View.VISIBLE
        binding.txtViewUpcomingServicesSeeAllHeading.visibility = View.VISIBLE
        binding.txtViewUpcomingServicesSeeAllHeading.visibility = View.VISIBLE
        populateUpcomingServicesList(listEncounters)
      }
    }
  }

  private fun handleLastSeen(listEncounters: List<EncounterItem>) {
    when {
      listEncounters.isEmpty() -> {
        binding.txtViewNoLastSeenServices.visibility = View.VISIBLE
        binding.lastSeenListView.visibility = View.GONE
      }
      else -> {
        binding.txtViewNoLastSeenServices.visibility = View.GONE
        binding.lastSeenListView.visibility = View.VISIBLE
        populateLastSeenList(listEncounters)
      }
    }
  }

  private fun setupViews() {
    binding.carePlanListView.apply {
      adapter = carePlanAdapter
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    binding.upcomingServicesListView.apply {
      adapter = upcomingServicesAdapter
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    binding.lastSeenListView.apply {
      adapter = lastSeen
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }
  }

  companion object {
    fun newInstance(bundle: Bundle = Bundle()) = AncDetailsFragment().apply { arguments = bundle }
  }

  private fun handlePatientDemographics(patient: AncPatientDetailItem) {
    with(patient) {
      val patientDetails =
        this.patientDetails.name +
          ", " +
          this.patientDetails.gender +
          ", " +
          this.patientDetails.age
      val patientId =
        this.patientDetailsHead.demographics + " ID: " + this.patientDetails.patientIdentifier
      binding.txtViewPatientDetails.text = patientDetails
      binding.txtViewPatientId.text = patientId
    }
  }

  private fun handleCarePlan(immunizations: List<CarePlanItem>) {
    when {
      immunizations.isEmpty() -> {
        binding.txtViewNoCarePlan.visibility = View.VISIBLE
        binding.txtViewCarePlanSeeAllHeading.visibility = View.GONE
        binding.imageViewSeeAllArrow.visibility = View.GONE
        binding.carePlanListView.visibility = View.GONE
      }
      else -> {
        binding.txtViewNoCarePlan.visibility = View.GONE
        binding.txtViewCarePlanSeeAllHeading.visibility = View.VISIBLE
        binding.imageViewSeeAllArrow.visibility = View.VISIBLE
        binding.carePlanListView.visibility = View.VISIBLE
        populateImmunizationList(immunizations)
      }
    }
  }

  private fun populateImmunizationList(listCarePlan: List<CarePlanItem>) {
    carePlanAdapter.submitList(listCarePlan)
  }

  private fun populateUpcomingServicesList(upcomingServiceItem: List<UpcomingServiceItem>) {
    upcomingServicesAdapter.submitList(upcomingServiceItem)
  }
  private fun populateLastSeenList(upcomingServiceItem: List<EncounterItem>) {
    lastSeen.submitList(upcomingServiceItem)
  }

  fun buttonCQLSetOnClickListener() {
    button_CQLEvaluate.setOnClickListener {
      if (parametersEvaluate.isEmpty()) {
        loadCQLLibraryData()
      } else {
        parametersCQLToggleFinalView()
      }
    }
  }

  fun buttonCQLMeasureEvaluateSetOnClickListener() {
    button_CQL_Measure_Evaluate.setOnClickListener {
      if (parametersMeasure.isEmpty()) {
        loadMeasureEvaluateLibrary()
      } else {
        parametersCQLMeasureToggleFinalView()
      }
    }
  }

  fun startProgressBarAndTextViewCQLResults() {
    progress_circular_cql.visibility = View.VISIBLE
    textView_CQLResults.visibility = View.GONE
  }

  fun loadCQLLibraryData() {
    button_CQL_Measure_Evaluate.isEnabled = false
    startProgressBarAndTextViewCQLResults()

    ancDetailsViewModel
      .fetchCQLLibraryData(parser, fhirResourceDataSource, libraryURL)
      .observe(viewLifecycleOwner, this::handleCQLLibraryData)
  }

  fun loadCQLHelperData() {
    ancDetailsViewModel
      .fetchCQLFhirHelperData(parser, fhirResourceDataSource, cqlHelperURL)
      .observe(viewLifecycleOwner, this::handleCQLHelperData)
  }

  fun loadCQLValueSetData() {
    ancDetailsViewModel
      .fetchCQLValueSetData(parser, fhirResourceDataSource, valueSetURL)
      .observe(viewLifecycleOwner, this::handleCQLValueSetData)
  }

  fun loadCQLPatientData() {
    ancDetailsViewModel
      .fetchCQLPatientData(parser, fhirResourceDataSource, "$patientURL$patientId/\$everything")
      .observe(viewLifecycleOwner, this::handleCQLPatientData)
  }

  fun loadMeasureEvaluateLibrary() {
    button_CQLEvaluate.isEnabled = false
    startProgressBarAndTextViewCQLResults()
    ancDetailsViewModel
      .fetchCQLMeasureEvaluateLibraryAndValueSets(
        parser,
        fhirResourceDataSource,
        measureEvaluateLibraryURL,
        measureTypeURL,
        cqlMeasureReportLibInitialString
      )
      .observe(viewLifecycleOwner, this::handleMeasureEvaluateLibrary)
  }

  fun loadMeasureEvaluatePatient() {
    ancDetailsViewModel
      .fetchCQLPatientData(parser, fhirResourceDataSource, "$patientURL$patientId/\$everything")
      .observe(viewLifecycleOwner, this::handleMeasureEvaluatePatient)
  }

  fun handleCQLLibraryData(auxLibraryData: String) {
    libraryData = auxLibraryData
    loadCQLHelperData()
  }

  fun handleCQLHelperData(auxHelperData: String) {
    helperData = auxHelperData
    loadCQLValueSetData()
  }

  fun handleCQLValueSetData(auxValueSetData: String) {
    valueSetData = auxValueSetData
    loadCQLPatientData()
  }

  fun handleCQLPatientData(auxPatientData: String) {
    testData = libraryEvaluator.processCQLPatientBundle(auxPatientData)
    parametersEvaluate =
      libraryEvaluator.runCql(
        libraryData,
        helperData,
        valueSetData,
        testData,
        evaluatorId,
        contextCQL,
        contextLabel
      )
    parametersCQLToggleFinalView()
  }

  fun handleMeasureEvaluatePatient(auxPatientData: String) {
    testData = libraryEvaluator.processCQLPatientBundle(auxPatientData)
    var patientResources: ArrayList<String> = ArrayList()
    patientResources.add(testData)
    parametersMeasure =
      measureEvaluator.runMeasureEvaluate(
        measureEvaluateLibraryData,
        patientResources,
        cqlMasureReportURL,
        cqlMeasureReportStartDate,
        cqlMeasureReportEndDate,
        cqlMeasureReportReportType,
        cqlMeasureReportSubject
      )
    parametersCQLMeasureToggleFinalView()
  }
  fun parametersCQLToggleFinalView() {
    handleParametersQCLMeasure(parametersEvaluate)
    button_CQL_Measure_Evaluate.isEnabled = true
  }

  fun parametersCQLMeasureToggleFinalView() {
    handleParametersQCLMeasure(parametersMeasure)
    button_CQLEvaluate.isEnabled = true
  }

  fun handleParametersQCLMeasure(parameters: String) {
    val jsonObject = JSONObject(parameters)
    textView_CQLResults.text = jsonObject.toString(4)
    progress_circular_cql.visibility = View.GONE
    textView_CQLResults.visibility = View.VISIBLE
  }

  fun handleMeasureEvaluateLibrary(auxMeasureEvaluateLibData: String) {
    measureEvaluateLibraryData = auxMeasureEvaluateLibData
    loadMeasureEvaluatePatient()
  }

  val ANC_TEST_PATIENT_ID = "e8725b4c-6db0-4158-a24d-50a5ddf1c2ed"
  fun showCQLCard() {
    if (patientId == ANC_TEST_PATIENT_ID) {
      textView_EvaluateCQLHeader.visibility = View.VISIBLE
      cardView_CQLSection.visibility = View.VISIBLE
      buttonCQLSetOnClickListener()
      buttonCQLMeasureEvaluateSetOnClickListener()
    }
  }

  fun getAncPatientRepository(): PatientRepository {
    return PatientRepository(
      (requireActivity().application as AncApplication).fhirEngine,
      AncPatientItemMapper
    )
  }
}
