package org.smartregister.fhircore.quest.ui.patient.register

import androidx.test.filters.SmallTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule

import org.junit.Test
import org.smartregister.fhircore.quest.launchFragmentInHiltContainer

/**
 * Created by ndegwamartin on 06/02/2022.
 */

@ExperimentalCoroutinesApi
@SmallTest
@HiltAndroidTest
class PatientRegisterFragmentInstrumentedTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    // single task rule
   // @get:Rule
   // var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun navigateToDetails() {
    }

    @Test
    fun onItemClicked() {
        launchFragmentInHiltContainer<PatientRegisterFragment> {
        }
    }

    @Test
    fun performFilter() {
    }
}