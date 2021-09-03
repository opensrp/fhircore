package org.smartregister.fhircore.eir.ui.adverseevent

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientRepository
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.FormConfigUtil
import org.smartregister.fhircore.engine.util.extension.createFactory

class AdverseEventActivity : BaseMultiLanguageActivity() {

  private val questionnaireViewModel: QuestionnaireViewModel by viewModels()
  private lateinit var adverseEventViewModel: AdverseEventViewModel
  lateinit var clientIdentifier: String
  private lateinit var questionnaireFormConfig: QuestionnaireFormConfig

  private lateinit var adverseEvent: ActivityResultLauncher<QuestionnaireFormConfig>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_adverse_event)
    clientIdentifier = intent?.getStringExtra(QuestionnaireFormConfig.COVAX_ARG_ITEM_ID)!!
    questionnaireFormConfig =
      FormConfigUtil.loadConfig(QuestionnaireFormConfig.COVAX_DETAIL_VIEW_CONFIG_ID, this)

    adverseEvent = registerForActivityResult(AdverseEventResult(clientIdentifier)) {
      it?.run { handleAdverseEventResult(it) }
    }

    supportActionBar!!.apply {
      title = questionnaireFormConfig.adverseEventsQuestionnaireTitle
      setDisplayHomeAsUpEnabled(true)
    }

    adverseEventViewModel =
      ViewModelProvider(
        this,
        AdverseEventViewModel(
          application,
          PatientRepository(
            (application as ConfigurableApplication).fhirEngine,
            PatientItemMapper
          )
        ).createFactory()
      ).get(AdverseEventViewModel::class.java)

    selectAdverseEvent()

  }

  private fun selectAdverseEvent() {
    adverseEventViewModel.getPatientImmunization(clientIdentifier)
      .observe(this@AdverseEventActivity, {
        adverseEvent.launch(questionnaireFormConfig)
      })
  }

  private fun handleAdverseEventResult(response: QuestionnaireResponse) {
    //todo: Do all the logic that we were doing after onSave of Adverse Events

    val questionnaire =
      questionnaireViewModel.loadQuestionnaire(questionnaireFormConfig.adverseEventsQuestionnaireIdentifier)

    adverseEventViewModel.getPatientImmunization(clientIdentifier).observe(this@AdverseEventActivity) { oldImmunization ->
      lifecycleScope.launch(Dispatchers.Main) {
        val immunization = ResourceMapper.extract(questionnaire, response).entry[0].resource as Immunization
      }
    }
/*
    val questionnaireResponse = fragment.getQuestionnaireResponse()

    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val questionnaire = iParser.parseResource(org.hl7.fhir.r4.model.Questionnaire::class.java, viewModel.questionnaire) as Questionnaire
    try {

      val observation = QuestionnaireUtils.extractObservation(questionnaireResponse, questionnaire, patientId)
      viewModel.saveObservation(observation)
      val immunizationReaction = QuestionnaireUtils.extractReaction(questionnaireResponse, questionnaire, observation)
        val oldImmunization = viewModel.getImmunizationOfPatient(patientId).value ?: Immunization()
        immunization.reaction = listOf(
          Immunization.ImmunizationReactionComponent().apply {
            this.dateElement = questionnaireResponse.item[2].answer[0].valueDateTimeType
            val observation = QuestionnaireUtils.asObs(questionnaireResponse.item[1], patientId, questionnaire)
            detail = Reference().apply { this.reference = "Observation/${observation.id}" }
          }
        )
        oldImmunization?.let {
          it.reaction = immunization.reaction
        }
        viewModel.saveResource(oldImmunization)

    } catch (e: Exception) {
      Timber.d("AdverseEventException:$e")
    }
*/

  }

  companion object {
    fun getExtraBundles(patientId: String): Bundle {
      return bundleOf(Pair(QuestionnaireFormConfig.COVAX_ARG_ITEM_ID, patientId))
    }
  }


}