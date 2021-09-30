package org.smartregister.fhirecore.quest.config

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.loadRegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhirecore.quest.robolectric.RobolectricTest

class RegisterViewConfigTest: RobolectricTest() {

    @Test
    fun testLoadRegisterViewConfigShouldReturnValidConfig(){
        val result = ApplicationProvider.getApplicationContext<Application>()
            .loadRegisterViewConfiguration("quest-patient-register")

        assertEquals("quest-patient-register",result.appId)
        assertEquals("Quest",result.appTitle)
        assertEquals("Show overdue",result.filterText)
        assertEquals("Search by ID or Name",result.searchBarHint)
        assertEquals("New Client",result.newClientButtonText)
        assertEquals(true, result.showSearchBar)
        assertEquals(true, result.showFilter)
        assertEquals(true,result.switchLanguages)
        assertEquals(true,result.showScanQRCode)
        assertEquals(true,result.showNewClientButton)
        assertEquals("patient-registration",result.registrationForm)

    }
}