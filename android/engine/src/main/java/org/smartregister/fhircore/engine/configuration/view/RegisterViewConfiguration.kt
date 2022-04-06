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
  val filterText: String = "",
  val searchBarHint: String = "",
  val newClientButtonText: String = "",
  val newClientButtonStyle: String = "",
  val showSearchBar: Boolean = true,
  val showFilter: Boolean = true,
  val showScanQRCode: Boolean = true,
  val showNewClientButton: Boolean = true,
  val registrationForm: String = "patient-registration",
  val showSideMenu: Boolean = true,
  val showBottomMenu: Boolean = false,
  val useLabel: Boolean = true,
  val showHeader: Boolean = true,
  val showFooter: Boolean = true,
  val primaryFilter: SearchFilter? = null,
  val bottomNavigationOptions: List<NavigationOption>? = null
) : Configuration

/**
 * A function providing a DSL for configuring [RegisterViewConfiguration]. The configurations
 * provided by this method are used on the register calling this method
 *
 * @param appId Sets Application ID
 * @param classification Categorize this configuration type
 * @param appTitle Sets the title of the app as displayed on the side menu
 * @param filterText Sets the text displayed on the switch view
 * @param searchBarHint Sets the text on the searchBar
 * @param newClientButtonText Sets the text on the register client button
 * @param showSearchBar Hides or shows the search bar
 * @param showFilter Hides or shows the filter view
 * @param showScanQRCode Hides or shows the scan QR code button
 * @param showNewClientButton Hides or shows the button for register new client
 * @param registrationForm Name of questionnaire form used for registration
 * @param showSideMenu Hide or show the side menu
 * @param showBottomMenu Hide or show the Bottom navigation menu
 * @param useLabel Use label if true, otherwise use icon
 * @param showHeader Hide or show the header
 * @param showFooter Hide or show the footer
 */
@Stable
fun Context.registerViewConfigurationOf(
  appId: String = "",
  classification: String = "",
  appTitle: String = this.getString(R.string.default_app_title),
  filterText: String = this.getString(R.string.show_overdue),
  searchBarHint: String = this.getString(R.string.search_hint),
  newClientButtonText: String = this.getString(R.string.register_new_client),
  newClientButtonStyle: String = "",
  showSearchBar: Boolean = true,
  showFilter: Boolean = true,
  showScanQRCode: Boolean = true,
  showNewClientButton: Boolean = true,
  registrationForm: String = "patient-registration",
  showSideMenu: Boolean = true,
  showBottomMenu: Boolean = false,
  useLabel: Boolean = true,
  showHeader: Boolean = true,
  showFooter: Boolean = true,
  bottomNavigationOptions: List<NavigationOption>? = null
): RegisterViewConfiguration {
  return RegisterViewConfiguration(
    appId = appId,
    classification = classification,
    appTitle = appTitle,
    filterText = filterText,
    searchBarHint = searchBarHint,
    newClientButtonText = newClientButtonText,
    newClientButtonStyle = newClientButtonStyle,
    showSearchBar = showSearchBar,
    showFilter = showFilter,
    showScanQRCode = showScanQRCode,
    showNewClientButton = showNewClientButton,
    registrationForm = registrationForm,
    showSideMenu = showSideMenu,
    showBottomMenu = showBottomMenu,
    useLabel = useLabel,
    showHeader = showHeader,
    showFooter = showFooter,
    bottomNavigationOptions = bottomNavigationOptions
  )
}
