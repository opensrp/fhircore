package org.smartregister.fhircore.eir.ui.adverseevent

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.createFactory

class AdverseEventActivity : QuestionnaireActivity() {

  private lateinit var adverseEventViewModel: AdverseEventViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_adverse_event)

    adverseEventViewModel =
      ViewModelProvider(
        this@AdverseEventActivity,
        AdverseEventViewModel(
          application,
          PatientRepository(
            (application as ConfigurableApplication).fhirEngine,
            PatientItemMapper
          )
        ).createFactory()
      ).get(AdverseEventViewModel::class.java)
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {

    lifecycleScope.launch {
      clientIdentifier?.let { identifier: String ->
        adverseEventViewModel.getPatientImmunization(identifier).observe(this@AdverseEventActivity) { oldImmunization: Immunization ->
          lifecycleScope.launch {
            questionnaire?.let { questionnaire ->
              //todo Extract Immunization and further steps
            }
          }
        }
      }
    }
  }
}