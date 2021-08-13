package org.smartregister.fhircore.eir.ui.patient.register

import android.os.Bundle
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity

class PatientRegisterActivity : BaseRegisterActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
        registerViewConfigurationOf().apply {
          appTitle = getString(R.string.covax_app)
        })
  }
}
