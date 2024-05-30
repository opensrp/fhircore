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

package org.smartregister.fhircore.engine.trace

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase

class FirebaseAnalyticReporter : AnalyticReporter {
  private var firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

  override fun log(key: String, params: Map<String, String>) {
    firebaseAnalytics.logEvent(key) { params.forEach { entry -> param(entry.key, entry.value) } }
  }

  override fun log(key: String) {
    firebaseAnalytics.logEvent(key, null)
  }

  override fun login(id: String) {
    log(AnalyticsKeys.LOGIN)
  }
}

object AnalyticsKeys {
  const val LOGIN = "login"
  const val SEARCH = "search"
  const val TRANSFER_OUT = "transfer_out"
}
