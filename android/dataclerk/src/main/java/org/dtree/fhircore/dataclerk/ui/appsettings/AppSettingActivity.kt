package org.dtree.fhircore.dataclerk.ui.appsettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import org.dtree.fhircore.dataclerk.R
import org.dtree.fhircore.dataclerk.ui.theme.FhircoreandroidTheme
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingScreen
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.showToast
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
        val existingAppId =
                sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.trimEnd()

        appSettingViewModel.error.observe(appSettingActivity) { error ->
            if (!error.isNullOrEmpty()) showToast(error)
        }

        // If app exists load the configs otherwise fetch from the server
        if (!existingAppId.isNullOrEmpty()) {
            appSettingViewModel.run {
                onApplicationIdChanged(existingAppId)
                loadConfigurations(appSettingActivity)
            }
        } else {
            setContent {
                AppTheme {
                    val appId by appSettingViewModel.appId.observeAsState("")
                    val showProgressBar by appSettingViewModel.showProgressBar.observeAsState(false)
                    val error by appSettingViewModel.error.observeAsState("")
                    AppSettingScreen(
                            appId = appId,
                            onAppIdChanged = appSettingViewModel::onApplicationIdChanged,
                            fetchConfiguration = appSettingViewModel::fetchConfigurations,
                            showProgressBar = showProgressBar,
                            error = error
                    )
                }
            }
        }
      }
}