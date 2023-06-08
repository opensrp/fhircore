package org.smartregister.fhircore.engine.configuration.view

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


import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
data class ImageProperties(
    override val viewType: ViewType = ViewType.IMAGE,
    override val weight: Float = 0f,
    override val backgroundColor: String? = null,
    override val padding: Int = 0,
    override val borderRadius: Int = 2,
    override val alignment: ViewAlignment = ViewAlignment.NONE,
    override val fillMaxWidth: Boolean = false,
    override val fillMaxHeight: Boolean = false,
    override val clickable: String = "false",
    override val visible: String = "true",
    var imageConfig: ImageConfig? = null,
) : ViewProperties() {
    override fun interpolate(computedValuesMap: Map<String, Any>): ViewProperties {
        return this.copy(
            backgroundColor = backgroundColor?.interpolate(computedValuesMap),
        )
    }
}

