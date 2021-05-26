package org.smartregister.fhircore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.util.Utils
import timber.log.Timber

class BaseViewModel(application: Application) : AndroidViewModel(application) {
    private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application)

    var covaxClientsCount = MutableLiveData(0)

    fun loadClientCount() {
        Timber.d("Loading client counts")

        viewModelScope.launch {
            var p: List<Patient> = fhirEngine.search {
                Utils.addBasePatientFilter(this)

                apply {

                }
                sort(Patient.GIVEN, Order.ASCENDING)
            }

            covaxClientsCount.value =
                p.size //TODO use a proper count query after Google devs respond

            Timber.d("Loaded %s clients from db", p.size)
        }
    }
}