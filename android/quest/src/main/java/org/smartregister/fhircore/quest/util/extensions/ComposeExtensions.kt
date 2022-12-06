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

package org.smartregister.fhircore.quest.util.extensions

import androidx.compose.ui.Modifier

/**
 * Apply [Modifier] conditionally. If the condition is met the [ifTrue] block will be called
 * otherwise default to [ifFalse]
 */
fun Modifier.conditional(
  condition: Boolean,
  ifTrue: Modifier.() -> Modifier,
  ifFalse: (Modifier.() -> Modifier)? = null
): Modifier {
  return when {
    condition -> then(ifTrue(Modifier))
    ifFalse != null -> then(ifFalse(Modifier))
    else -> this
  }
}
