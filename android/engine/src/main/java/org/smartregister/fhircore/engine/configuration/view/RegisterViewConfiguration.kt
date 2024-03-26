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

package org.smartregister.fhircore.engine.configuration.view

import android.content.Context
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.Configuration

// TODO remove primaryFilter and other unused properties

@Serializable
@Stable
data class RegisterViewConfiguration(
  override val appId: String = "",
  override val classification: String = "",
  val appTitle: String = "",
  val newClientButtonText: String = "",
  val registrationForm: String = "patient-registration",
) : Configuration

/**
 * A function providing a DSL for configuring [RegisterViewConfiguration]. The configurations
 * provided by this method are used on the register calling this method
 *
 * @param appId Sets Application ID
 * @param classification Categorize this configuration type
 * @param appTitle Sets the title of the app as displayed on the side menu
 * @param newClientButtonText Sets the text on the register client button
 * @param registrationForm Name of questionnaire form used for registration
 */
@Stable
fun Context.registerViewConfigurationOf(
  appId: String = "",
  classification: String = "",
  appTitle: String = this.getString(R.string.default_app_title),
  newClientButtonText: String = this.getString(R.string.register_new_client),
  registrationForm: String = "patient-registration",
): RegisterViewConfiguration {
  return RegisterViewConfiguration(
    appId = appId,
    classification = classification,
    appTitle = appTitle,
    newClientButtonText = newClientButtonText,
    registrationForm = registrationForm,
  )
}
