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

typealias StatusPriority = Int

enum class HealthStatus(var display: String = "") {
  CLIENT_ALREADY_ON_ART {
    override fun priority(): StatusPriority = 1
  },
  NEWLY_DIAGNOSED_CLIENT {
    override fun priority(): StatusPriority = 2
  },
  EXPOSED_INFANT {
    override fun priority(): StatusPriority = 3
  },
  CHILD_CONTACT {
    override fun priority(): StatusPriority = 3
  },
  SEXUAL_CONTACT {
    override fun priority(): StatusPriority = 3
  },
  COMMUNITY_POSITIVE {
    override fun priority(): StatusPriority = 3
  },
  NOT_ON_ART(display = Constant.NOT_ON_ART) {
    override fun priority(): StatusPriority = Int.MAX_VALUE - 1
  },
  DEFAULT {
    override fun priority(): StatusPriority = Int.MAX_VALUE
  };

  abstract fun priority(): StatusPriority

  object Constant {
    const val NOT_ON_ART = "Not on ART"
  }
}
