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

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize
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
@Parcelize
data class ButtonProperties(
  override val viewType: ViewType = ViewType.BUTTON,
  override val weight: Float = 0f,
  override val backgroundColor: String? = null,
  override val padding: Int = 0,
  override val borderRadius: Int = 14,
  override val alignment: ViewAlignment = ViewAlignment.NONE,
  override val fillMaxWidth: Boolean = true,
  override val fillMaxHeight: Boolean = false,
  override val clickable: String = "false",
  override val visible: String = "true",
  override val opacity: Float? = null,
  val contentColor: String? = null,
  val enabled: String = "true",
  val text: String? = null,
  val status: String,
  val fontSize: Float = 14.0f,
  val actions: List<ActionConfig> = emptyList(),
  val buttonType: ButtonType = ButtonType.MEDIUM,
  val startIcon: ImageConfig? = null,
  val letterSpacing: Int = 0,
  val backgroundOpacity: Float = 0f,
  val colorOpacity: Float = 0f,
  val statusIconSize: Int = 16,
) : ViewProperties(), Parcelable {
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
      ServiceStatus.EXPIRED -> DefaultColor
      ServiceStatus.FAILED -> DangerColor
    }
  }

  override fun interpolate(computedValuesMap: Map<String, Any>): ButtonProperties {
    return this.copy(
      backgroundColor = backgroundColor?.interpolate(computedValuesMap),
      visible = visible.interpolate(computedValuesMap),
      status = interpolateStatus(computedValuesMap).name,
      text = text?.interpolate(computedValuesMap),
      enabled = enabled.interpolate(computedValuesMap),
      clickable = clickable.interpolate(computedValuesMap),
      contentColor = contentColor?.interpolate(computedValuesMap),
      startIcon = startIcon?.interpolate(computedValuesMap),
    )
  }

  private fun interpolateStatus(computedValuesMap: Map<String, Any>): ServiceStatus {
    val interpolated = this.status.interpolate(computedValuesMap)
    return if (ServiceStatus.values().map { it.name }.contains(interpolated)) {
      ServiceStatus.valueOf(interpolated)
    } else {
      ServiceStatus.UPCOMING
    }
  }
}

enum class ButtonType {
  TINY,
  MEDIUM,
  BIG,
}
