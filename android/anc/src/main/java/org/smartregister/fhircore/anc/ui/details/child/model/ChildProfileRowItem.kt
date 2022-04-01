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

import androidx.compose.ui.graphics.Color

data class ChildProfileRowItem(
  val title: String,
  val titleIcon: Int? = null,
  val subtitle: String,
  val subtitleStatus: String? = null,
  val subtitleStatusColor: Color? = null,
  val actionButtonColor: Color? = null,
  val actionButtonText: String? = null,
  val showDot: Boolean = false
)
