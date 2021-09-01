package org.smartregister.fhircore.engine.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.google.android.fhir.search.count
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.register.model.Language
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.ui.register.model.SyncStatus
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.runSync
import timber.log.Timber

/**
 * Subclass of [ViewModel]. This view model is responsible for updating configuration views by
 * providing [registerViewConfiguration] variable which is a [MutableLiveData] that can be observed
 * on when UI configuration changes.
 */
class RegisterViewModel(
  application: Application,
  registerViewConfiguration: RegisterViewConfiguration,
  val dispatcher: DispatcherProvider = DefaultDispatcherProvider
) : AndroidViewModel(application) {

  private val _filterValue = MutableLiveData<Pair<RegisterFilterType, Any>>()
  val filterValue
    get() = _filterValue

  private val _currentPage = MutableLiveData(0)
  val currentPage
    get() = _currentPage

  private val applicationConfiguration =
    (getApplication<Application>() as ConfigurableApplication).applicationConfiguration
  private val fhirEngine = (application as ConfigurableApplication).fhirEngine

  lateinit var languages: List<Language>

  val syncStatus = MutableLiveData(SyncStatus.NOT_SYNCING)

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

  fun runSync() =
    viewModelScope.launch(dispatcher.io()) {
      try {
        getApplication<Application>().runSync()
        syncStatus.postValue(SyncStatus.COMPLETE)
      } catch (exception: Exception) {
        Timber.e("Error syncing data", exception)
        syncStatus.postValue(SyncStatus.FAILED)
      }
    }

  suspend fun performCount(sideMenuOption: SideMenuOption): Long {
    if (sideMenuOption.countForResource &&
        sideMenuOption.entityTypePatient &&
        sideMenuOption.showCount
    ) {
      return try {
        withContext(dispatcher.io()) {
            val count = fhirEngine.count<Patient> { sideMenuOption.searchFilterLambda }.toInt()
            Timber.d("Loaded %s clients from db", count)
            count
          }
          .toLong()
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        -1
      }
    }
    return -1
  }

  fun updateFilterValue(registerFilterType: RegisterFilterType, newValue: Any) {
    _filterValue.value = Pair(registerFilterType, newValue)
  }

  fun backToPreviousPage() {
    if (_currentPage.value!! > 0) _currentPage.value = _currentPage.value?.minus(1)
  }

  fun nextPage() {
    _currentPage.value = _currentPage.value?.plus(1)
  }
}
