package org.smartregister.fhircore.engine.ui.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration

/**
 * Subclass of [ViewModel]. This view model is responsible for updating configuration views by
 * providing [registerViewConfiguration] variable which is a [MutableLiveData] that can be observed
 * on when UI configuration changes.
 */
class BaseRegisterViewModel(registerViewConfiguration: RegisterViewConfiguration) : ViewModel() {
  val registerViewConfiguration = MutableLiveData(registerViewConfiguration)
  fun updateViewConfigurations(registerViewConfiguration: RegisterViewConfiguration) {
    this.registerViewConfiguration.value = registerViewConfiguration
  }
}
