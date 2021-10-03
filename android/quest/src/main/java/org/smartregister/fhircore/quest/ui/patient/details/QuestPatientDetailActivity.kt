package org.smartregister.fhircore.quest.ui.patient.details

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.data.QuestPatientRepository

class QuestPatientDetailActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: "1"
    val fhirEngine = (QuestApplication.getContext() as ConfigurableApplication).fhirEngine
    val repository = QuestPatientRepository(fhirEngine)
    val viewModel = QuestPatientDetailViewModel.get(this, application as QuestApplication, repository, patientId)

    viewModel.setOnBackPressListener(this::onBackPressListener)
    viewModel.setOnMenuItemClickListener(this::onMenuItemClickListener)

    setContent { AppTheme { QuestPatientDetailScreen(viewModel) } }
  }

  private fun onBackPressListener() {
    finish()
  }

  private fun onMenuItemClickListener(menuItem: String) {
    startActivity(Intent(
      this, QuestPatientTestResultActivity::class.java
    ).apply {
      putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)
    })
  }
}
