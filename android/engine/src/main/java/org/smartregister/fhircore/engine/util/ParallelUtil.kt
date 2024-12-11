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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Launch a new coroutine for each map iteration using async. From
 * https://jivimberg.io/blog/2018/05/04/parallel-map-in-kotlin/.
 *
 * @param A the type of elements in the iterable
 * @param B the type of elements returned by the function
 * @param dispatcher dispatcher that creates the async coroutine
 * @param f the function to apply to the elements
 * @return the resulting list after apply *f* to the elements of the iterable
 */
suspend fun <A, B> Iterable<A>.pmap(dispatcher: CoroutineDispatcher, f: suspend (A) -> B): List<B> =
  coroutineScope {
    map { async(dispatcher) { f(it) } }.awaitAll()
  }

/**
 * Launch a new coroutine for each loop iteration using launch and the specified Dispatcher for
 * computationaly intensive tasks.
 *
 * @param T the type of elements in the iterable
 * @param dispatcher dispatcher that creates the async coroutine
 * @param action the function to apply to the elements
 */
suspend fun <T> Iterable<T>.forEachAsync(
  dispatcher: CoroutineDispatcher,
  action: suspend (T) -> Unit,
): Unit = coroutineScope { forEach { launch(dispatcher) { action(it) } } }
