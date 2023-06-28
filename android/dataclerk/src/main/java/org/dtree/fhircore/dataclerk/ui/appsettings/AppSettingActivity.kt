package org.dtree.fhircore.dataclerk.ui.appsettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import org.dtree.fhircore.dataclerk.R
import org.dtree.fhircore.dataclerk.ui.theme.FhircoreandroidTheme
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import javax.inject.Inject

@AndroidEntryPoint
class AppSettingActivity : ComponentActivity() {
    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    @Inject
    lateinit var dispatcherProvider: DispatcherProvider
    val appSettingViewModel: AppSettingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FhircoreandroidTheme {
                LoaderDialog(dialogMessage = stringResource(R.string.initializing))
            }
        }
      }
}