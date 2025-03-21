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

package org.smartregister.fhircore.engine.util.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncState
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@ExcludeFromJacocoGeneratedReport
@AndroidEntryPoint
class HiltActivityForTest : AppCompatActivity(), OnSyncListener {
  override fun onCreate(savedInstanceState: Bundle?) {
    if (intent.hasExtra(THEME_EXTRAS_BUNDLE_KEY)) {
      setTheme(intent.getIntExtra(THEME_EXTRAS_BUNDLE_KEY, R.style.AppTheme))
    }

    super.onCreate(savedInstanceState)
  }

  override fun onSync(syncState: SyncState) {
    // DO nothing. This activity implements OnSyncListener for testing purposes
  }

  companion object {
    const val THEME_EXTRAS_BUNDLE_KEY =
      "org.smartregister.fhircore.engine.util.test.HiltActivityForTest.THEME_EXTRAS_BUNDLE_KEY"
  }
}
