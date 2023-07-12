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

package org.smartregister.fhircore.engine.util

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.google.gson.Gson
import com.google.gson.JsonIOException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.encodeJson
import timber.log.Timber

@Singleton
class SharedPreferencesHelper
@Inject
constructor(@ApplicationContext val context: Context, val gson: Gson) {

  val prefs: SharedPreferences by lazy {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  /** @see [SharedPreferences.getString] */
  fun read(key: String, defaultValue: String?) = prefs.getString(key, defaultValue)

  /** @see [SharedPreferences.Editor.putString] */
  fun write(key: String, value: String?) {
    with(prefs.edit()) {
      putString(key, value)
      commit()
    }
  }

  /** @see [SharedPreferences.getLong] */
  fun read(key: String, defaultValue: Long) = prefs.getLong(key, defaultValue)

  /** @see [SharedPreferences.Editor.putLong] */
  fun write(key: String, value: Long) {
    val prefsEditor: SharedPreferences.Editor = prefs.edit()
    with(prefsEditor) {
      putLong(key, value)
      commit()
    }
  }

  /** @see [SharedPreferences.getBoolean] */
  fun read(key: String, defaultValue: Boolean) = prefs.getBoolean(key, defaultValue)

  /** @see [SharedPreferences.Editor.putBoolean] */
  fun write(key: String, value: Boolean) {
    with(prefs.edit()) {
      putBoolean(key, value)
      commit()
    }
  }

  /** Read any JSON object with type T */
  inline fun <reified T> read(key: String, decodeWithGson: Boolean = true): T? =
    if (decodeWithGson) {
      try {
        gson.fromJson(this.read(key, null), T::class.java)
      } catch (jsonIoException: JsonIOException) {
        Timber.e(jsonIoException)
        null
      }
    } else {
      try {
        this.read(key, null)?.decodeJson<T>()
      } catch (serializationException: SerializationException) {
        Timber.e(serializationException)
        null
      }
    }

  /** Write any object by saving it as JSON */
  inline fun <reified T> write(key: String, value: T?, encodeWithGson: Boolean = true) {
    with(prefs.edit()) {
      putString(key, if (encodeWithGson) gson.toJson(value) else value.encodeJson())
      commit()
    }
  }

  fun remove(key: String) {
    prefs.edit().remove(key).apply()
  }

  /** This method resets/clears all existing values in the shared preferences asynchronously */
  fun resetSharedPrefs() {
    prefs.edit()?.clear()?.apply()
  }

  fun registerSharedPreferencesListener(
    onSharedPreferenceChangeListener: OnSharedPreferenceChangeListener,
  ) {
    prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
  }

  fun unregisterSharedPreferencesListener(
    onSharedPreferenceChangeListener: OnSharedPreferenceChangeListener,
  ) {
    prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
  }

  fun retrieveApplicationId() = read(SharedPreferenceKey.APP_ID.name, null)

  companion object {
    const val PREFS_NAME = "params"
    const val PREFS_SYNC_PROGRESS_TOTAL = "sync_progress_total"
  }
}
