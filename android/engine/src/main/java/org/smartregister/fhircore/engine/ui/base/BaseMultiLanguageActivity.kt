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

package org.smartregister.fhircore.engine.ui.base

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.lang.UnsupportedOperationException
import java.util.Locale
import javax.inject.Inject
import org.smartregister.fhircore.engine.util.ContextUtil
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.setAppLocale

abstract class BaseMultiLanguageActivity : AppCompatActivity() {

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  override fun onCreate(savedInstanceState: Bundle?) {
    inject()
    super.onCreate(savedInstanceState)
    ContextUtil.context = this
    val themePref =
      sharedPreferencesHelper.read(key = SharedPreferenceKey.THEME.name, defaultValue = "")!!

    if (themePref.isNotEmpty()) {
      val resourceId = this.resources.getIdentifier(themePref, "style", packageName)
      if (resourceId != 0) theme.applyStyle(resourceId, true)
    }
  }

  override fun attachBaseContext(baseContext: Context) {
    val lang =
      baseContext
        .getSharedPreferences(SharedPreferencesHelper.PREFS_NAME, Context.MODE_PRIVATE)
        .getString(SharedPreferenceKey.LANG.name, Locale.ENGLISH.toLanguageTag())
        ?: Locale.ENGLISH.toLanguageTag()
    baseContext.setAppLocale(lang).run {
      super.attachBaseContext(baseContext)
      applyOverrideConfiguration(this)
    }
  }

  /**
   * This method is required by Hilt to inject dependencies for the base class. Hilt injection
   * occurs within super.onCreate() instead of before super.onCreate()
   */
  protected open fun inject() {
    throw UnsupportedOperationException(
      "Annotate $this with @AndroidEntryPoint annotation. The inject method should be overridden by the Hilt generated class."
    )
  }
}
