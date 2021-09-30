package org.smartregister.fhircore.quest.ui.patient.register

import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.configuration.view.loadRegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.quest.R

class PatientRegisterActivity : BaseRegisterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureViews(
            applicationContext.loadRegisterViewConfiguration("quest-patient-register")
        )
    }

    override fun sideMenuOptions(): List<SideMenuOption> {
        return listOf(
            SideMenuOption(
                itemId = 1000,//TODO
                titleResource = R.string.app_name,
                iconResource = ContextCompat.getDrawable(this, R.drawable.ic_menu)!!,
                opensMainRegister = true,
                countMethod = { runBlocking { 1000 } }
            )
        )
    }

    override fun onSideMenuOptionSelected(item: MenuItem): Boolean {
        return true
    }

    override fun supportedFragments(): List<Fragment> {
        return emptyList()
    }
}