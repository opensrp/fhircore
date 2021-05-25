package org.smartregister.fhircore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilter
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.FhirApplication
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import timber.log.Timber

class BaseViewModel(application: Application) : AndroidViewModel(application) {
    private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application)

    var covaxClientsCount = MutableLiveData(0)

    fun loadClientCount() {
        viewModelScope.launch {
            var p: List<Patient> = fhirEngine.search {
                filter(Patient.ADDRESS_CITY) {
                    prefix = ParamPrefixEnum.EQUAL
                    value = "NAIROBI"
                }

                apply {

                }
                sort(Patient.GIVEN, Order.ASCENDING)
            }

            Timber.i(p.toString())

            covaxClientsCount.value = covaxClientsCount.value?.plus(1) //p.size //TODO use a proper count query
        }
    }
}