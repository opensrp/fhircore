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

package org.smartregister.fhircore.engine.util.extension

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.json.JSONException
import org.json.JSONObject

fun Resource.toJson(parser: IParser = FhirContext.forR4().newJsonParser()): String =
  parser.encodeResourceToString(this)

fun <T : Resource> T.updateFrom(updatedResource: Resource): T {
  val jsonParser = FhirContext.forR4().newJsonParser()
  val stringJson = toJson(jsonParser)
  val originalResourceJson = JSONObject(stringJson)

  originalResourceJson.updateFrom(JSONObject(updatedResource.toJson(jsonParser)))
  return jsonParser.parseResource(this::class.java, originalResourceJson.toString())
}

@Throws(JSONException::class)
fun JSONObject.updateFrom(updated: JSONObject) {
  val keys =
    mutableListOf<String>().apply {
      keys().forEach { add(it) }
      updated.keys().forEach { add(it) }
    }

  keys.forEach { key -> updated.opt(key)?.run { put(key, this) } }
}

fun Resource.asReference() = Reference().apply { reference = "$resourceType/$logicalId"}

fun Resource.asReferenceString() = "$resourceType/$logicalId"