package org.smartregister.fhircore.eir.ui.adverseevent

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.createFactory

class AdverseEventActivity : QuestionnaireActivity() {

  private lateinit var adverseEventViewModel: AdverseEventViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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
}