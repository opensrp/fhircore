/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest

import android.util.Log
import android.util.Log.ERROR
import io.sentry.Sentry
import timber.log.Timber

class ReleaseTree : Timber.Tree() {

  override fun isLoggable(tag: String?, priority: Int): Boolean {
    if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
      return false
    }
    return true
  }

  override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
    if (priority == ERROR || priority == Log.WARN || priority == Log.ASSERT) {
      if (throwable != null) {
        Sentry.captureException(throwable)
      }
    }
  }
}
