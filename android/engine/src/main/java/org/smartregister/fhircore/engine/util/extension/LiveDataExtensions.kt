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

package org.smartregister.fhircore.engine.util.extension

import androidx.lifecycle.MutableLiveData

/*
operator fun <T> MutableLiveData<LinkedList<T>>.plusAssign(item: T) {
    val value = this.value ?: LinkedList<T>()
    value.add(item)
    this.value = value
}*/

// for immutable list
operator fun <T> MutableLiveData<List<T>>.plusAssign(item: T) {
  val value = this.value ?: emptyList()
  this.value = value + listOf(item)
}
