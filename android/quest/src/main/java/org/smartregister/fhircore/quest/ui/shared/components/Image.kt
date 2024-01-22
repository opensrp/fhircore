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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.configuration.view.ImageShape
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.engine.util.extension.retrieveResourceId
import org.smartregister.fhircore.quest.ui.main.components.SIDE_MENU_ICON
import org.smartregister.fhircore.quest.util.extensions.conditional

const val SIDE_MENU_ITEM_LOCAL_ICON_TEST_TAG = "sideMenuItemLocalIconTestTag"
const val SIDE_MENU_ITEM_REMOTE_ICON_TEST_TAG = "sideMenuItemBinaryIconTestTag"

@Composable
fun Image(
  modifier: Modifier = Modifier,
  paddingEnd: Int? = null,
  tint: Color? = null,
  imageProperties: ImageProperties = ImageProperties(viewType = ViewType.IMAGE, size = 24),
) {
  val imageConfig = imageProperties.imageConfig
  if (imageConfig != null) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        modifier
          .conditional(
            imageProperties.shape != null,
            { clip(imageProperties.shape!!.composeShape) },
            { clip(RoundedCornerShape(imageProperties.borderRadius.dp)) },
          )
          .conditional(
            imageProperties.size != null,
            { size(imageProperties.size!!.dp) },
            { size(24.dp) },
          )
          .conditional(
            !imageProperties.backgroundColor.isNullOrEmpty(),
            { background(imageProperties.backgroundColor.parseColor()) },
          )
          .conditional(imageProperties.padding >= 0, { padding(imageProperties.padding.dp) }),
    ) {
      when (imageConfig.type) {
        ICON_TYPE_LOCAL ->
          LocalContext.current.retrieveResourceId(imageConfig.reference)?.let { drawableId ->
            Icon(
              modifier =
                Modifier.testTag(SIDE_MENU_ITEM_LOCAL_ICON_TEST_TAG)
                  .conditional(paddingEnd != null, { padding(end = paddingEnd!!.dp) })
                  .align(Alignment.Center)
                  .fillMaxSize(0.9f),
              painter = painterResource(id = drawableId),
              contentDescription = SIDE_MENU_ICON,
              tint = tint ?: imageProperties.tint.parseColor(),
            )
          }
        ICON_TYPE_REMOTE ->
          if (imageConfig.decodedBitmap != null) {
            Image(
              modifier =
                Modifier.testTag(SIDE_MENU_ITEM_REMOTE_ICON_TEST_TAG)
                  .conditional(paddingEnd != null, { padding(end = paddingEnd!!.dp) })
                  .align(Alignment.Center)
                  .fillMaxSize(0.9f),
              bitmap = imageConfig.decodedBitmap!!.asImageBitmap(),
              contentDescription = null,
              contentScale = ContentScale.Crop,
            )
          }
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun ImagePreview() {
  Image(
    modifier = Modifier,
    imageProperties =
      ImageProperties(
        imageConfig = ImageConfig(ICON_TYPE_LOCAL, "ic_walk"),
        backgroundColor = "dangerColor",
        size = 80,
        shape = ImageShape.CIRCLE,
      ),
    tint = DangerColor.copy(0.1f),
  )
}
