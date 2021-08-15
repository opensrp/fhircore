package org.smartregister.fhircore.engine.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.model.Language
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.syncData
import timber.log.Timber

/**
 * Subclass of [ViewModel]. This view model is responsible for updating configuration views by
 * providing [registerViewConfiguration] variable which is a [MutableLiveData] that can be observed
 * on when UI configuration changes.
 */
class BaseRegisterViewModel(
  application: Application,
  private val applicationConfiguration: ApplicationConfiguration,
  registerViewConfiguration: RegisterViewConfiguration,
  private val dispatcher: DispatcherProvider = DefaultDispatcherProvider
) : AndroidViewModel(application) {

  private val fhirEngine = (application as ConfigurableApplication).fhirEngine

  lateinit var languages: List<Language>

  val dataSynced = MutableLiveData(false)

  var selectedLanguage =
    MutableLiveData(
      SharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.ENGLISH.toLanguageTag())
        ?: Locale.ENGLISH.toLanguageTag()
    )
  val registerViewConfiguration = MutableLiveData(registerViewConfiguration)
  fun updateViewConfigurations(registerViewConfiguration: RegisterViewConfiguration) {
    this.registerViewConfiguration.value = registerViewConfiguration
  }

  fun loadLanguages() {
    languages =
      applicationConfiguration.languages.map { Language(it, Locale.forLanguageTag(it).displayName) }
  }

  fun syncData() =
    viewModelScope.launch(dispatcher.io()) {
      try {
        getApplication<Application>().syncData(applicationConfiguration)
        dataSynced.postValue(true)
      } catch (exception: Exception) {
        Timber.e("Error syncing data", exception)
        dataSynced.postValue(false)
      }
    }
}
