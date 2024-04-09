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

package org.smartregister.fhircore.engine.configuration.event

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceFilterExpression

@Serializable
@Parcelize
data class EventWorkflow(
  val eventType: EventType = EventType.RESOURCE_CLOSURE,
  val triggerConditions: List<EventTriggerCondition> = emptyList(),
  val eventResources: List<ResourceConfig> = emptyList(),
  val updateValues: List<UpdateWorkflowValueConfig> = emptyList(),
  val resourceFilterExpressions: List<ResourceFilterExpression>? = null,
) : java.io.Serializable, Parcelable

@Serializable
data class UpdateWorkflowValueConfig(
  val jsonPathExpression: String,
  val value: JsonElement,
  val resourceType: ResourceType = ResourceType.Task,
) : java.io.Serializable, Parcelable {
  constructor(
    parcel: Parcel,
  ) : this(
    parcel.readString() ?: "",
    Json.decodeFromString(parcel.readString() ?: ""),
    ResourceType.fromCode(parcel.readString()),
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(jsonPathExpression)
    parcel.writeString(value.toString())
    parcel.writeString(resourceType.name)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<UpdateWorkflowValueConfig> {
    override fun createFromParcel(parcel: Parcel): UpdateWorkflowValueConfig {
      return UpdateWorkflowValueConfig(parcel)
    }

    override fun newArray(size: Int): Array<UpdateWorkflowValueConfig?> {
      return arrayOfNulls(size)
    }
  }
}
