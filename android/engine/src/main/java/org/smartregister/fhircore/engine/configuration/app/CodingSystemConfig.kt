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

package org.smartregister.fhircore.engine.configuration.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.Code

/**
 * This configuration provides the various [Code] systems used in the application and how they are
 * to be used. For instance the code with [CodingSystemUsage.LOCATION_LINKAGE] type of usage will be
 * used to link any FHIR resource to a Location via a List resource. The List resource will include
 * the Location as it's subject and the linked resources in it's entry property. The List resource
 * will have a special code used to differentiate it from other List resources.
 */
@Serializable
@Parcelize
data class CodingSystemConfig(val coding: Code, val usage: CodingSystemUsage) :
  Parcelable, java.io.Serializable

enum class CodingSystemUsage {
  LOCATION_LINKAGE,
}
