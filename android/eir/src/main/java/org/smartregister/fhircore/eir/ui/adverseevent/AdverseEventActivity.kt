package org.smartregister.fhircore.eir.ui.adverseevent

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_adverse_event.*
import kotlinx.android.synthetic.main.activity_patient_details.*
import kotlinx.android.synthetic.main.fragment_patient_details.*
import org.hl7.fhir.r4.model.Immunization
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.databinding.ActivityAdverseEventBinding
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.details.toImmunizationAdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show

class AdverseEventActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String
  private lateinit var adverseEventViewModel: AdverseEventViewModel
  private val adverseEventAdapter = AdverseEventAdapter()
  private lateinit var activityAdverseEventBinding: ActivityAdverseEventBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityAdverseEventBinding = DataBindingUtil.setContentView(this, R.layout.activity_adverse_event)
    setSupportActionBar(activityAdverseEventBinding.adverseEventsToolbar)

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    adverseEventViewModel =
      ViewModelProvider(
        this@AdverseEventActivity,
        AdverseEventViewModel(
          application,
          questionnaireConfig = QuestionnaireConfig("adverse-event", "Adverse Event", "1568"),
          PatientRepository(
            (application as ConfigurableApplication).fhirEngine,
            PatientItemMapper
          )
        ).createFactory()
      ).get(AdverseEventViewModel::class.java)

    setupViews()

    adverseEventViewModel.getPatientImmunizations(patientId = patientId).observe(this@AdverseEventActivity,
      this::handleImmunizations)

    activityAdverseEventBinding.adverseEventsToolbar.setNavigationOnClickListener { onBackPressed() }
  }

  private fun setupViews() {
    adverseEventListView.apply {
      adapter = adverseEventAdapter
      layoutManager = LinearLayoutManager(this@AdverseEventActivity)
    }
  }

  private fun handleImmunizations(immunizations: List<Immunization>) {
    when {
      immunizations.isEmpty() -> {
        activityAdverseEventBinding.adverseEventListView.hide()
        activityAdverseEventBinding.noAdverseEventTextView.show()
      }
      else -> {
        adverseEventViewModel.getAdverseEvents(immunizations).observe(this@AdverseEventActivity) { immunizationAdverseEvents ->
          immunizationAdverseEvents.filter { it.second.isNotEmpty() }.run {
            if (this.isEmpty()) {
              activityAdverseEventBinding.adverseEventListView.hide()
              activityAdverseEventBinding.noAdverseEventTextView.show()
            } else {
              activityAdverseEventBinding.noAdverseEventTextView.hide()
              activityAdverseEventBinding.adverseEventListView.show()
              populateAdverseEventsList(immunizations, immunizationAdverseEvents)
            }

          }
        }
      }
    }
  }

  private fun populateAdverseEventsList(immunizations: List<Immunization>, immunizationAdverseEvents: List<Pair<String, List<AdverseEventItem>>>) {
    adverseEventAdapter.submitList(immunizations.toImmunizationAdverseEventItem(this@AdverseEventActivity, immunizationAdverseEvents))
  }

  companion object {
    fun requiredIntentArgs(clientIdentifier: String) = bundleOf(
      Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier)
    )
  }
}