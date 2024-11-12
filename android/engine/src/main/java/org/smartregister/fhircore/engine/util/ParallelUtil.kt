/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Launch a new coroutine for each map iteration using async. From
 * https://jivimberg.io/blog/2018/05/04/parallel-map-in-kotlin/.
 *
 * @param A the type of elements in the iterable
 * @param B the type of elements returned by the function
 * @param f the function to apply to the elements
 * @return the resulting list after apply *f* to the elements of the iterable
 */
suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): Iterable<B> = coroutineScope {
  map { async { f(it) } }.awaitAll()
}

/**
 * Launch a new coroutine for each loop iteration using async.
 *
 * @param T the type of elements in the iterable
 */
suspend fun <T> Iterable<T>.forEachAsync(action: suspend (T) -> Unit): Unit = coroutineScope {
  forEach { async { action(it) } }
}
