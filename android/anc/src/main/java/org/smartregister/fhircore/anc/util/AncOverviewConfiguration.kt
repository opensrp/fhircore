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

package org.smartregister.fhircore.anc.util

data class AncOverviewConfiguration(
  val id: String,
  val eddFilter: SearchFilter?,
  val gaFilter: SearchFilter?,
  val fetusesFilter: SearchFilter?,
  val riskFilter: SearchFilter?,
  val weightFilter: SearchFilter?,
  val heightFilter: SearchFilter?,
  val bloodOxygenLevelFilter: SearchFilter?,
  val bpsFilter: SearchFilter?,
  val bpdsFilter: SearchFilter?,
  val pulseRateFilter: SearchFilter?,
  val bloodGlucoseFilter: SearchFilter?,
  val bmiFilter: SearchFilter?
)
