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
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor
import org.smartregister.fhircore.engine.util.extension.retrieveResourceId
import org.smartregister.fhircore.quest.ui.main.components.SIDE_MENU_ICON
import org.smartregister.fhircore.quest.util.extensions.conditional
import org.smartregister.p2p.search.ui.theme.SuccessColor

const val SIDE_MENU_ITEM_LOCAL_ICON_TEST_TAG = "sideMenuItemLocalIconTestTag"
const val SIDE_MENU_ITEM_REMOTE_ICON_TEST_TAG = "sideMenuItemBinaryIconTestTag"

@Composable
fun Image(
  modifier: Modifier = Modifier,
  color: Color?,
  paddingEnd: Int = 8,
  imageProperties: ImageProperties? = null,
  imageConfig: ImageConfig?
) {

  val imageConfigFinal: ImageConfig? = imageConfig ?: imageProperties?.imageConfig
  val colorFinal: Color = (getTintColor(imageProperties?.tint) ?: color) as Color
  val size = imageProperties?.size

  if (imageConfigFinal != null) {
    when (imageConfigFinal.type) {
      ICON_TYPE_LOCAL -> {
        LocalContext.current.retrieveResourceId(imageConfigFinal.reference)?.let { drawableId ->
          Icon(
            modifier =
              modifier
                .testTag(SIDE_MENU_ITEM_LOCAL_ICON_TEST_TAG)
                .conditional(size != null, { modifier.size(size!!.dp) }, { modifier.size(24.dp) })
                .padding(end = paddingEnd.dp),
            painter = painterResource(id = drawableId),
            contentDescription = SIDE_MENU_ICON,
            tint = colorFinal
          )
        }
      }
      ICON_TYPE_REMOTE ->
        if (imageConfigFinal.decodedBitmap != null) {
          Image(
            modifier =
              modifier
                .testTag(SIDE_MENU_ITEM_REMOTE_ICON_TEST_TAG)
                .conditional(size != null, { modifier.size(size!!.dp) }, { modifier.size(24.dp) })
                .padding(end = paddingEnd.dp),
            bitmap = imageConfigFinal.decodedBitmap!!.asImageBitmap(),
            contentDescription = null
          )
        }
    }
  }
}

@Composable
private fun getTintColor(colorString: String?): Color? {
  var color: Color? = null
  when (colorString) {
    "COMPLETED" -> color = SuccessColor
    "OVERDUE" -> color = DangerColor
    "DUE" -> color = WarningColor
  }
  return color
}
