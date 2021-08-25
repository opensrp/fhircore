package org.smartregister.fhircore.activity

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.QuestionnaireFragment
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseActivity
import org.smartregister.fhircore.model.AdverseEventResult
import org.smartregister.fhircore.model.CovaxDetailView
import org.smartregister.fhircore.util.QuestionnaireUtils
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.CovaxListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel
import timber.log.Timber

class AdverseEventActivity : BaseActivity() {

  private val questionnaireViewModel: QuestionnaireViewModel by viewModels()
  private lateinit var covaxListViewModel: CovaxListViewModel
  lateinit var clientIdentifier: String
  private lateinit var detailView: CovaxDetailView
  private lateinit var adverseEvent: ActivityResultLauncher<CovaxDetailView>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    clientIdentifier = intent?.getStringExtra(CovaxDetailView.COVAX_ARG_ITEM_ID)!!
    detailView =
      Utils.loadConfig(CovaxDetailView.COVAX_DETAIL_VIEW_CONFIG_ID,
        CovaxDetailView::class.java, this)

    adverseEvent = registerForActivityResult(AdverseEventResult(clientIdentifier)) {
      it?.run { handleAdverseEventResult(it) }
    }

    supportActionBar!!.apply {
      title = detailView.adverseEventsQuestionnaireTitle
      setDisplayHomeAsUpEnabled(true)
    }

    covaxListViewModel =
      ViewModelProvider(
        this,
        PatientListViewModelFactory(application, FhirApplication.fhirEngine(this))
      )
        .get(CovaxListViewModel::class.java)

    selectAdverseEvent()

  }

  private fun selectAdverseEvent() {
    covaxListViewModel.getPatientItem(clientIdentifier)
      .observe(this, { adverseEvent.launch(detailView) })
  }

  private fun handleAdverseEventResult(it: QuestionnaireResponse) {
    //todo: Do all the logic that we were doing after onSave of Adverse Events

    val questionnaire =
      questionnaireViewModel.loadQuestionnaire(detailView.adverseEventsQuestionnaireIdentifier)
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

  override fun getContentLayout(): Int {
    return R.layout.activity_adverse_event
  }

  companion object {
    fun getExtraBundles(patientId: String): Bundle {
      return bundleOf(Pair(CovaxDetailView.COVAX_ARG_ITEM_ID, patientId))
    }
  }


}