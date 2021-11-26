package org.smartregister.fhircore.mwcore.ui.patient.register

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.loadRegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.ui.patient.register.components.fragments.ClientRegisterFragment
import org.smartregister.fhircore.mwcore.ui.patient.register.components.fragments.ContactRegisterFragment
import org.smartregister.fhircore.mwcore.ui.patient.register.components.fragments.ExposedInfantRegisterFragment

class ContactRegisterActivity: BaseRegisterActivity() {
    private val contactsRegister = "mwcore-app-contacts-register"
    private lateinit var registerViewConfiguration: RegisterViewConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerViewConfiguration = loadRegisterViewConfiguration(contactsRegister)
        configureViews(registerViewConfiguration)
    }

    override fun supportedFragments(): Map<String, Fragment> = mapOf(Pair(ContactRegisterFragment.TAG, ContactRegisterFragment()))

    override fun mainFragmentTag() = ContactRegisterFragment.TAG

    override fun sideMenuOptions(): List<SideMenuOption> {
        return listOf(
            SideMenuOption(
                R.id.nav_drawer_clients,
                R.string.register_clients,
                AppCompatResources.getDrawable(this, R.drawable.ic_baseline_person_24)!!,
            ),
            SideMenuOption(
                R.id.nav_drawer_contacts,
                R.string.register_contacts,
                AppCompatResources.getDrawable(this, R.drawable.ic_baseline_group_24)!!,
            ),
            SideMenuOption(
                R.id.nav_drawer_exposed_infants,
                R.string.register_exposed_infants,
                AppCompatResources.getDrawable(this, R.drawable.ic_baseline_child_care_24)!!,
            )
        )
    }

    override fun onNavigationOptionItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_drawer_clients -> openPage(ClientRegisterFragment.TAG)
            R.id.nav_drawer_contacts -> openPage(ContactRegisterFragment.TAG)
            R.id.nav_drawer_exposed_infants -> openPage(ExposedInfantRegisterFragment.TAG)
        }
        return true
    }

    private fun openPage(tag: String) {
        val intent: Intent = when(tag) {
            ClientRegisterFragment.TAG -> Intent(this, ClientRegisterActivity::class.java)
            ContactRegisterFragment.TAG -> Intent(this, ContactRegisterActivity::class.java)
            ExposedInfantRegisterFragment.TAG -> Intent(this, ExposedInfantRegisterActivity::class.java)
            else -> Intent(this, ClientRegisterActivity::class.java)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}