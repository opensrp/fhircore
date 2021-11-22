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

package org.smartregister.fhircore.engine.util

import android.content.Context
import org.smartregister.fhircore.engine.util.extension.decodeJson

object AssetUtil {
  /** Read a file from the assets directory and decode to type T */
  inline fun <reified T> decodeAsset(fileName: String, context: Context): T =
    context.assets.open(fileName).bufferedReader().use { it.readText() }.decodeJson()
}
