package org.smartregister.fhircore.activity

import android.app.Activity
import android.view.MenuInflater
import android.view.MenuItem
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.DisplayName
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.R
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class PatientDetailsActivityTest : ActivityRobolectricTest() {

    private lateinit var patientDetailsActivity: PatientDetailsActivity

    private lateinit var patientDetailsActivitySpy: PatientDetailsActivity

    @Before
    fun setUp() {
        patientDetailsActivity =
            Robolectric.buildActivity(PatientDetailsActivity::class.java, null).create().get()
        patientDetailsActivitySpy = spyk(objToCopy = patientDetailsActivity)
    }

    @Test
    @DisplayName("Should start patient details activity")
    fun testPatientActivityShouldNotNull() {
        Assert.assertNotNull(patientDetailsActivity)
    }

    @Test
    @DisplayName("Should inflate menu and return true")
    fun testThatMenuIsCreated() {

        val menuInflater = mockk<MenuInflater>()

        every { patientDetailsActivitySpy.menuInflater } returns menuInflater
        every { menuInflater.inflate(any(), any()) } returns Unit

        Assert.assertTrue(patientDetailsActivitySpy.onCreateOptionsMenu(null))
    }

    @Test
    @DisplayName("Should start QuestionnaireActivity when menu edit is selected")
    fun testThatMenuItemListenerWorks() {
        val menuItem = mockk<MenuItem>(relaxed = true)
        every { menuItem.itemId } returns R.id.patient_profile_edit
        every { patientDetailsActivitySpy.startActivity(any()) } just runs
        Assert.assertTrue(patientDetailsActivitySpy.onOptionsItemSelected(menuItem))
    }

    override fun getActivity(): Activity {
        return patientDetailsActivity
    }
}