package org.smartregister.fhircore.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.shadow.FhirApplicationShadow

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 03-07-2021.
 */
@Config(shadows = [FhirApplicationShadow::class])
class QuestionnaireViewModelTest : RobolectricTest() {

    lateinit var questionnaireViewModel: QuestionnaireViewModel

    @Before
    fun setUp() {
        //MockKAnnotations.init(this, relaxUnitFun = true)
        questionnaireViewModel = spyk(QuestionnaireViewModel(ApplicationProvider.getApplicationContext(), SavedStateHandle()))
    }

    @Test
    fun `saveBundleResources() should call saveResources()`() {
        val bundle = Bundle()
        val size = 23

        for (i in 1..size) {
            val bundleEntry = Bundle.BundleEntryComponent()
            bundleEntry.resource = Patient()
            bundle.addEntry(bundleEntry)
        }
        bundle.total = size

        every { questionnaireViewModel.saveResource(any()) } just runs

        // call the method under test
        questionnaireViewModel.saveBundleResources(bundle)

        verify(exactly = size) {
            questionnaireViewModel.saveResource(any())
        }
    }

    @Test
    fun `saveBundleResources() should call saveResources and inject resourceId()`() {
        val bundle = Bundle()
        val size = 5
        val resource = slot<Resource>()
        val resourceId = "my-res-id"

        val bundleEntry = Bundle.BundleEntryComponent()
        bundleEntry.resource = Patient()
        bundle.addEntry(bundleEntry)
        bundle.total = size

        every { questionnaireViewModel.saveResource(any()) } just runs

        // call the method under test
        questionnaireViewModel.saveBundleResources(bundle)

        verify(exactly = 1) {
            questionnaireViewModel.saveResource(capture(resource))
        }

        Assert.assertEquals(resourceId, resource.captured.id)

    }

    @Test
    fun fetchStructureMap() {
    }

    @Test
    fun saveExtractedResources() {
    }
}