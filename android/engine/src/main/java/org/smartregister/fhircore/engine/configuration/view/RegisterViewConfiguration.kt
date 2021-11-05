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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.loadBinaryResourceConfiguration

@Serializable
@Stable
data class RegisterViewConfiguration(
  var id: String,
  var appTitle: String,
  var filterText: String,
  var searchBarHint: String,
  var newClientButtonText: String,
  var newClientButtonStyle: String,
  var showSearchBar: Boolean = true,
  var showFilter: Boolean = true,
  var switchLanguages: Boolean = true,
  var showScanQRCode: Boolean = true,
  var showNewClientButton: Boolean = true,
  var registrationForm: String = "patient-registration",
  var showSideMenu: Boolean = true,
  var showBottomMenu: Boolean = false,
  var primaryFilter: SearchFilter? = null
) : Configuration

@Serializable
@Stable
data class SearchFilter(val key: String, val code: String, val system: String)

/**
 * A function providing a DSL for configuring [RegisterViewConfiguration]. The configurations
 * provided by this method are used on the register calling this method
 *
 * @param appTitle Sets the title of the app as displayed on the side menu
 * @param filterText Sets the text displayed on the switch view
 * @param searchBarHint Sets the text on the searchBar
 * @param newClientButtonText Sets the text on the register client button
 * @param showSearchBar Hides or shows the search bar
 * @param showFilter Hides or shows the filter view
 * @param switchLanguages Hides or shows the language picker menu option
 * @param showScanQRCode Hides or shows the scan QR code button
 * @param showNewClientButton Hides or shows the button for register new client
 * @param registrationForm Name of questionnaire form used for registration
 */
@Stable
fun Context.registerViewConfigurationOf(
  id: String = this.getString(R.string.default_app_title),
  appTitle: String = this.getString(R.string.default_app_title),
  filterText: String = this.getString(R.string.show_overdue),
  searchBarHint: String = this.getString(R.string.search_hint),
  newClientButtonText: String = this.getString(R.string.register_new_client),
  newClientButtonStyle: String = "",
  showSearchBar: Boolean = true,
  showFilter: Boolean = true,
  switchLanguages: Boolean = true,
  showScanQRCode: Boolean = true,
  showNewClientButton: Boolean = true,
  languages: List<String> = listOf("en"),
  registrationForm: String = "patient-registration",
  showSideMenu: Boolean = true,
  showBottomMenu: Boolean = false,
  primaryFilter: SearchFilter? = null
): RegisterViewConfiguration {
  return RegisterViewConfiguration(
    id = id,
    appTitle = appTitle,
    filterText = filterText,
    searchBarHint = searchBarHint,
    newClientButtonText = newClientButtonText,
    newClientButtonStyle = newClientButtonStyle,
    showSearchBar = showSearchBar,
    showFilter = showFilter,
    switchLanguages = switchLanguages,
    showScanQRCode = showScanQRCode,
    showNewClientButton = showNewClientButton,
    registrationForm = registrationForm,
    showSideMenu = showSideMenu,
    showBottomMenu = showBottomMenu,
    primaryFilter = primaryFilter
  )
}

private const val REGISTER_VIEW_CONFIG_FILE = "register_view_config.json"

fun Context.loadRegisterViewConfiguration(id: String): RegisterViewConfiguration {
  return runBlocking { loadBinaryResourceConfiguration(id) }
    ?: assets
      .open(REGISTER_VIEW_CONFIG_FILE)
      .bufferedReader()
      .use { it.readText() }
      .decodeJson<List<RegisterViewConfiguration>>()
      .let { it.firstOrNull { it.id == id } ?: it.first() }
}
