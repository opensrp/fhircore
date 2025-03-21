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

package org.smartregister.fhircore.quest.ui.main.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlin.math.min
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.engine.domain.model.TopScreenSectionConfig
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.event.ToolbarClickEvent
import org.smartregister.fhircore.quest.ui.shared.components.Image
import org.smartregister.fhircore.quest.ui.shared.models.SearchMode
import org.smartregister.fhircore.quest.ui.shared.models.SearchQuery
import org.smartregister.fhircore.quest.util.QrCodeScanUtils

const val DRAWER_MENU = "Drawer Menu"
const val SEARCH = "Search"
const val CLEAR = "Clear"
const val FILTER = "Filter"
const val TITLE_ROW_TEST_TAG = "titleRowTestTag"
const val TOP_ROW_ICON_TEST_TAG = "topRowIconTestTag"
const val TOP_ROW_TEXT_TEST_TAG = "topRowTextTestTag"
const val TOP_ROW_FILTER_ICON_TEST_TAG = "topRowFilterIconTestTag"
const val OUTLINED_BOX_TEST_TAG = "outlinedBoxTestTag"
const val TRAILING_ICON_TEST_TAG = "trailingIconTestTag"
const val TRAILING_ICON_BUTTON_TEST_TAG = "trailingIconButtonTestTag"
const val TRAILING_QR_SCAN_ICON_TEST_TAG = "qrCodeScanTrailingIconTestTag"
const val TRAILING_QR_SCAN_ICON_BUTTON_TEST_TAG = "qrCodeScanTrailingIconButtonTestTag"
const val LEADING_ICON_TEST_TAG = "leadingIconTestTag"
const val SEARCH_FIELD_TEST_TAG = "searchFieldTestTag"
const val TOP_ROW_TOGGLE_ICON_TEST_tAG = "topRowToggleIconTestTag"

@Composable
fun TopScreenSection(
  modifier: Modifier = Modifier,
  title: String,
  navController: NavController,
  isSearchBarVisible: Boolean,
  searchQuery: SearchQuery,
  showSearchByQrCode: Boolean = false,
  filteredRecordsCount: Long? = null,
  searchPlaceholder: String? = null,
  placeholderColor: String? = null,
  toolBarHomeNavigation: ToolBarHomeNavigation = ToolBarHomeNavigation.OPEN_DRAWER,
  onSearchTextChanged: (SearchQuery, Boolean) -> Unit = { _, _ -> },
  performSearchOnValueChanged: Boolean = true,
  isFilterIconEnabled: Boolean = false,
  topScreenSection: TopScreenSectionConfig? = null,
  decodeImage: ((String) -> Bitmap?)?,
  onClick: (ToolbarClickEvent) -> Unit = {},
) {
  val currentContext = LocalContext.current

  // Trigger search automatically on launch if text is not empty
  LaunchedEffect(Unit) {
    if (!searchQuery.isBlank()) {
      onSearchTextChanged(searchQuery, true)
    }
  }

  Column(
    modifier = modifier.fillMaxWidth().background(MaterialTheme.colors.primary),
  ) {
    Row(
      modifier =
        modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 16.dp)
          .testTag(
            TITLE_ROW_TEST_TAG,
          ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(4f)) {
        Icon(
          when (toolBarHomeNavigation) {
            ToolBarHomeNavigation.OPEN_DRAWER -> Icons.Filled.Menu
            ToolBarHomeNavigation.NAVIGATE_BACK -> Icons.AutoMirrored.Filled.ArrowBack
          },
          contentDescription = DRAWER_MENU,
          tint = Color.White,
          modifier =
            modifier
              .clickable { onClick(ToolbarClickEvent.Navigate) }
              .testTag(TOP_ROW_ICON_TEST_TAG),
        )
        Text(
          text = title,
          fontSize = 20.sp,
          color = Color.White,
          modifier = modifier.padding(start = 16.dp).testTag(TOP_ROW_TEXT_TEST_TAG),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp).weight(1f),
      ) {
        SetupToolbarIcons(
          menuIcons = topScreenSection?.menuIcons,
          isFilterIconEnabled = isFilterIconEnabled,
          filteredRecordsCount = filteredRecordsCount,
          navController = navController,
          modifier = modifier,
          onClick = onClick,
          decodeImage = decodeImage,
        )
      }
    }
    if (isSearchBarVisible) {
      OutlinedTextField(
        colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
        value = searchQuery.query,
        onValueChange = {
          onSearchTextChanged(
            SearchQuery(it, mode = SearchMode.KeyboardInput),
            performSearchOnValueChanged,
          )
        },
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions =
          KeyboardActions(
            onSearch = {
              onSearchTextChanged(
                SearchQuery(searchQuery.query, mode = SearchMode.KeyboardInput),
                true,
              )
            },
          ),
        maxLines = 1,
        singleLine = true,
        placeholder = {
          Text(
            color = placeholderColor?.parseColor() ?: GreyTextColor,
            text = searchPlaceholder ?: stringResource(R.string.search_hint),
            modifier = modifier.testTag(SEARCH_FIELD_TEST_TAG),
          )
        },
        modifier =
          modifier
            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(size = 10.dp))
            .background(Color.White)
            .testTag(OUTLINED_BOX_TEST_TAG),
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Search,
            SEARCH,
            modifier = modifier.testTag(LEADING_ICON_TEST_TAG),
          )
        },
        trailingIcon = {
          Box(contentAlignment = Alignment.CenterEnd) {
            when {
              !searchQuery.isBlank() -> {
                IconButton(
                  onClick = { onSearchTextChanged(SearchQuery.emptyText, true) },
                  modifier = modifier.testTag(TRAILING_ICON_BUTTON_TEST_TAG),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = CLEAR,
                    tint = Color.Gray,
                    modifier = modifier.testTag(TRAILING_ICON_TEST_TAG),
                  )
                }
              }
              showSearchByQrCode -> {
                IconButton(
                  onClick = {
                    currentContext.getActivity()?.let {
                      QrCodeScanUtils.scanQrCode(it) { code ->
                        onSearchTextChanged(
                          SearchQuery(
                            code ?: "",
                            mode = SearchMode.QrCodeScan,
                          ),
                          performSearchOnValueChanged,
                        )
                      }
                    }
                  },
                  modifier =
                    modifier.testTag(
                      TRAILING_QR_SCAN_ICON_BUTTON_TEST_TAG,
                    ),
                ) {
                  Icon(
                    painter =
                      painterResource(id = org.smartregister.fhircore.quest.R.drawable.ic_qr_code),
                    contentDescription =
                      stringResource(
                        id = org.smartregister.fhircore.quest.R.string.qr_code,
                      ),
                    modifier = modifier.testTag(TRAILING_QR_SCAN_ICON_TEST_TAG),
                  )
                }
              }
            }
          }
        },
      )
    }
  }
}

