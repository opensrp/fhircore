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

package org.smartregister.fhircore.anc.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.Sync
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

class SplashActivity : BaseMultiLanguageActivity() {
  @InternalCoroutinesApi
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.splash)

    lifecycleScope.launch {
      Sync.basicSyncJob(this@SplashActivity).stateFlow().collect {
        Timber.i("Got sync update $it")

        when (it) {
          is State.Finished -> {
            showToast(getString(R.string.sync_completed))
            delay(5000)
            startActivity(Intent(this@SplashActivity, FamilyRegisterActivity::class.java))
          }
          is State.Failed -> {
            showToast(getString(R.string.sync_failed))
            startActivity(Intent(this@SplashActivity, FamilyRegisterActivity::class.java))
          }
        }
      }
    }

    AncApplication.schedulePolling()
  }
}
