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

package org.smartregister.fhircore.anc.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.UriType

object AncOverviewType {
  const val ANC_OVERVIEW_ID = "anc_overview"
}

const val ANC_OVERVIEW_FILE = "anc_overview_configurations.json"
val ANC_OVERVIEW_CONFIG: Gson =
  GsonBuilder()
    .registerTypeAdapter(UriType::class.java, UriTypeDeserializer())
    .registerTypeAdapter(CodeType::class.java, CodeTypeDeserializer())
    .create()

fun Context.loadRegisterConfigAnc(id: String): AncOverviewConfiguration {
  val json = assets.open(ANC_OVERVIEW_FILE).bufferedReader().use { it.readText() }

  val type = object : TypeToken<List<AncOverviewConfiguration>>() {}.type

  return ANC_OVERVIEW_CONFIG.fromJson<List<AncOverviewConfiguration>>(json, type).first {
    it.id == id
  }
}
