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

package org.smartregister.fhircore.engine.cql

import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import timber.log.Timber

class R4FhirModelResolverExt : R4FhirModelResolver() {
  override fun resolveType(typeName: String): Class<*> {
    // TODO https://github.com/DBCG/cql_engine/issues/537
    // FHIR has a bug that does not allow processing inner static classes
    // i.e. Dosage.DosageDoseAndRateComponent hence creating those on own.
    // This is would be represented by Dosage.DoseAndRate in cql and should resolve to above
    // mentioned
    if (typeName.matches(Regex("\\w+\\.\\w+"))) {
      val path = typeName.split(".")
      val cls =
        this.packageNames.firstNotNullOfOrNull {
          kotlin
            .runCatching {
              // builds package.Dosage$DosageDoseAndRateComponent from Dosage.DoseAndRate
              Class.forName("$it.${path[0]}$${path[0]}${path[1]}Component")
            }
            .onFailure { Timber.e(it.stackTraceToString()) }
            .getOrNull()
        }
      if (cls != null) return cls
    }
    return super.resolveType(typeName)
  }
}
