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

package org.smartregister.fhircore.engine.ui.questionnaire.items

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import org.hl7.fhir.r4.model.Binary
import org.smartregister.fhircore.engine.domain.model.LocationHierarchy
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.SystemConstants
import timber.log.Timber

class CustomQuestItemDataProvider
@Inject
constructor(
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val fhirEngine: FhirEngine,
  val gson: Gson,
) {

  fun fetchCurrentFacilityLocationHierarchies(): List<LocationHierarchy> {
    return try {
      val type = object : TypeToken<List<LocationHierarchy>>() {}.type
      sharedPreferencesHelper.readJsonArray<List<LocationHierarchy>>(
        SharedPreferenceKey.PRACTITIONER_LOCATION_HIERARCHIES.name,
        type,
      )
    } catch (e: Exception) {
      Timber.e(e)
      listOf()
    }
  }

  suspend fun fetchAllFacilityLocationHierarchies(): List<LocationHierarchy> {
    return try {
      val binary: Binary = fhirEngine.get(SystemConstants.LOCATION_HIERARCHY_BINARY)
      val config = gson.fromJson(binary.content.decodeToString(), LocationHierarchy::class.java)
      config.children
    } catch (e: Exception) {
      Timber.e(e)
      listOf()
    }
  }
}
