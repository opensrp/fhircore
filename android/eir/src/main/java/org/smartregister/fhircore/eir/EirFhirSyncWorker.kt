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

package org.smartregister.fhircore.eir

import android.content.Context
import androidx.work.WorkerParameters
import com.google.android.fhir.sync.FhirSyncWorker
import org.smartregister.fhircore.eir.util.Utils

class EirFhirSyncWorker(private val appContext: Context, workerParams: WorkerParameters) :
  FhirSyncWorker(appContext, workerParams) {

  override fun getSyncData() = Utils.buildResourceSyncParams()

  override fun getDataSource() =
    Utils.buildDatasource(appContext, EirApplication.getContext().eirConfigurations())

  override fun getFhirEngine() = EirApplication.fhirEngine(applicationContext)
}
