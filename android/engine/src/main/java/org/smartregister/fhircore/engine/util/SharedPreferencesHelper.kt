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

package org.smartregister.fhircore.engine.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesHelper @Inject constructor(@ApplicationContext val context: Context) {

  private var prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /** @see [SharedPreferences.getString] */
  fun read(key: String, defaultValue: String?) = prefs.getString(key, defaultValue)

  /** @see [SharedPreferences.Editor.putString] */
  fun write(key: String, value: String?) {
    with(prefs.edit()) {
      putString(key, value)
      commit()
    }
  }

  fun write(key: String, value: Long) {
    val prefsEditor: SharedPreferences.Editor = prefs.edit()
    with(prefsEditor) {
      putLong(key, value)
      commit()
    }
  }

  companion object {
    const val LANG = "shared_pref_lang"
    const val THEME = "shared_pref_theme"
    const val PREFS_NAME = "params"
  }
}
