/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.nfc

import com.famoco.desfireservicelib.DESFireServiceAccess
import com.github.os72.protobuf.dynamic.DynamicSchema

/**
 * Since activities cannot share the same viewModel, this object is needed to share information
 * between each viewModel of each activities.
 */
object NFCDataProvider {
  // Values that will be shared between each viewModel in order to keep consistency
  var protoFile: DynamicSchema = DynamicSchema.newBuilder().build()
  var fileNumber: String = DESFireServiceAccess.File.BINARY_FILE
}
