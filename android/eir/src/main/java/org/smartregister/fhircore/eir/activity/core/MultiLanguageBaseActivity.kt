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

package org.smartregister.fhircore.eir.activity.core

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import org.smartregister.fhircore.eir.util.SharedPreferencesHelper
import org.smartregister.fhircore.eir.util.Utils

// todo can we move to BaseActivity to
open class MultiLanguageBaseActivity : AppCompatActivity() {

  override fun attachBaseContext(base: Context) {
    val lang: String? =
      SharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.ENGLISH.toLanguageTag())
    val newConfiguration: Configuration? = Utils.setAppLocale(base, lang)
    super.attachBaseContext(base)
    applyOverrideConfiguration(newConfiguration)
  }
}
