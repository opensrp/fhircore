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

package org.smartregister.fhircore.anc.data.madx

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.count
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.madx.model.PatientBMIItem
import org.smartregister.fhircore.anc.sdk.ResourceMapperExtended
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.ui.madx.details.form.BMIQuestionnaireActivity
import org.smartregister.fhircore.anc.util.*
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.searchActivePatients

class PatientBmiRepository(
    override val fhirEngine: FhirEngine,
    override val domainMapper: DomainMapper<Patient, PatientBMIItem>,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Patient, PatientBMIItem> {

    private val registerConfig =
        AncApplication.getContext().loadRegisterConfig(RegisterType.FAMILY_REGISTER_ID)

    private val ancPatientRepository = AncPatientRepository(fhirEngine, AncItemMapper)

    private val resourceMapperExtended = ResourceMapperExtended(fhirEngine)

    override suspend fun loadData(
        query: String,
        pageNumber: Int,
        loadAll: Boolean
    ): List<PatientBMIItem> {
        return withContext(dispatcherProvider.io()) {
            val patients =
                fhirEngine.searchActivePatients(
                    query = query,
                    pageNumber = pageNumber,
                    loadAll = loadAll
                )
            patients.map { domainMapper.mapToDomainModel(it) }
        }
    }

    override suspend fun countAll(): Long {
        return fhirEngine.count<Patient> { filterBy(registerConfig.primaryFilter!!) }
    }

    fun isUnitModeMetric(questionnaireResponse: QuestionnaireResponse): Boolean {
        val unitMode = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_UNIT_SELECTION)
        return (unitMode?.answer?.get(0)?.valueCoding?.code == "metric")
    }

    fun computeBMI(questionnaireResponse: QuestionnaireResponse): Double {
        try {
            val unitMode = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_UNIT_SELECTION)
            // for Standard Units
            if (unitMode?.answer?.get(0)?.valueCoding?.code == "standard") {
                val weightPounds =
                    questionnaireResponse.find(BMIQuestionnaireActivity.KEY_WEIGHT_SI)
                val heightFeets = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_HEIGHT_SI)
                val heightInches =
                    questionnaireResponse.find(BMIQuestionnaireActivity.KEY_HEIGHT_DP_SI)

                val weight = weightPounds?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
                val heightInFeets = heightFeets?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
                val heightInInches =
                    heightInches?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
                val height = (heightInFeets.toDouble() * 12) + heightInInches.toDouble()

                return computeBMIViaStandardUnits(height, weight.toDouble())
            } else {
                // for Metric Units
                val weightKgs = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_WEIGHT_MU)
                val heightCms = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_HEIGHT_MU)
                val weight = weightKgs?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
                val height = heightCms?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
                return computeBMIViaMetricUnits(height.toDouble() / 100, weight.toDouble())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return -1.0
        }
    }

    fun getInputHeight(
        questionnaireResponse: QuestionnaireResponse,
        isUnitModeMetric: Boolean
    ): Double {
        return if (isUnitModeMetric) {
            val heightCms = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_HEIGHT_MU)
            val height = heightCms?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
            val heightInMeters = height.div(100)
            heightInMeters
        } else {
            val heightFeets = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_HEIGHT_SI)
            val heightInches = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_HEIGHT_DP_SI)
            val heightInFeets = heightFeets?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
            val heightInInches = heightInches?.answer?.firstOrNull()?.valueDecimalType?.value ?: 0
            val height = (heightInFeets.toDouble() * 12) + heightInInches.toDouble()
            height
        }
    }

    fun getInputWeight(
        questionnaireResponse: QuestionnaireResponse,
        isUnitModeMetric: Boolean
    ): Double {
        return if (isUnitModeMetric) {
            val weightKgs = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_WEIGHT_MU)
            weightKgs?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
        } else {
            val weightPounds = questionnaireResponse.find(BMIQuestionnaireActivity.KEY_WEIGHT_SI)
            weightPounds?.answer?.firstOrNull()?.valueDecimalType?.value?.toDouble() ?: 0.0
        }
    }

    fun calculateBMI(
        height: Double,
        weight: Double,
        isUnitModeMetric: Boolean
    ): Double {
        return try {
            if (isUnitModeMetric)
                computeBMIViaMetricUnits(heightInMeters = height, weightInKgs = weight)
            else
                computeBMIViaStandardUnits(heightInInches = height, weightInPounds = weight)
        } catch (e: Exception) {
            e.printStackTrace()
            -1.0
        }
    }

    suspend fun recordComputedBMI(
        questionnaire: Questionnaire,
        questionnaireResponse: QuestionnaireResponse,
        patientId: String,
        height: Double,
        weight: Double,
        computedBMI: Double
    ) {
        resourceMapperExtended.saveParsedResource(
            questionnaireResponse,
            questionnaire,
            patientId,
            null
        )
        ancPatientRepository.recordBmi(patientId, height, weight, computedBMI)
    }

}