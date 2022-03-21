package org.smartregister.fhircore.anc.ui.family.form

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import javax.inject.Inject

@HiltViewModel
class RemoveFamilyQuestionnaireViewModel
@Inject
constructor(
    fhirEngine: FhirEngine,
    defaultRepository: DefaultRepository,
    configurationRegistry: ConfigurationRegistry,
    transformSupportServices: TransformSupportServices,
    val patientRepository: PatientRepository,
    val familyDetailRepository: FamilyDetailRepository,
    dispatcherProvider: DispatcherProvider,
    sharedPreferencesHelper: SharedPreferencesHelper,
    libraryEvaluator: LibraryEvaluator
) :
    QuestionnaireViewModel(
        fhirEngine,
        defaultRepository,
        configurationRegistry,
        transformSupportServices,
        dispatcherProvider,
        sharedPreferencesHelper,
        libraryEvaluator
    ) {

    private lateinit var reasonRemove : String
    val shouldOpenHeadDialog = MutableLiveData<Boolean>()

    val familyMembers = MutableLiveData<List<FamilyMemberItem>>()

    private suspend fun saveResponse(questionnaire: Questionnaire, questionnaireResponse: QuestionnaireResponse) {
        reasonRemove = (questionnaireResponse.item?.first()?.answer?.first()?.value as Coding).display
        saveQuestionnaireResponse(questionnaire, questionnaireResponse)
    }

    fun fetchFamilyMembers(familyId: String) {
        viewModelScope.launch { familyMembers.postValue(familyDetailRepository.fetchFamilyMembers(familyId)) }
    }

    fun changeFamilyHead(currentHead: String, newHead: String): LiveData<Boolean> {
        val changed = MutableLiveData(false)
        viewModelScope.launch {
            familyDetailRepository.familyRepository.changeFamilyHead(currentHead, newHead)
            changed.postValue(true)
        }
        return changed
    }

    fun process(
        patientId: String?,
        questionnaire: Questionnaire,
        questionnaireResponse: QuestionnaireResponse
    ) {
        viewModelScope.launch {
            patientId?.let {
                val patient = loadPatient(it)
                if (patient?.isFamilyHead() == true)  {
                    shouldOpenHeadDialog.value = true
                } else {
                    patientRepository.deletePatient(it, DeletionReason.ENTRY_IN_ERROR)
                    //finish activity
                }
            }
            saveResponse(questionnaire, questionnaireResponse)
        }
    }




}