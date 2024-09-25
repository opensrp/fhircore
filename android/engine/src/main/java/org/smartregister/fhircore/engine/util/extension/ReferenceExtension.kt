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

package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType

fun Reference.extractId(): String =
  if (this.reference.isNullOrEmpty()) "" else this.reference.extractLogicalIdUuid()

fun Reference.extractType(): ResourceType? =
  if (this.reference.isNullOrEmpty()) {
    null
  } else {
    this.reference.substringBefore("/" + this.extractId()).substringAfterLast("/").let {
      ResourceType.fromCode(it)
    }
  }

fun String.asReference(resourceType: ResourceType): Reference {
  val resourceId = this
  return Reference().apply { reference = "${resourceType.name}/$resourceId" }
}
