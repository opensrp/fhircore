package org.smartregister.fhircore.engine.ui.questionnaire.items

import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.model.location.LocationHierarchy
import javax.inject.Inject

class CustomQuestItemDataProvider @Inject constructor(val sharedPreferencesHelper: SharedPreferencesHelper) {

    fun fetchLocationHierarchies(): List<LocationHierarchy> {
        return sharedPreferencesHelper.read<List<LocationHierarchy>>(SharedPreferenceKey.PRACTITIONER_LOCATION_HIERARCHIES.name) ?: listOf()
    }
}