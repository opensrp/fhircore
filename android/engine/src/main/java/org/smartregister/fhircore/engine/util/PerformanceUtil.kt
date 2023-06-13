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

package org.smartregister.fhircore.engine.util

import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect
import timber.log.Timber

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 13-06-2023. */
@OptIn(ExperimentalReflectionOnLambdas::class)
suspend fun <T> logTimeTaken(funToProfile: suspend () -> T): T {
  val startTime = System.currentTimeMillis()
  val returnVal = funToProfile()
  val timeTaken = System.currentTimeMillis() - startTime

  Timber.i(
    "logTimeTaken: ${funToProfile.reflect()?.name} has taken $timeTaken ms / $timeTaken/1000 seconds"
  )

  return returnVal
}
