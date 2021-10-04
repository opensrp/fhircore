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
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.anc.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.anc.model.CarePlanItem
import org.smartregister.fhircore.anc.databinding.FragmentAncDetailsBinding
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.cql.MeasureEvaluator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.FileUtil
import org.smartregister.fhircore.engine.util.extension.createFactory

class AncDetailsFragment private constructor() : Fragment() {

  lateinit var patientId: String
  private lateinit var fhirEngine: FhirEngine

  lateinit var ancDetailsViewModel: AncDetailsViewModel

  private lateinit var ancPatientRepository: AncPatientRepository

  private var carePlanAdapter = CarePlanAdapter()

  lateinit var binding: FragmentAncDetailsBinding

  lateinit var parser: IParser

  lateinit var fhirResourceService: FhirResourceService

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

  var CQL_BASE_URL = ""
  var LIBRARY_URL = ""
  var MEASURE_EVALUATE_LIBRARY_URL = ""
  var MEASURE_RESOURCE_TYPE_URL = ""

  var CQL_MEASURE_REPORT_URL = ""
  var CQL_MEASURE_REPORT_START_DATE = ""
  var CQL_MEASURE_REPORT_END_DATE = ""
  var CQL_MEASURE_REPORT_REPORT_TYPE = ""
  var CQL_MEASURE_REPORT_SUBJECT = ""

  var HELPER_URL = ""
  var VALUE_SET_URL = ""
  var PATIENT_URL = ""
  var CQL_CONFIG_FILE_NAME = "configs/cql_configs.properties"

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
    fhirResourceService =
      FhirResourceService.create(
        parser,
        activity?.applicationContext!!,
        AncApplication.getContext().applicationConfiguration
      )

    fhirResourceDataSource = FhirResourceDataSource(fhirResourceService)

    fhirEngine = AncApplication.getContext().fhirEngine

    setupViews()

    ancPatientRepository = getAncPatientRepository()

    ancDetailsViewModel =
      ViewModelProvider(
        this,
        AncDetailsViewModel(ancPatientRepository, patientId = patientId).createFactory()
      )[AncDetailsViewModel::class.java]

    binding.txtViewPatientId.text = patientId

    ancDetailsViewModel
      .fetchDemographics()
      .observe(viewLifecycleOwner, this::handlePatientDemographics)

    ancDetailsViewModel
      .fetchCarePlan(
        context?.assets?.open("careplan_sample.json")?.bufferedReader().use { it?.readText() }
      )
      .observe(viewLifecycleOwner, this::handleCarePlan)

    fileUtil = FileUtil()
    CQL_BASE_URL =
      context?.let { fileUtil.getProperty("smart_register_base_url", it, CQL_CONFIG_FILE_NAME) }!!
    LIBRARY_URL =
      CQL_BASE_URL +
        context?.let { fileUtil.getProperty("cql_library_url", it, CQL_CONFIG_FILE_NAME) }
    HELPER_URL =
      CQL_BASE_URL +
        context?.let { fileUtil.getProperty("cql_helper_library_url", it, CQL_CONFIG_FILE_NAME) }
    VALUE_SET_URL =
      CQL_BASE_URL +
        context?.let { fileUtil.getProperty("cql_value_set_url", it, CQL_CONFIG_FILE_NAME) }
    PATIENT_URL =
      CQL_BASE_URL +
        context?.let { fileUtil.getProperty("cql_patient_url", it, CQL_CONFIG_FILE_NAME) }

    MEASURE_EVALUATE_LIBRARY_URL =
      context?.let {
        fileUtil.getProperty("cql_measure_report_library_value_sets_url", it, CQL_CONFIG_FILE_NAME)
      }!!

    MEASURE_RESOURCE_TYPE_URL =
      context?.let {
        fileUtil.getProperty("cql_measure_report_resource_url", it, CQL_CONFIG_FILE_NAME)
      }!!

    CQL_MEASURE_REPORT_URL =
      context?.let { fileUtil.getProperty("cql_measure_report_url", it, CQL_CONFIG_FILE_NAME) }!!

    CQL_MEASURE_REPORT_START_DATE =
      context?.let {
        fileUtil.getProperty("cql_measure_report_start_date", it, CQL_CONFIG_FILE_NAME)
      }!!

    CQL_MEASURE_REPORT_END_DATE =
      context?.let {
        fileUtil.getProperty("cql_measure_report_end_date", it, CQL_CONFIG_FILE_NAME)
      }!!