@Composable
fun SetupToolbarIcons(
  menuIcons: List<ImageProperties>?,
  isFilterIconEnabled: Boolean,
  filteredRecordsCount: Long? = null,
  navController: NavController,
  modifier: Modifier,
  onClick: (ToolbarClickEvent) -> Unit,
  decodeImage: ((String) -> Bitmap?)?,
) {
  var showOverflowMenu by remember { mutableStateOf(false) }
  if (!menuIcons.isNullOrEmpty()) {
    val iconsCount = remember { if (isFilterIconEnabled) 1 else 2 }
    if (menuIcons.size <= iconsCount) {
      RenderMenuIcon(
        menuIcons = menuIcons.subList(0, min(iconsCount, menuIcons.size)),
        isFilterIconEnabled = isFilterIconEnabled,
        filteredRecordsCount = filteredRecordsCount,
        navController = navController,
        modifier = modifier,
        onClick = onClick,
        decodeImage = decodeImage,
      )
    } else {
      Row(verticalAlignment = Alignment.CenterVertically) {
        RenderMenuIcon(
          menuIcons = menuIcons.subList(0, iconsCount),
          isFilterIconEnabled = false,
          filteredRecordsCount = null,
          navController = navController,
          modifier = modifier,
          onClick = onClick,
          decodeImage = decodeImage,
        )
        Icon(
          imageVector = Icons.Outlined.MoreVert,
          contentDescription = null,
          tint = Color.White,
          modifier =
            Modifier.padding(start = 8.dp).size(22.dp).clickable {
              showOverflowMenu = !showOverflowMenu
            },
        )
        DropdownMenu(
          expanded = showOverflowMenu,
          onDismissRequest = { showOverflowMenu = false },
        ) {
          menuIcons.subList(iconsCount, menuIcons.size).forEach {
            DropdownMenuItem(
              onClick = {
                onClick(ToolbarClickEvent.Actions(it.actions))
                showOverflowMenu = !showOverflowMenu
              },
            ) {
              Image(
                imageProperties = it,
                navController = navController,
                tint = it.tint?.parseColor() ?: DefaultColor,
                modifier =
                  modifier
                    .clickable { onClick(ToolbarClickEvent.Actions(it.actions)) }
                    .testTag(TOP_ROW_TOGGLE_ICON_TEST_tAG),
                decodeImage = decodeImage,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RenderMenuIcon(
  menuIcons: List<ImageProperties>,
  isFilterIconEnabled: Boolean,
  filteredRecordsCount: Long? = null,
  navController: NavController,
  modifier: Modifier,
  onClick: (ToolbarClickEvent) -> Unit,
  decodeImage: ((String) -> Bitmap?)?,
) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
    item {
      if (isFilterIconEnabled) {
        BadgedBox(
          modifier = Modifier.padding(end = 8.dp),
          badge = {
            if (filteredRecordsCount != null && filteredRecordsCount > -1) {
              Badge {
                Text(
                  text = if (filteredRecordsCount > 99) "99+" else filteredRecordsCount.toString(),
                  overflow = TextOverflow.Clip,
                  maxLines = 1,
                )
              }
            }
          },
        ) {
          Icon(
            imageVector = Icons.Outlined.FilterAlt,
            contentDescription = FILTER,
            tint = Color.White,
            modifier =
              modifier
                .size(22.dp)
                .clickable { onClick(ToolbarClickEvent.FilterData) }
                .testTag(TOP_ROW_FILTER_ICON_TEST_TAG),
          )
        }
      }
    }
    items(menuIcons) {
      Image(
        imageProperties = it,
        navController = navController,
        tint = Color.White,
        modifier =
          modifier
            .clickable { onClick(ToolbarClickEvent.Actions(it.actions)) }
            .testTag(TOP_ROW_TOGGLE_ICON_TEST_tAG),
        decodeImage = decodeImage,
      )
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun TopScreenSectionWithFilterItemOverNinetyNinePreview() {
  TopScreenSection(
    title = "All Clients All Clients All Clients All Clients All Clients",
    searchQuery = SearchQuery("Eddy"),
    filteredRecordsCount = 120,
    onSearchTextChanged = { _, _ -> },
    toolBarHomeNavigation = ToolBarHomeNavigation.NAVIGATE_BACK,
    isFilterIconEnabled = true,
    onClick = {},
    isSearchBarVisible = true,
    topScreenSection =
      TopScreenSectionConfig(
        searchBar = null,
        menuIcons =
          listOf(
            ImageProperties(
              imageConfig = ImageConfig(ICON_TYPE_LOCAL, "ic_toggle_map_view"),
              backgroundColor = "#FFFFFF",
              size = 10,
            ),
          ),
      ),
    navController = rememberNavController(),
    decodeImage = null,
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun TopScreenSectionWithFilterCountNinetyNinePreview() {
  TopScreenSection(
    title = "All Clients",
    searchQuery = SearchQuery("Eddy"),
    filteredRecordsCount = 99,
    onSearchTextChanged = { _, _ -> },
    toolBarHomeNavigation = ToolBarHomeNavigation.NAVIGATE_BACK,
    isFilterIconEnabled = true,
    onClick = {},
    isSearchBarVisible = true,
    navController = rememberNavController(),
    decodeImage = null,
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun TopScreenSectionNoFilterIconPreview() {
  TopScreenSection(
    title = "All Clients",
    searchQuery = SearchQuery("Eddy"),
    onSearchTextChanged = { _, _ -> },
    toolBarHomeNavigation = ToolBarHomeNavigation.NAVIGATE_BACK,
    isFilterIconEnabled = false,
    onClick = {},
    isSearchBarVisible = true,
    navController = rememberNavController(),
    topScreenSection =
      TopScreenSectionConfig(
        searchBar = null,
        title = "Service Point",
        menuIcons =
          listOf(
            ImageProperties(imageConfig = ImageConfig(reference = "ic_service_points")),
          ),
      ),
    decodeImage = null,
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun TopScreenSectionWithFilterIconAndToggleIconPreview() {
  TopScreenSection(
    title = "All Clients",
    searchQuery = SearchQuery("Eddy"),
    filteredRecordsCount = 120,
    onSearchTextChanged = { _, _ -> },
    toolBarHomeNavigation = ToolBarHomeNavigation.NAVIGATE_BACK,
    isFilterIconEnabled = true,
    onClick = {},
    isSearchBarVisible = true,
    navController = rememberNavController(),
    topScreenSection =
      TopScreenSectionConfig(
        searchBar = null,
        title = "Service Point",
        menuIcons =
          listOf(
            ImageProperties(imageConfig = ImageConfig(reference = "ic_service_points")),
          ),
      ),
    decodeImage = null,
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun TopScreenSectionWithOpenDrawerIconPreview() {
  TopScreenSection(
    title = "All Clients",
    searchQuery = SearchQuery("Eddy"),
    filteredRecordsCount = 120,
    onSearchTextChanged = { _, _ -> },
    toolBarHomeNavigation = ToolBarHomeNavigation.OPEN_DRAWER,
    isFilterIconEnabled = false,
    onClick = {},
    isSearchBarVisible = true,
    navController = rememberNavController(),
    decodeImage = null,
  )
}
