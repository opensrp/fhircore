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

package org.smartregister.fhircore.engine.configuration.view

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
data class ButtonProperties(
  override val viewType: ViewType = ViewType.BUTTON,
  override val weight: Float = 0f,
  override val backgroundColor: String? = null,
  override val padding: Int = 0,
  override val borderRadius: Int = 2,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = true,
  override val fillMaxHeight: Boolean = false,
  override val clickable: String = "false",
  override val visible: String = "true",
  val enabled: String = "true",
  val text: String? = null,
  val status: String,
  val smallSized: Boolean = false,
  val fontSize: Float = 14.0f,
  val actions: List<ActionConfig> = emptyList(),
  val buttonType: ButtonType = ButtonType.MEDIUM,
) : ViewProperties() {
  /**
   * This function determines the status color to display depending on the value of the service
   * status
   *
   * @property computedValuesMap Contains data extracted from the resources to be used on the UI
   */
  fun statusColor(computedValuesMap: Map<String, Any>): Color {
    return when (interpolateStatus(computedValuesMap)) {
      ServiceStatus.DUE -> InfoColor
      ServiceStatus.OVERDUE -> DangerColor
      ServiceStatus.UPCOMING -> DefaultColor
      ServiceStatus.COMPLETED -> DefaultColor
      ServiceStatus.IN_PROGRESS -> WarningColor
    }
  }
  override fun interpolate(computedValuesMap: Map<String, Any>): ButtonProperties {
    return this.copy(
      backgroundColor = backgroundColor?.interpolate(computedValuesMap),
      visible = visible.interpolate(computedValuesMap),
      status = interpolateStatus(computedValuesMap).name,
      text = text?.interpolate(computedValuesMap),
      enabled = enabled.interpolate(computedValuesMap),
      clickable = clickable.interpolate(computedValuesMap)
    )
  }

  private fun interpolateStatus(computedValuesMap: Map<String, Any>): ServiceStatus {
    val interpolated = this.status.interpolate(computedValuesMap)
    return if (ServiceStatus.values().map { it.name }.contains(interpolated))
      ServiceStatus.valueOf(interpolated)
    else ServiceStatus.UPCOMING
  }
}

enum class ButtonType {
  TINY,
  MEDIUM,
  BIG
}