    CQL_MEASURE_REPORT_REPORT_TYPE =
      context?.let {
        fileUtil.getProperty("cql_measure_report_report_type", it, CQL_CONFIG_FILE_NAME)
      }!!

    CQL_MEASURE_REPORT_SUBJECT =
      context?.let {
        fileUtil.getProperty("cql_measure_report_subject", it, CQL_CONFIG_FILE_NAME)
      }!!

    showCQLCard()
  }

  private fun setupViews() {
    binding.carePlanListView.apply {
      adapter = carePlanAdapter
      layoutManager = LinearLayoutManager(requireContext())
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
        binding.carePlanListView.visibility = View.GONE
      }
      else -> {
        binding.txtViewNoCarePlan.visibility = View.GONE
        binding.carePlanListView.visibility = View.VISIBLE
        populateImmunizationList(immunizations)
      }
    }
  }

  private fun populateImmunizationList(listCarePlan: List<CarePlanItem>) {
    carePlanAdapter.submitList(listCarePlan)
  }

  fun buttonCQLSetOnClickListener() {
    button_CQLEvaluate.setOnClickListener { loadCQLLibraryData() }
  }

  fun buttonCQLMeasureEvaluateSetOnClickListener() {
    button_CQL_Measure_Evaluate.setOnClickListener { loadMeasureEvaluateLibrary() }
  }

  fun loadCQLLibraryData() {
    ancDetailsViewModel
      .fetchCQLLibraryData(parser, fhirResourceDataSource, LIBRARY_URL)
      .observe(viewLifecycleOwner, this::handleCQLLibraryData)
  }

  fun loadCQLHelperData() {
    ancDetailsViewModel
      .fetchCQLFhirHelperData(parser, fhirResourceDataSource, HELPER_URL)
      .observe(viewLifecycleOwner, this::handleCQLHelperData)
  }

  fun loadCQLValueSetData() {
    ancDetailsViewModel
      .fetchCQLValueSetData(parser, fhirResourceDataSource, VALUE_SET_URL)
      .observe(viewLifecycleOwner, this::handleCQLValueSetData)
  }

  fun loadCQLPatientData() {
    ancDetailsViewModel
      .fetchCQLPatientData(parser, fhirResourceDataSource, "$PATIENT_URL$patientId/\$everything")
      .observe(viewLifecycleOwner, this::handleCQLPatientData)
  }

  fun loadMeasureEvaluateLibrary() {
    ancDetailsViewModel
      .fetchCQLMeasureEvaluateLibraryAndValueSets(
        parser,
        fhirResourceDataSource,
        MEASURE_EVALUATE_LIBRARY_URL,
        MEASURE_RESOURCE_TYPE_URL
      )
      .observe(viewLifecycleOwner, this::handleMeasureEvaluateLibrary)
  }

  fun loadMeasureEvaluatePatient() {
    ancDetailsViewModel
      .fetchCQLPatientData(
        parser,
        fhirResourceDataSource,
        "$PATIENT_URL$patientId/\$everything"
      )
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
    val parameters =
      libraryEvaluator.runCql(
        libraryData,
        helperData,
        valueSetData,
        testData,
        evaluatorId,
        contextCQL,
        contextLabel
      )
    val jsonObject = JSONObject(parameters)
    textView_CQLResults.text = jsonObject.toString(4)
    textView_CQLResults.visibility = View.VISIBLE
  }

  fun handleMeasureEvaluatePatient(auxPatientData: String) {
    testData = libraryEvaluator.processCQLPatientBundle(auxPatientData)
    var patientResources: ArrayList<String> = ArrayList()
    patientResources.add(testData)
    val parameters =
      measureEvaluator.runMeasureEvaluate(
        measureEvaluateLibraryData,
        patientResources,
        CQL_MEASURE_REPORT_URL,
        CQL_MEASURE_REPORT_START_DATE,
        CQL_MEASURE_REPORT_END_DATE,
        CQL_MEASURE_REPORT_REPORT_TYPE,
        CQL_MEASURE_REPORT_SUBJECT
      )
    val jsonObject = JSONObject(parameters)
    textView_CQLResults.text = jsonObject.toString(4)
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

  fun getAncPatientRepository(): AncPatientRepository {
    return AncPatientRepository(
      (requireActivity().application as AncApplication).fhirEngine,
      AncPatientItemMapper
    )
  }
}
