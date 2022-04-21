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

package org.smartregister.fhircore.engine.domain.model

import java.util.Date
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Type
import org.smartregister.fhircore.engine.appfeature.model.HealthModule

sealed class RegisterData(open val id: String, open val name: String) {

  data class RawRegisterData(
    override val id: String,
    override val name: String,
    val healthModule: HealthModule,
    val main: Pair<String, Resource>,
    internal val _details: MutableMap<String, List<Resource>> = mutableMapOf(),
    val details: Map<String, List<Resource>> = _details
  ) : RegisterData(id = id, name = name)

  data class DefaultRegisterData(
    override val id: String,
    override val name: String,
    val gender: String,
    val birthDate: Date
  ) : RegisterData(id = id, name = name)

  data class FamilyRegisterData(
    override val id: String,
    override val name: String,
    val identifier: String? = null,
    val address: String? = null,
    var members: MutableList<FamilyMemberRegisterData> = mutableListOf(),
    val servicesDue: Int? = null,
    val servicesOverdue: Int? = null
  ) : RegisterData(id = id, name = name)

  data class FamilyMemberRegisterData(
    override val id: String,
    override val name: String,
    val identifier: String? = null,
    val birthDate: Date?,
    val gender: String?,
    val isHead: Boolean?,
    val pregnant: Boolean? = null,
    val deceased: Type? = null, // deceased can be boolean or datetime as well
    val isDead: Boolean? = deceased != null,
    val deathDate: Date? =
      deceased?.let { if (it is BooleanType) null else it.dateTimeValue().value },
    val servicesDue: Int? = null,
    val servicesOverdue: Int? = null
  ) : RegisterData(id = id, name = name)

  data class AncRegisterData(
    override val id: String,
    override val name: String,
    val identifier: String? = null,
    val age: String,
    val address: String,
    val visitStatus: VisitStatus,
    val servicesDue: Int? = null,
    val servicesOverdue: Int? = null
  ) : RegisterData(id = id, name = name)

  fun addCollectionData(fieldName: String, data: Collection<Any>) {
    this::class.memberProperties.find { it.name == fieldName }?.let {
      (it as KMutableProperty<MutableCollection<Any>>).getter.call(this).addAll(data)
    }
  }
}
