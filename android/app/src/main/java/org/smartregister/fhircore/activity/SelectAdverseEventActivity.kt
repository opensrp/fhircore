package org.smartregister.fhircore.activity

import android.os.Bundle
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.QuestionnaireFragment
import org.hl7.fhir.r4.model.Questionnaire
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.util.QuestionnaireUtils
import org.smartregister.fhircore.viewmodel.AdverseEventViewModel
import org.smartregister.fhircore.viewmodel.AdverseEventViewModelFactory
import timber.log.Timber

class SelectAdverseEventActivity : MultiLanguageBaseActivity() {

  lateinit var viewModel: AdverseEventViewModel
  lateinit var fhirEngine: FhirEngine
  private lateinit var fragment: QuestionnaireFragment
  lateinit var patientId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_select_adverse_event)

    supportActionBar!!.hide()
    patientId = intent?.getStringExtra(PATIENT_ID)!!
    fhirEngine = FhirApplication.fhirEngine(this)
    viewModel = ViewModelProvider(this, AdverseEventViewModelFactory(this, this.application, fhirEngine, Bundle().apply { putString(QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY, "adverse-event.json") })).get(AdverseEventViewModel::class.java)

    // Only add the fragment once, when the activity is first created.
    if (savedInstanceState == null) {
      fragment = QuestionnaireFragment()
      fragment.arguments =
        bundleOf(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE to viewModel.questionnaire)

      supportFragmentManager.commit { add(R.id.container, fragment, QuestionnaireActivity.QUESTIONNAIRE_FRAGMENT_TAG) }
    }

    findViewById<Button>(R.id.btn_save_adverse_event).setOnClickListener {
      val questionnaireResponse = fragment.getQuestionnaireResponse()

      val iParser: IParser = FhirContext.forR4().newJsonParser()
      val questionnaire = iParser.parseResource(org.hl7.fhir.r4.model.Questionnaire::class.java, viewModel.questionnaire) as Questionnaire
      try {

        val observation = QuestionnaireUtils.extractObservation(questionnaireResponse, questionnaire, patientId)
        viewModel.saveObservation(observation)
        val immunizationReaction = QuestionnaireUtils.extractReaction(questionnaireResponse, questionnaire, observation)
      /*  val oldImmunization = viewModel.getImmunizationOfPatient(patientId).value ?: Immunization()
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
        viewModel.saveResource(oldImmunization)*/

      } catch (e: Exception) {
        Timber.d("AdverseEventException:$e")
      }

    }
  }

}