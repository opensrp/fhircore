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
import kotlinx.android.synthetic.main.fragment_anc_details.button_CQLEvaluate
import kotlinx.android.synthetic.main.fragment_anc_details.cardView_CQLSection
import kotlinx.android.synthetic.main.fragment_anc_details.textView_CQLResults
import kotlinx.android.synthetic.main.fragment_anc_details.textView_EvaluateCQLHeader
import org.json.JSONArray
import org.json.JSONObject
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.anc.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.anc.model.CarePlanItem
import org.smartregister.fhircore.anc.databinding.FragmentAncDetailsBinding
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.createFactory

class AncDetailsFragment private constructor() : Fragment() {

  lateinit var patientId: String
  private lateinit var fhirEngine: FhirEngine

  lateinit var ancDetailsViewModel: AncDetailsViewModel

  private lateinit var ancPatientRepository: AncPatientRepository

  private val carePlanAdapter = CarePlanAdapter()

  lateinit var binding: FragmentAncDetailsBinding

  lateinit var parser: IParser

  lateinit var fhirResourceService: FhirResourceService

  lateinit var fhirResourceDataSource: FhirResourceDataSource

  lateinit var libraryEvaluator: LibraryEvaluator

  var libraryData = ""
  var helperData = ""
  var valueSetData = ""
  var testData = ""
  val evaluatorId = "ANCRecommendationA2"
  val contextCQL = "patient"
  val contextLabel = "mom-with-anemia"

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

    ancPatientRepository =
      AncPatientRepository(
        (requireActivity().application as AncApplication).fhirEngine,
        AncPatientItemMapper
      )

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

  fun loadCQLLibraryData() {
    ancDetailsViewModel
      .fetchCQLLibraryData(parser, fhirResourceDataSource)
      .observe(viewLifecycleOwner, this::handleCQLLibraryData)
  }

  fun loadCQLHelperData() {
    ancDetailsViewModel
      .fetchCQLFhirHelperData(parser, fhirResourceDataSource)
      .observe(viewLifecycleOwner, this::handleCQLHelperData)
  }

  fun loadCQLValueSetData() {
    ancDetailsViewModel
      .fetchCQLValueSetData(parser, fhirResourceDataSource)
      .observe(viewLifecycleOwner, this::handleCQLValueSetData)
  }

  fun loadCQLPatientData() {
    ancDetailsViewModel
      .fetchCQLPatientData(parser, fhirResourceDataSource, patientId)
      .observe(viewLifecycleOwner, this::handleCQLPatientData)
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
    testData = processCQLPatientBundle(auxPatientData)
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

  fun processCQLPatientBundle(auxPatientData: String): String {
    var auxPatientDataObj = JSONObject(auxPatientData)
    var oldJSONArrayEntry = auxPatientDataObj.getJSONArray("entry")
    var newJSONArrayEntry = JSONArray()
    for (i in 0 until oldJSONArrayEntry.length() - 1) {
      var resourceType =
        oldJSONArrayEntry.getJSONObject(i).getJSONObject("resource").getString("resourceType")
      if (i != 0 && !resourceType.equals("Patient")) {
        newJSONArrayEntry.put(oldJSONArrayEntry.getJSONObject(i))
      }
    }

    auxPatientDataObj.remove("entry")
    auxPatientDataObj.put("entry", newJSONArrayEntry)

    return auxPatientDataObj.toString()
  }

  val ANC_TEST_PATIENT_ID = "e8725b4c-6db0-4158-a24d-50a5ddf1c2ed"
  fun showCQLCard() {
    if (patientId == ANC_TEST_PATIENT_ID) {
      textView_EvaluateCQLHeader.visibility = View.VISIBLE
      cardView_CQLSection.visibility = View.VISIBLE
      buttonCQLSetOnClickListener()
    }
  }
}
