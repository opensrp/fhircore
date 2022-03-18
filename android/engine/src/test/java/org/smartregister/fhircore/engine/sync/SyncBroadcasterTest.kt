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

package org.smartregister.fhircore.engine.sync

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.SyncJob
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Before
import org.mockito.Mock
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
internal class SyncBroadcasterTest {

  private val syncListenerSpy =
    spyk<OnSyncListener>(
      object : OnSyncListener {
        override fun onSync(state: State) {
          // onSync that does nothing
        }
      }
    )

  private val syncInitiatorSpy =
    spyk<OnSyncListener>(
      object : OnSyncListener {

        override fun onSync(state: State) {
          TODO("Not yet implemented")
        }
      }
    )

  @Mock private lateinit var fhirResourceService: FhirResourceService
  private lateinit var fhirResourceDataSource: FhirResourceDataSource
  @Mock private lateinit var configService: ConfigService
  @Mock private lateinit var context: Context
  @Mock private lateinit var syncJob: SyncJob
  @Mock private lateinit var fhirEngine: FhirEngine
  @Mock private lateinit var dispatcherProvider: DispatcherProvider
  private val sharedSyncStatus: MutableSharedFlow<State> = MutableSharedFlow()

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private lateinit var syncBroadcaster: SyncBroadcaster

  @Before
  fun setup() {
    fhirResourceDataSource = FhirResourceDataSource(fhirResourceService)
    sharedPreferencesHelper = SharedPreferencesHelper(context)
    syncBroadcaster =
      SyncBroadcaster(
        fhirResourceDataSource,
        configService,
        syncJob,
        fhirEngine,
        sharedSyncStatus,
        dispatcherProvider
      )
  }
}
