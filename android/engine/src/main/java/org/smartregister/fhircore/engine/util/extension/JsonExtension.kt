/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

val json = Json {
  encodeDefaults = true
  ignoreUnknownKeys = true
  isLenient = true
  useAlternativeNames = true
}

/**
 * Decode string to an entity of type [T]
 * @param jsonInstance the custom json object used to decode the specified string
 * @return the decoded object of type [T]
 */
inline fun <reified T> String.decodeJson(jsonInstance: Json? = null): T =
  jsonInstance?.decodeFromString(this) ?: json.decodeFromString(this)

/**
 * Decode string to an entity of type [T] or return null if json is invalid
 * @param jsonInstance the custom json object used to decode the specified string
 * @return the decoded object of type [T]
 */
inline fun <reified T> String.tryDecodeJson(jsonInstance: Json? = null): T? =
  kotlin.runCatching { this.decodeJson<T>(jsonInstance) }.onFailure { Timber.w(it) }.getOrNull()

/**
 * Encode the type [T] into a Json string
 * @param jsonInstance the custom json object used to decode the specified string
 * @return the encoded json string from given type [T]
 */
inline fun <reified T> T.encodeJson(jsonInstance: Json? = null): String =
  jsonInstance?.encodeToString(this) ?: json.encodeToString(this)
