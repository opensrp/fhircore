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

import java.math.RoundingMode
import java.text.DecimalFormat

fun computeBMIViaMetricUnits(heightInCentimeters: Double, weightInKgs: Double): Double {
  return roundOffDecimal(weightInKgs / ((heightInCentimeters / 100) * (heightInCentimeters / 100)))
}

fun computeBMIViaUSCUnits(heightInInches: Double, weightInPounds: Double): Double {
  return roundOffDecimal(703.00 * (weightInPounds / (heightInInches * heightInInches)))
}

fun roundOffDecimal(number: Double): Double {
  val df = DecimalFormat("#.##")
  df.roundingMode = RoundingMode.CEILING
  return df.format(number).toDouble()
}
