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

package org.smartregister.fhircore.engine.navigation

object NavigationArg {
  const val FEATURE = "feature"
  const val HEALTH_MODULE = "healthModule"
  const val SCREEN_TITLE = "screenTitle"
  const val HOME_ROUTE_PATH =
    "?feature={$FEATURE}&healthModule={$HEALTH_MODULE}&screenTitle={$SCREEN_TITLE}"
}
