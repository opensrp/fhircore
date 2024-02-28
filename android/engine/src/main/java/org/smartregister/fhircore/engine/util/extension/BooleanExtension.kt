package org.smartregister.fhircore.engine.util.extension

import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

fun appIdExistsAndIsNotNull (sharedPreferencesHelper: SharedPreferencesHelper): Boolean{
    val appId = sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.trimEnd()
    return appId!= null && appId.trim().endsWith(ConfigurationRegistry.DEBUG_SUFFIX, ignoreCase = true)
}