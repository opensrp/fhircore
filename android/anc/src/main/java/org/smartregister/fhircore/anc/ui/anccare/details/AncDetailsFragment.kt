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

import android.database.DatabaseUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.databinding.FragmentAncDetailsBinding
import org.smartregister.fhircore.anc.form.config.AncFormConfig
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName

class AncDetailsFragment private constructor() : Fragment() {

    private lateinit var fhirEngine: FhirEngine

    lateinit var ancDetailsViewModel: AncDetailsViewModel

    lateinit var binding: FragmentAncDetailsBinding

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
        val patientId = arguments?.getString(AncFormConfig.ANC_ARG_ITEM_ID) ?: ""


        fhirEngine = AncApplication.getContext().fhirEngine

        ancDetailsViewModel =
            ViewModelProvider(
                this,
                AncDetailsViewModel(fhirEngine = fhirEngine, patientId = patientId).createFactory()
            )[AncDetailsViewModel::class.java]

        binding.txtViewPatientId.text = patientId

        ancDetailsViewModel.patientDemographics.observe(
            viewLifecycleOwner,
            this::handlePatientDemographics
        )
    }

    override fun onResume() {
        super.onResume()
        ancDetailsViewModel.run {
            fetchDemographics()
        }
    }

    companion object {
        fun newInstance(bundle: Bundle = Bundle()) =
            AncDetailsFragment().apply { arguments = bundle }
    }

    private fun handlePatientDemographics(patient: Patient) {
        with(patient) {
            val patientDetails =
                extractName() + ", " + extractGender(requireContext()) + ", " + extractAge()
            binding.txtViewPatientDetails.text = patientDetails
        }
    }
}
