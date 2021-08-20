package org.smartregister.fhircore.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine

class PatientListViewModelFactory(
  private val application: Application,
  private val fhirEngine: FhirEngine
) : ViewModelProvider.Factory {
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(CovaxListViewModel::class.java)) {
      return CovaxListViewModel(application, fhirEngine) as T
    }
    if (modelClass.isAssignableFrom(FamilyListViewModel::class.java)) {
      return FamilyListViewModel(application, fhirEngine) as T
    }
    if (modelClass.isAssignableFrom(AncListViewModel::class.java)) {
      return AncListViewModel(application, fhirEngine) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}