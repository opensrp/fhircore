/*
 * Copyright 2020 Google LLC
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

package org.smartregister.fhircore.data

import org.smartregister.fhircore.api.HapiFhirService
import com.google.android.fhir.sync.FhirDataSource
import org.hl7.fhir.r4.model.Bundle

/**
 * Implementation of the [FhirDataSource] that communicates with hapi fhir.
 */
class HapiFhirResourceDataSource(
    private val service: HapiFhirService
) : FhirDataSource {

    override suspend fun loadData(path: String): Bundle {
        return service.getResource(path)
    }
}
