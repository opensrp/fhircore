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

package org.smartregister.fhircore.mwcore.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.UriType

object RegisterType {
  const val CLIENT_ID = "patient_client"
  const val EXPOSED_INFANT_ID = "patient_exposed_infant"
  const val PATIENT_TYPE = "Patient_Type"
}

const val REGISTER_CONFIG_FILE = "register_configurations.json"
val GSON_REGISTER_CONFIG: Gson =
  GsonBuilder()
    .registerTypeAdapter(UriType::class.java, UriTypeDeserializer())
    .registerTypeAdapter(CodeType::class.java, CodeTypeDeserializer())
    .create()

fun Context.loadRegisterConfig(id: String): RegisterConfiguration {
  val json = assets.open(REGISTER_CONFIG_FILE).bufferedReader().use { it.readText() }

  val type = object : TypeToken<List<RegisterConfiguration>>() {}.type

  return GSON_REGISTER_CONFIG.fromJson<List<RegisterConfiguration>>(json, type).first {
    it.id == id
  }
}
