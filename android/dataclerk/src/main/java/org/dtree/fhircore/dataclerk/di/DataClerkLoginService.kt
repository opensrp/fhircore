package org.dtree.fhircore.dataclerk.di

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import org.dtree.fhircore.dataclerk.ui.main.AppMainActivity
import org.smartregister.fhircore.engine.ui.login.LoginService
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
class DataClerkLoginService @Inject constructor() : LoginService {

    override lateinit var loginActivity: AppCompatActivity

    override fun navigateToHome() {
        loginActivity.run {
            startActivity(
                    Intent(loginActivity, AppMainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
            )
            finish()
        }
    }
}