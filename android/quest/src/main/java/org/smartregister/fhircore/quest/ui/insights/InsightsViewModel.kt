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

package org.smartregister.fhircore.quest.ui.insights

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import kotlin.concurrent.fixedRateTimer
import kotlin.math.pow
import kotlin.math.roundToInt

@HiltViewModel
class InsightsViewModel
@Inject
constructor(
  val dispatcherProvider: DispatcherProvider,
  val registerRepository: AppRegisterRepository,
  application: Application
) : AndroidViewModel(application) {

  private val _isRefreshingRamAvailabilityStats = MutableStateFlow(false)
  val isRefreshingRamAvailabilityStatsStateFlow = _isRefreshingRamAvailabilityStats.asStateFlow()

  val isRefreshing = _isRefreshingRamAvailabilityStats.stateIn(viewModelScope, SharingStarted.Lazily, initialValue = false)

  val ramAvailabilityStatsStateFlow = MutableStateFlow("")

  fun refresh() {
    getMemoryInfo()
  }

  init{
    fixedRateTimer("timer", false, 0L, 1000) {
      refresh()
    }
  }

  private fun getMemoryInfo() {
    viewModelScope.launch {

      val actManager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val memInfo = ActivityManager.MemoryInfo()
      actManager.getMemoryInfo(memInfo)

      // Fetch the available and total memory in GB
      val availMemory = memInfo.availMem.toDouble()/(1024*1024*1024)
      val totalMemory= memInfo.totalMem.toDouble()/(1024*1024*1024)

      // Update the RAM Availability stateflow
      ramAvailabilityStatsStateFlow.emit("${(totalMemory - availMemory).roundTo(3)}G/${totalMemory.roundTo(3)}G")
    }
  }
  private fun Double.roundTo(numFractionDigits: Int): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
  }
}
