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

package org.smartregister.fhircore.engine.performance

import timber.log.Timber

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 22-02-2023. */
class Timer(val startString: String = "", val methodName: String = "") {

  var startTime = 0L
  var stopTime = 0L

  init {
    startTime = System.currentTimeMillis()
    Timber.e(startString)
    Timber.e("Starting $methodName")
  }

  fun stop() {
    stopTime = System.currentTimeMillis()
    val timeTaken = stopTime - startTime
    val formattedTime = "%,d".format(timeTaken)
    Timber.e("Finished $methodName and it took $formattedTime ms / ${timeTaken / 1000} secs")
  }
}
