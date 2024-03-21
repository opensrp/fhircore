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

package org.smartregister.fhircore.engine.configuration

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.smartregister.fhircore.engine.configuration.event.UpdateWorkflowValueConfig

@RunWith(AndroidJUnit4::class)
class UpdateWorkflowValueConfigTest {

  @Test
  fun testWriteToParcel() {
    val config =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "$.example",
        value = Json.decodeFromString("{ \"key\": \"value\" }"),
        resourceType = ResourceType.Task,
      )
    val parcel = Parcel.obtain()
    config.writeToParcel(parcel, config.describeContents())
    parcel.setDataPosition(0)
    val createdConfig = UpdateWorkflowValueConfig.CREATOR.createFromParcel(parcel)
    assertEquals(config, createdConfig)
  }

  @Test
  fun testCreateFromParcel() {
    // Create a sample UpdateWorkflowValueConfig instance
    val config =
      UpdateWorkflowValueConfig(
        jsonPathExpression = "$.example",
        value = Json.decodeFromString("{ \"key\": \"value\" }"),
        resourceType = ResourceType.Task,
      )
    val parcel = Parcel.obtain()
    config.writeToParcel(parcel, config.describeContents())
    parcel.setDataPosition(0)
    val createdConfig = UpdateWorkflowValueConfig.CREATOR.createFromParcel(parcel)
    assertEquals("$.example", createdConfig.jsonPathExpression)
    assertEquals(Json.decodeFromString<JsonElement>("{ \"key\": \"value\" }"), createdConfig.value)
    assertEquals(ResourceType.Task, createdConfig.resourceType)
  }
}
