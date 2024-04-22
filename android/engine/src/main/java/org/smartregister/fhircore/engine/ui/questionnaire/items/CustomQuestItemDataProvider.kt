package org.smartregister.fhircore.engine.ui.questionnaire.items

import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.model.location.LocationHierarchy
import timber.log.Timber
import javax.inject.Inject

class CustomQuestItemDataProvider @Inject constructor(val sharedPreferencesHelper: SharedPreferencesHelper) {

    fun fetchLocationHierarchies(): List<LocationHierarchy> {
        return try {
            sharedPreferencesHelper.read<List<LocationHierarchy>>(SharedPreferenceKey.PRACTITIONER_LOCATION_HIERARCHIES.name) ?: listOf()
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }
    }
}