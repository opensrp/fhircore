package org.smartregister.fhircore.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import com.google.android.fhir.datacapture.QuestionnaireFragment
import org.smartregister.fhircore.R
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel

class RecordVaccineActivity : AppCompatActivity() {

  private val viewModel: QuestionnaireViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_record_vaccine)

    supportActionBar!!.apply {
      title = intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY)
      setDisplayHomeAsUpEnabled(true)
    }

    // Only add the fragment once, when the activity is first created.
    if (savedInstanceState == null) {
      val fragment = QuestionnaireFragment()
      fragment.arguments =
        bundleOf(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE to viewModel.questionnaire)

      supportFragmentManager.commit { add(R.id.container, fragment,
        QuestionnaireActivity.QUESTIONNAIRE_FRAGMENT_TAG
      ) }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home ->  {
        finish()
        true
      }           //do whatever
      else -> super.onOptionsItemSelected(item)
    }
  }
}