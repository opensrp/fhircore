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

package org.smartregister.fhircore.quest.data

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.XFhirQueryResolver
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Resource

@Singleton
class QuestXFhirQueryResolver @Inject constructor(val fhirEngine: FhirEngine) : XFhirQueryResolver {
  override suspend fun resolve(xFhirQuery: String): List<Resource> {
    return fhirEngine.search(xFhirQuery)
  }
}
