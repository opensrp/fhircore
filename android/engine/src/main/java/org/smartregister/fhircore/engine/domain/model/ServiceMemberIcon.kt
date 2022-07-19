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

import kotlinx.serialization.json.JsonNames
import org.smartregister.fhircore.engine.R

enum class ServiceMemberIcon(val icon: Int) {
  @JsonNames("child", "Child") CHILD(R.drawable.ic_kids),
  @JsonNames("pregnant_woman", "PregnantWoman") PREGNANT_WOMAN(R.drawable.ic_pregnant)
}
