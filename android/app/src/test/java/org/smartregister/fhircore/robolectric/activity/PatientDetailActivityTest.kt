package org.smartregister.fhircore.robolectric.activity

import android.app.Activity
import android.view.MenuItem
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PatientDetailActivity
import org.smartregister.fhircore.fragment.PatientDetailFragment
import org.smartregister.fhircore.robolectric.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class PatientDetailActivityTest : ActivityRobolectricTest() {

  private lateinit var patientDetailActivity: PatientDetailActivity

  @Before fun setUp() {
    patientDetailActivity = Robolectric.buildActivity(PatientDetailActivity::class.java, null).create().resume().get()
  }

  @Test fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(patientDetailActivity)
  }

  @Test fun testOnOptionsItemSelectedShouldCallEditPatientInfo() {
    val menuItem = Mockito.mock(MenuItem::class.java)
    Mockito.`when`(menuItem.itemId).thenReturn(R.id.patient_profile_edit)

    val fragment = Mockito.mock(PatientDetailFragment::class.java)
    patientDetailActivity.fragment = fragment

    patientDetailActivity.onOptionsItemSelected(menuItem)
    Mockito.verify(fragment, Mockito.times(1)).editPatient()
  }

  override fun getActivity(): Activity {
    return patientDetailActivity
  }
}