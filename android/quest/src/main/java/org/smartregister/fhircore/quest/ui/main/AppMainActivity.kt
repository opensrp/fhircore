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

package org.smartregister.fhircore.quest.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.sync.State
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_BACK_REFERENCE_KEY
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalMaterialApi
open class AppMainActivity : BaseMultiLanguageActivity(), OnSyncListener {

  @Inject lateinit var syncBroadcaster: SyncBroadcaster

  @Inject lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator

  @Inject lateinit var configService: ConfigService

  val appMainViewModel by viewModels<AppMainViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { AppTheme { MainScreen(appMainViewModel = appMainViewModel) } }
    syncBroadcaster.registerSyncListener(this, lifecycleScope)
  }

  override fun onResume() {
    super.onResume()
    appMainViewModel.run {
      refreshDataState.value = true
      retrieveAppMainUiState()
    }
  }

  override fun onSync(state: State) {
    Timber.i("Sync state received is $state")
    when (state) {
      is State.Started -> {
        showToast(getString(R.string.syncing))
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, getString(R.string.syncing_initiated))
        )
      }
      is State.InProgress -> {
        Timber.d("Syncing in progress: Resource type ${state.resourceType?.name}")
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, getString(R.string.syncing_in_progress))
        )
      }
      is State.Glitch -> {
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(state, appMainViewModel.retrieveLastSyncTimestamp())
        )
        Timber.w(state.exceptions.joinToString { it.exception.message.toString() })
      }
      is State.Failed -> {
        showToast(getString(R.string.sync_failed))
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(
            state,
            if (!appMainViewModel.retrieveLastSyncTimestamp().isNullOrEmpty())
              getString(R.string.last_sync_timestamp, appMainViewModel.retrieveLastSyncTimestamp())
            else getString(R.string.syncing_failed)
          )
        )
        Timber.e(state.result.exceptions.joinToString { it.exception.message.toString() })
        scheduleFhirTaskStatusUpdater()
      }
      is State.Finished -> {
        showToast(getString(R.string.sync_completed))
        appMainViewModel.run {
          onEvent(
            AppMainEvent.UpdateSyncState(
              state,
              getString(
                R.string.last_sync_timestamp,
                formatLastSyncTimestamp(state.result.timestamp)
              )
            )
          )
          updateLastSyncTimestamp(state.result.timestamp)
        }
        scheduleFhirTaskStatusUpdater()
      }
    }
  }

  private fun scheduleFhirTaskStatusUpdater() {
    // TODO use sharedpref to save the state
    with(configService) {
      if (true /*registerViewModel.applicationConfiguration.scheduleDefaultPlanWorker*/)
        this.schedulePlan(this@AppMainActivity)
      else this.unschedulePlan(this@AppMainActivity)
    }
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK)
      data?.getStringExtra(QUESTIONNAIRE_BACK_REFERENCE_KEY)?.let {
        when {
            it.startsWith(ResourceType.Task.name) -> {
              lifecycleScope.launch(Dispatchers.IO) {
                fhirCarePlanGenerator.completeTask(it.asReference(ResourceType.Task).extractId())
              }
              syncBroadcaster.runSync()
            }
        }
      }
  }
}
