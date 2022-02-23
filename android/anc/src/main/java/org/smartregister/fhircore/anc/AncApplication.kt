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

package org.smartregister.fhircore.anc

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.smartregister.fhircore.anc.configuration.view.ActionNavigateToReport
import org.smartregister.fhircore.anc.configuration.view.ActionSwitchFragment
import org.smartregister.fhircore.engine.configuration.view.NavigationAction
import org.smartregister.fhircore.engine.util.JsonSpecificationProvider
import timber.log.Timber

@HiltAndroidApp
class AncApplication : Application(), JsonSpecificationProvider {
  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }

  override fun getJson(): Json {

    val module = SerializersModule {
      polymorphic(NavigationAction::class) {
        subclass(ActionSwitchFragment::class)
        subclass(ActionNavigateToReport::class)
      }
    }

    return Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
      isLenient = true
      serializersModule = module
    }
  }
}
