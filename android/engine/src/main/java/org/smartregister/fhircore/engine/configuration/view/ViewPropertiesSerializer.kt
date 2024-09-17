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

package org.smartregister.fhircore.engine.configuration.view

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.smartregister.fhircore.engine.domain.model.ViewType

private const val VIEW_TYPE = "viewType"

object ViewPropertiesSerializer :
  JsonContentPolymorphicSerializer<ViewProperties>(ViewProperties::class) {
  override fun selectDeserializer(
    element: JsonElement,
  ): DeserializationStrategy<ViewProperties> {
    val jsonObject = element.jsonObject
    val viewType = jsonObject[VIEW_TYPE]?.jsonPrimitive?.content
    require(
      viewType != null && ViewType.entries.toTypedArray().contains(ViewType.valueOf(viewType)),
    ) {
      """Ensure that supported `viewType` property is included in your register view properties configuration.
         Supported types: ${ViewType.entries.toTypedArray()}
         Parsed JSON: $jsonObject
            """
        .trimMargin()
    }
    return when (ViewType.valueOf(viewType)) {
      ViewType.ROW -> RowProperties.serializer()
      ViewType.COLUMN -> ColumnProperties.serializer()
      ViewType.COMPOUND_TEXT -> CompoundTextProperties.serializer()
      ViewType.SERVICE_CARD -> ServiceCardProperties.serializer()
      ViewType.CARD -> CardViewProperties.serializer()
      ViewType.PERSONAL_DATA -> PersonalDataProperties.serializer()
      ViewType.BUTTON -> ButtonProperties.serializer()
      ViewType.SPACER -> SpacerProperties.serializer()
      ViewType.LIST -> ListProperties.serializer()
      ViewType.IMAGE -> ImageProperties.serializer()
      ViewType.BORDER -> DividerProperties.serializer()
      ViewType.STACK -> StackViewProperties.serializer()
    }
  }
}
