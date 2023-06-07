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

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Event designed to be delivered only once to a concrete entity, but it can also be delivered to
 * multiple different entities.
 *
 * Keeps track of who has already handled its content.
 */
class OneTimeEvent<out T>(private val content: T) {

  private val handlers = CopyOnWriteArraySet<String>()

  /**
   * @param asker Used to identify, whether this "asker" has already handled this Event.
   *
   * @return Event content or null if it has been already handled by asker
   */
  fun getIfNotHandled(asker: String): T? = if (handlers.add(asker)) content else null
}
