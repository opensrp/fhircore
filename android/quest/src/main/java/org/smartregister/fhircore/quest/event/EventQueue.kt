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

package org.smartregister.fhircore.quest.event

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transform

/**
 * (See [this](https://github.com/Kotlin/kotlinx.coroutines/issues/3002#issuecomment-1239063714)).
 */
@Singleton
class EventQueue<T> @Inject constructor() : SharedEvent<T> {

  private val innerQueue = MutableSharedFlow<OneTimeEvent<T>>()

  suspend fun push(event: T) {
    innerQueue.emit(OneTimeEvent(event))
  }

  override fun getFor(consumerId: String): Flow<T> = innerQueue.filterNotHandledBy(consumerId)

  fun <T> Flow<OneTimeEvent<T>>.filterNotHandledBy(consumerId: String): Flow<T> =
      transform { event ->
    event.getIfNotHandled(consumerId)?.let { emit(it) }
  }
}
