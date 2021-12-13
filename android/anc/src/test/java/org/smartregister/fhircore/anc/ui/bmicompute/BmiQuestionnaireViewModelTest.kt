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

package org.smartregister.fhircore.anc.ui.bmicompute

import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.details.bmicompute.BmiQuestionnaireViewModel

@HiltAndroidTest
class BmiQuestionnaireViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var patientRepository: PatientRepository

  private lateinit var viewModel: BmiQuestionnaireViewModel
  private val app = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  @Before
  fun setUp() {
    hiltRule.inject()
    viewModel = BmiQuestionnaireViewModel(patientRepository)
  }

  @Test
  fun testInputHeightAsPerSiUnit() {
    val expectedHeightCm = 170.0
    val inputHeight2 = viewModel.getHeightAsPerMetricUnit(170.0, true)
    Assert.assertEquals(expectedHeightCm, inputHeight2, 0.05)

    val expectedHeightMeter = 1.78
    val inputHeight = viewModel.getHeightAsPerMetricUnit(70.0, false)
    Assert.assertEquals(expectedHeightMeter, inputHeight, 0.05)
  }

  @Test
  fun testInputWeightAsPerSiUnit() {
    val expectedWeightKgs = 72.50
    val inputWeight2 = viewModel.getWeightAsPerMetricUnit(72.5, true)
    Assert.assertEquals(expectedWeightKgs, inputWeight2, 0.5)

    val expectedWeightKgs2 = 72.5
    val inputWeight = viewModel.getWeightAsPerMetricUnit(160.5, false)
    Assert.assertEquals(expectedWeightKgs2, inputWeight, 0.5)
  }

  @Test
  fun testCalculateBmi() {
    // via Standard Unit
    val expectedBmi = 22.96
    val computedBmi = viewModel.calculateBmi(70.0, 160.0, false)
    Assert.assertEquals(expectedBmi, computedBmi, 0.1)

    // via metric Unit
    val expectedBmi2 = 22.90
    val computedBmi2 = viewModel.calculateBmi(1.78, 72.57, true)
    Assert.assertEquals(expectedBmi2, computedBmi2, 0.1)
  }

  @Test
  fun testBmiResultHighlightColor() {
    val expectedColor = -65536 // Color.Red
    val resultHighLightColor =
      viewModel.getBmiResultHighlightColor(BmiQuestionnaireViewModel.BmiCategory.OVERWEIGHT)
    Assert.assertEquals(expectedColor, resultHighLightColor)

    val expectedColor2 = -10835103 // "#5AAB61"
    val resultHighLightColor2 =
      viewModel.getBmiResultHighlightColor(BmiQuestionnaireViewModel.BmiCategory.NORMAL)
    Assert.assertEquals(expectedColor2, resultHighLightColor2)
  }

  @Test
  fun testBmiResultCategoryIndices() {
    val expectedIndex = BmiQuestionnaireViewModel.BmiCategory.UNDERWEIGHT
    val resultIndex = viewModel.getBmiResultCategoryIndex(09.00)
    Assert.assertEquals(expectedIndex, resultIndex)

    val expectedIndex2 = BmiQuestionnaireViewModel.BmiCategory.NORMAL
    val resultIndex2 = viewModel.getBmiResultCategoryIndex(19.00)
    Assert.assertEquals(expectedIndex2, resultIndex2)

    val expectedIndex3 = BmiQuestionnaireViewModel.BmiCategory.OVERWEIGHT
    val resultIndex3 = viewModel.getBmiResultCategoryIndex(29.00)
    Assert.assertEquals(expectedIndex3, resultIndex3)

    val expectedIndex4 = BmiQuestionnaireViewModel.BmiCategory.OBESITY
    val resultIndex4 = viewModel.getBmiResultCategoryIndex(39.0)
    Assert.assertEquals(expectedIndex4, resultIndex4)
  }

  @Test
  fun testBmiResultStringIndexInCategories() {
    val expectedIndex = 48
    val resultIndex =
      viewModel.getStartingIndexInCategories(BmiQuestionnaireViewModel.BmiCategory.NORMAL, app)
    Assert.assertEquals(expectedIndex, resultIndex)

    val expectedIndex2 = 75
    val resultIndex2 =
      viewModel.getEndingIndexInCategories(BmiQuestionnaireViewModel.BmiCategory.NORMAL, app)
    Assert.assertEquals(expectedIndex2, resultIndex2)

    val expectedIndex3 = 99
    val resultIndex3 =
      viewModel.getStartingIndexInCategories(BmiQuestionnaireViewModel.BmiCategory.OBESITY, app)
    Assert.assertEquals(expectedIndex3, resultIndex3)

    val expectedIndex4 = 129
    val resultIndex4 =
      viewModel.getEndingIndexInCategories(BmiQuestionnaireViewModel.BmiCategory.OBESITY, app)
    Assert.assertEquals(expectedIndex4, resultIndex4)
  }
}
