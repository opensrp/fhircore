package org.smartregister.fhircore.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import kotlinx.android.synthetic.main.activity_patient_details.*
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientDetailsFragment

class PatientDetailsActivity : AppCompatActivity() {

  private lateinit var patientId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_patient_details)
    setSupportActionBar(patientDetailsToolbar)

    if (savedInstanceState == null) {
      patientId = intent.extras?.getString(PatientDetailsFragment.PATIENT_ID) ?: ""
      supportFragmentManager
        .beginTransaction()
        .replace(
          R.id.container,
          PatientDetailsFragment.newInstance(
            bundleOf(Pair(PatientDetailsFragment.PATIENT_ID, patientId))
          )
        )
        .commitNow()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.profile_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.patient_profile_edit) {
      startActivity(
        Intent(this, QuestionnaireActivity::class.java)
          .putExtras(QuestionnaireActivity.getExtrasBundle(patientId))
      )
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}
