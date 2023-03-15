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

package org.smartregister.fhircore.engine.util.extension

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@Config(sdk = [Build.VERSION_CODES.O])
class AndroidExtensionApi26Test : RobolectricTest() {

  @Test
  fun `Context#setLocale() should change the default locale`() {
    assertEquals("en-US", Locale.getDefault().toLanguageTag())

    ApplicationProvider.getApplicationContext<Application>().setAppLocale("sw")

    assertEquals("sw", Locale.getDefault().toLanguageTag())
  }
}
