/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.anc.ui.anccare.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.*
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale.filter

class AncDetailsViewModel(
    var dispatcher: DispatcherProvider = DefaultDispatcherProvider,
    val fhirEngine: FhirEngine,
    val patientId: String
) : ViewModel() {

    val patientDemographics = MutableLiveData<Patient>()
    val patientCarePlan = MutableLiveData<List<CarePlan>>()

    // Todo migrate to PatientRepository to follow repository pattern
    fun fetchDemographics() {
        if (patientId.isNotEmpty())
            viewModelScope.launch(dispatcher.io()) {
                val patient = fhirEngine.load(Patient::class.java, patientId)
                patientDemographics.postValue(patient)
            }
    }


    // Todo dynamically get carePlan once stored on FHIR and will be migrated to PatientRepository to follow repository pattern
    fun fetchCarePlan(patientId: String, qJson: String?) {
        val iParser: IParser = FhirContext.forR4().newJsonParser()
        if (patientId.isNotEmpty())
            viewModelScope.launch(dispatcher.io()) {
                val listCarePlan = arrayListOf<CarePlan>()
                val carePlan = iParser.parseResource(qJson) as CarePlan
                listCarePlan.add(carePlan)
                patientCarePlan.postValue(listCarePlan)
            }
    }

}
