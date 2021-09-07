package org.smartregister.fhircore.anc.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.Sync
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.AncPatientRepository
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils
import org.smartregister.fhircore.engine.util.extension.extractExtendedPatient
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

class SplashActivity : BaseMultiLanguageActivity() {
  @InternalCoroutinesApi
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.splash)

    lifecycleScope.launch {
      Sync.basicSyncJob(this@SplashActivity).stateFlow().collect {
        Timber.i("Got sync update $it")

        when (it) {
          is State.Finished -> {
            showToast(getString(R.string.sync_completed))
            delay(5000)
            startActivity(Intent(this@SplashActivity, FamilyRegisterActivity::class.java))
          }
          is State.Failed -> {
            showToast(getString(R.string.sync_failed))
            startActivity(Intent(this@SplashActivity, FamilyRegisterActivity::class.java))
          }
        }
      }
    }

    AncApplication.schedulePolling()
  }
}
