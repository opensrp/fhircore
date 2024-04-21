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

package org.smartregister.fhircore.quest.util.extensions

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Binary
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.base64toBitmap
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.tryDecodeJson

fun loadImagesRecursively(
  views: List<ViewProperties>,
  registerRepository: RegisterRepository,
  computedValuesMap: Map<String, Any>,
) {
  fun loadIcons(view: ViewProperties) {
    when (view.viewType) {
      ViewType.IMAGE -> {
        val imageProps = view as ImageProperties
        if (
          !imageProps.imageConfig?.reference.isNullOrEmpty() &&
            imageProps.imageConfig?.type == ICON_TYPE_REMOTE
        ) {
          val resourceId =
            imageProps.imageConfig!!
              .reference!!
              .interpolate(computedValuesMap)
              .extractLogicalIdUuid()
          runBlocking {
            withTimeout(2000) {
              registerRepository.loadResource<Binary>(resourceId)?.let { binary ->
                imageProps.imageConfig?.decodedBitmap =
                  binary.data
                    .decodeToString()
                    .tryDecodeJson<ImageConfiguration>()
                    ?.data
                    ?.base64toBitmap()
              }
            }
          }
        }
      }
      ViewType.ROW -> {
        val container = view as RowProperties
        container.children.forEach { childView -> loadIcons(childView) }
      }
      ViewType.COLUMN -> {
        val container = view as ColumnProperties
        container.children.forEach { childView -> loadIcons(childView) }
      }
      ViewType.CARD -> {
        val card = view as CardViewProperties
        card.content.forEach { contentView -> loadIcons(contentView) }
      }
      ViewType.LIST -> {
        val list = view as ListProperties
        list.registerCard.views.forEach { contentView -> loadIcons(contentView) }
      }
      else -> {
        // Handle any other view types if needed
      }
    }
  }
  views.forEach { view -> loadIcons(view) }
}

@Serializable
data class ImageConfiguration(
  val id: String,
  override val resourceType: String,
  val contentType: String,
  val data: String,
) : Configuration()
