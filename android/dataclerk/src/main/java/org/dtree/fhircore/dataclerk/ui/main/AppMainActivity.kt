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

package org.dtree.fhircore.dataclerk.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.launch
import org.dtree.fhircore.dataclerk.ui.home.HomeViewModel
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

@AndroidEntryPoint
class AppMainActivity : BaseMultiLanguageActivity(), OnSyncListener {
  private val appMainViewModel by viewModels<AppMainViewModel>()
  private val homeViewModel by viewModels<HomeViewModel>()
  @Inject lateinit var syncBroadcaster: SyncBroadcaster
  var lastSyncState: SyncJobStatus? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      AppTheme() {
        AppScreen(appMainViewModel, homeViewModel) { appMainViewModel.sync(syncBroadcaster) }
      }
    }

    syncBroadcaster.registerSyncListener(this, lifecycleScope)
    appMainViewModel.run { lifecycleScope.launch { retrieveAppMainUiState(syncBroadcaster) } }

    syncBroadcaster.runSync()
  }

  override fun onSync(state: SyncJobStatus) {
    Timber.i("Sync state received is $state, last state is $lastSyncState")
    when (state) {
      is SyncJobStatus.Started -> {
        if (lastSyncState !is SyncJobStatus.Failed) {
          showToast(getString(org.smartregister.fhircore.engine.R.string.syncing))
        }
        appMainViewModel.onEvent(AppMainEvent.UpdateSyncState(state, null))
      }
      is SyncJobStatus.InProgress -> {
        Timber.d(
          "Syncing in progress: ${state.syncOperation.name} ${state.completed.div(max(state.total, 1).toDouble()).times(100)}%"
        )
        appMainViewModel.onEvent(AppMainEvent.UpdateSyncState(state, null))
      }
      is SyncJobStatus.Glitch -> {
        appMainViewModel.onEvent(AppMainEvent.UpdateSyncState(state, lastSyncTime = null))
        Timber.w(
          (if (state?.exceptions != null) state.exceptions else emptyList()).joinToString {
            it.exception.message.toString()
          }
        )
      }
      is SyncJobStatus.Failed -> {
        if (!state?.exceptions.isNullOrEmpty() &&
            state.exceptions.first().resourceType == ResourceType.Flag
        ) {
          if (lastSyncState !is SyncJobStatus.Failed) {
            showToast(state.exceptions.first().exception.message!!)
          }
          appMainViewModel.onEvent(AppMainEvent.UpdateSyncState(state, lastSyncTime = null))
          return
        }
        if (lastSyncState !is SyncJobStatus.Failed) {
          showToast(getString(org.smartregister.fhircore.engine.R.string.sync_failed_text))
        }

        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(
            state,
            lastSyncTime =
              if (!appMainViewModel.retrieveLastSyncTimestamp().isNullOrEmpty())
                getString(
                  org.smartregister.fhircore.engine.R.string.last_sync_timestamp,
                  appMainViewModel.retrieveLastSyncTimestamp()
                )
              else null
          )
        )
      }
      is SyncJobStatus.Finished -> {
        if (lastSyncState !is SyncJobStatus.Finished) {
          showToast(getString(org.smartregister.fhircore.engine.R.string.sync_completed))
        }
        appMainViewModel.run {
          onEvent(
            AppMainEvent.UpdateSyncState(
              state,
              getString(
                org.smartregister.fhircore.engine.R.string.last_sync_timestamp,
                formatLastSyncTimestamp(state.timestamp)
              )
            )
          )
          updateLastSyncTimestamp(state.timestamp)
        }
      }
    }
    lastSyncState = state
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK)
      data?.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_BACK_REFERENCE_KEY)?.let {
        appMainViewModel.onTaskComplete()
      }
  }
}
