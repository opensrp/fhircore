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

package org.smartregister.fhircore.engine.data.remote.shared

/**
 * Tracks whether a data-entry screen (a questionnaire) is currently in the foreground.
 *
 * When a background token refresh fails, [TokenAuthenticator] would otherwise relaunch the login
 * screen with a task-clearing intent, which destroys the open questionnaire and discards whatever
 * the user has typed. The questionnaire sets this flag while it is resumed so the token failure
 * handler can defer the re-login redirect and avoid the data loss, since saving a questionnaire
 * does not need the network.
 */
object DataEntryForegroundState {
  @Volatile var questionnaireInForeground: Boolean = false
}
