package org.smartregister.fhircore.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.fhir.FhirEngine
import kotlinx.android.synthetic.main.fragment_patient_details.*
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.RecordVaccineActivity
import org.smartregister.fhircore.adapter.PatientImmunizationsAdapter
import org.smartregister.fhircore.util.createFactory
import org.smartregister.fhircore.util.extractAge
import org.smartregister.fhircore.util.extractGender
import org.smartregister.fhircore.util.extractName
import org.smartregister.fhircore.util.hide
import org.smartregister.fhircore.util.show
import org.smartregister.fhircore.util.toImmunizationItems
import org.smartregister.fhircore.viewmodel.PatientDetailsViewModel

class PatientDetailsFragment private constructor() : Fragment() {

    private lateinit var fhirEngine: FhirEngine

    private lateinit var patientDetailsViewModel: PatientDetailsViewModel

    private val patientImmunizationsAdapter = PatientImmunizationsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_patient_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val patientId = arguments?.getString(PATIENT_ID) ?: ""

        setupViews(patientId)

        fhirEngine = FhirApplication.fhirEngine(requireContext())

        patientDetailsViewModel = ViewModelProvider(
            this, PatientDetailsViewModel(
                fhirEngine = fhirEngine, patientId = patientId
            ).createFactory()
        )[PatientDetailsViewModel::class.java]

        patientDetailsViewModel.patientDemographics.observe(
            viewLifecycleOwner, this::handlePatientDemographics
        )

        patientDetailsViewModel.patientImmunizations.observe(
            viewLifecycleOwner, this::handleImmunizations
        )
    }

    override fun onResume() {
        super.onResume()
        patientDetailsViewModel.run {
            fetchDemographics()
            fetchImmunizations()
        }
    }

    private fun setupViews(patientId: String) {
        immunizationsListView.apply {
            adapter = patientImmunizationsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        recordVaccineButton.setOnClickListener {
            startActivity(
                Intent(requireContext(), RecordVaccineActivity::class.java)
                    .putExtras(
                        RecordVaccineActivity.getExtraBundles(
                            title = getString(R.string.record_vaccine),
                            patientId = patientId
                        )
                    )
            )
        }
    }

    private fun handlePatientDemographics(patient: Patient) {
        with(patient) {
            patientNameTextView.text = extractName()
            patientGenderTextView.text = extractGender(requireContext())
            patientAgeTextView.text = getString(R.string.age, extractAge())
        }
    }

    private fun handleImmunizations(immunizations: List<Immunization>) {
        when {
            immunizations.isEmpty() -> {
                toggleImmunizationStatus(fullyImmunized = false)
                noVaccinesTextView.show()
                immunizationsListView.hide()
                recordVaccineButton.show()
                showQRCodeButton.hide()
                reportAdverseEventButton.hide()
            }
            immunizations.size == 1 -> {
                toggleImmunizationStatus(fullyImmunized = false)
                populateImmunizationList(immunizations)
                recordVaccineButton.show()
                showQRCodeButton.hide()
                reportAdverseEventButton.show()
                noVaccinesTextView.hide()
            }
            else -> {
                toggleImmunizationStatus(fullyImmunized = true)
                populateImmunizationList(immunizations)
                noVaccinesTextView.hide()
                recordVaccineButton.hide()
                showQRCodeButton.show()
                reportAdverseEventButton.show()
            }
        }
    }

    private fun populateImmunizationList(immunizations: List<Immunization>) {
        immunizationsListView.show()
        patientImmunizationsAdapter.submitList(
            immunizations.toImmunizationItems(requireContext())
        )
    }

    private fun toggleImmunizationStatus(fullyImmunized: Boolean = false) {
        immuneStatusImageView.background = if (fullyImmunized)
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_check)
        else ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)

        immuneTextView.apply {
            text =
                if (fullyImmunized) getString(R.string.immune) else getString(R.string.not_immune)
            setTextColor(
                if (fullyImmunized) ContextCompat.getColor(requireContext(), R.color.immune)
                else ContextCompat.getColor(requireContext(), R.color.not_immune)
            )
        }
    }

    companion object {
        fun newInstance(bundle: Bundle = Bundle()) =
            PatientDetailsFragment().apply { arguments = bundle }

        const val PATIENT_ID = "patientId"
    }
}