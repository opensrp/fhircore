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

package org.smartregister.fhircore.engine.rulesengine

import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.util.APP_ID_KEY
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.localize
import timber.log.Timber

@Singleton
class RulesEngineService @Inject constructor(val sharedPreferencesHelper: SharedPreferencesHelper) {

  fun translate(raw: String): String {
    var result = raw
    try {

      val lastAppId = sharedPreferencesHelper.read(APP_ID_KEY, "")?.trimEnd()

      result = raw.localize()
    } catch (e: Exception) {

      Timber.e(e)
    } finally {
      return result
    }
  }
}
