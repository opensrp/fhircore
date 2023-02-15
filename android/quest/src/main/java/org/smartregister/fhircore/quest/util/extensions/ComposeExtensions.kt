/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/** This function returns whether the list is currently scrolling up */
@Composable
fun LazyListState.isScrollingUp(): Boolean {
  var previousIndex: Int by remember(this) { mutableStateOf(firstVisibleItemIndex) }
  var previousScrollOffset: Int by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
  return remember(this) {
      derivedStateOf {
        if (previousIndex != firstVisibleItemIndex) {
            previousIndex > firstVisibleItemIndex
          } else {
            previousScrollOffset >= firstVisibleItemScrollOffset
          }
          .also {
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
          }
      }
    }
    .value
}

/** This function returns whether the list is currently scrolling down */
@Composable
fun LazyListState.isScrollingDown(): Boolean {
  var previousIndex: Int by remember(this) { mutableStateOf(firstVisibleItemIndex) }
  var previousScrollOffset: Int by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
  return remember(this) {
      derivedStateOf {
        if (previousIndex != firstVisibleItemIndex) {
            previousIndex < firstVisibleItemIndex
          } else {
            previousScrollOffset <= firstVisibleItemScrollOffset
          }
          .also {
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
          }
      }
    }
    .value
}
