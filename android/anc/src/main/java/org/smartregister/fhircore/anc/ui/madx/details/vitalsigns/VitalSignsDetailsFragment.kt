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

package org.smartregister.fhircore.anc.ui.madx.details.vitalsigns

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.NonAncPatientRepository
import org.smartregister.fhircore.anc.databinding.FragmentVitalDetailsBinding
import org.smartregister.fhircore.anc.ui.madx.details.NonAncPatientItemMapper
import org.smartregister.fhircore.anc.ui.madx.details.form.NonAncDetailsFormConfig
import org.smartregister.fhircore.anc.ui.madx.details.form.NonAncDetailsQuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.createFactory

class VitalSignsDetailsFragment private constructor() : Fragment() {

    private lateinit var patientId: String
    private lateinit var fhirEngine: FhirEngine

    lateinit var ancDetailsViewModel: VitalSignsDetailsViewModel

    private lateinit var ancPatientRepository: NonAncPatientRepository


    lateinit var binding: FragmentVitalDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_vital_details, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        patientId = arguments?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

        fhirEngine = AncApplication.getContext().fhirEngine

        ancPatientRepository =
            NonAncPatientRepository(
                (requireActivity().application as AncApplication).fhirEngine,
                NonAncPatientItemMapper
            )

        ancDetailsViewModel =
            ViewModelProvider(
                this,
                VitalSignsDetailsViewModel(ancPatientRepository, patientId = patientId).createFactory()
            )[VitalSignsDetailsViewModel::class.java]



        ancDetailsViewModel
            .fetchObservation()
            .observe(viewLifecycleOwner, this::handleObservation)

        ancDetailsViewModel
            .fetchEncounters()
            .observe(viewLifecycleOwner, this::handleEncounters)
    }

    private fun handleObservation(listObservation: List<Observation>) {
        val size = if (listObservation.isNotEmpty()) listObservation.size else 0
    }

    private fun handleEncounters(listEncounters: List<Encounter>) {
        val size = if (listEncounters.isNotEmpty()) listEncounters.size else 0
    }

    companion object {
        fun newInstance(bundle: Bundle = Bundle()) =
            VitalSignsDetailsFragment().apply { arguments = bundle }
    }

    private fun openVitalSignsMetric(patientId: String) {
        (requireActivity()).startActivity(
            Intent(requireActivity(), NonAncDetailsQuestionnaireActivity::class.java)
                .putExtras(
                    QuestionnaireActivity.requiredIntentArgs(
                        clientIdentifier = patientId,
                        form = NonAncDetailsFormConfig.ANC_VITAL_SIGNS_METRIC
                    )
                )
        )
    }

    private fun openVitalSignsStandard(patientId: String) {
        (requireActivity()).startActivity(
            Intent(requireActivity(), NonAncDetailsQuestionnaireActivity::class.java)
                .putExtras(
                    QuestionnaireActivity.requiredIntentArgs(
                        clientIdentifier = patientId,
                        form = NonAncDetailsFormConfig.ANC_VITAL_SIGNS_STANDARD
                    )
                )
        )
    }
}
