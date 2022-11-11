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
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.MenuIconConfig
import org.smartregister.fhircore.engine.util.extension.retrieveResourceId
import org.smartregister.fhircore.quest.ui.main.components.SIDE_MENU_ICON

const val SIDE_MENU_ITEM_LOCAL_ICON_TEST_TAG = "sideMenuItemLocalIconTestTag"
const val SIDE_MENU_ITEM_REMOTE_ICON_TEST_TAG = "sideMenuItemBinaryIconTestTag"

@Composable
fun MenuIcon(
  modifier: Modifier = Modifier,
  menuIconConfig: MenuIconConfig?,
  color: Color,
  paddingEnd: Int = 8
) {
  if (menuIconConfig != null) {
    when (menuIconConfig.type) {
      ICON_TYPE_LOCAL -> {
        LocalContext.current.retrieveResourceId(menuIconConfig.reference)?.let { drawableId ->
          Icon(
            modifier =
              modifier
                .testTag(SIDE_MENU_ITEM_LOCAL_ICON_TEST_TAG)
                .padding(end = paddingEnd.dp)
                .size(24.dp),
            painter = painterResource(id = drawableId),
            contentDescription = SIDE_MENU_ICON,
            tint = color
          )
        }
      }
      ICON_TYPE_REMOTE ->
        if (menuIconConfig.decodedBitmap != null) {
          Image(
            modifier =
              modifier
                .testTag(SIDE_MENU_ITEM_REMOTE_ICON_TEST_TAG)
                .padding(end = paddingEnd.dp)
                .size(24.dp),
            bitmap = menuIconConfig.decodedBitmap!!.asImageBitmap(),
            contentDescription = null
          )
        }
    }
  }
}
