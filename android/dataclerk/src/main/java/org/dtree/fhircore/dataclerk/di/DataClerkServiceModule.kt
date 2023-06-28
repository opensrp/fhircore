package org.dtree.fhircore.dataclerk.di

import androidx.appcompat.app.AppCompatActivity
import org.smartregister.fhircore.engine.ui.login.LoginService
import javax.inject.Inject

class DataClerkServiceModule @Inject constructor() : LoginService {

    override lateinit var loginActivity: AppCompatActivity

    override fun navigateToHome() {
        // Do nothing
    }
}