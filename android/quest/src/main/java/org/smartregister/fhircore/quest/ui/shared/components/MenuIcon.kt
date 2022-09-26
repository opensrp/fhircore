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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.configuration.navigation.MenuIconConfig

const val SIDE_MENU_ITEM_REMOTE_ICON_TEST_TAG = "sideMenuItemBinaryIconTestTag"

@Composable
fun MenuIcon(modifier: Modifier = Modifier, menuIconConfig: MenuIconConfig) {
  if (menuIconConfig.decodedBitmap != null) {
    Image(
      modifier =
        modifier.testTag(SIDE_MENU_ITEM_REMOTE_ICON_TEST_TAG).padding(end = 10.dp).size(24.dp),
      bitmap = menuIconConfig.decodedBitmap!!.asImageBitmap(),
      contentDescription = null
    )
  }
}
