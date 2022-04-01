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

package org.smartregister.fhircore.anc.ui.details.child.model

data class ChildProfileViewData(
  val name: String = "",
  val status: String = "",
  val id: String = "",
  val identifier: String = "",
  val sex: String = "",
  val age: String = "",
  val dob: String = "",
  val tasks: List<ChildProfileRowItem> = emptyList(),
)
