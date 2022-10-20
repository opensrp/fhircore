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

package org.smartregister.fhircore.quest

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.ui.core.inMilliseconds
import androidx.ui.core.minutes
import timber.log.Timber

class AppInActivityListener(val ignoreList: List<String>, onTimeLapse: () -> Unit) {
  val handler = Handler(Looper.getMainLooper())
  val runnable: Runnable = Runnable {
    if (ignoreList.firstOrNull { it == currentActivity?.name } == null) onTimeLapse()
  }
  var currentActivity: Class<Activity>? = null

  fun current(data: Class<Activity>) {
    currentActivity = data
  }

  fun start() {
    Timber.i("App is in background")
    handler.postDelayed(runnable, 5.minutes.inMilliseconds())
  }

  fun stop() {
    Timber.i("App is foreground")
    handler.removeCallbacks(runnable)
  }
}

interface OnInActivityListener {
  fun onTimeout()
}
