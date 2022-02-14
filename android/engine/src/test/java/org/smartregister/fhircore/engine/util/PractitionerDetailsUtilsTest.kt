///*
// * Copyright 2021 Ona Systems, Inc
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
package org.smartregister.fhircore.engine.util
//
//import androidx.arch.core.executor.testing.InstantTaskExecutorRule
//import com.google.android.fhir.FhirEngine
//import dagger.hilt.android.testing.HiltAndroidRule
//import io.mockk.spyk
//import javax.inject.Inject
//import org.junit.Before
//import org.junit.Rule
//import org.smartregister.fhircore.engine.robolectric.RobolectricTest
//
//class PractitionerDetailsUtilsTest : RobolectricTest() {
//
//  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
//
//  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()
//
//  private lateinit var practitionerDetailsUtils: PractitionerDetailsUtils
//
//  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper
//
//  @Before
//  fun setUp() {
//    hiltRule.inject()
//    // Spy needed to control interaction with the real injected dependency
//    val fhirEngine = spyk<FhirEngine>()
//
//    practitionerDetailsUtils =
//      PractitionerDetailsUtils(
//        fhirEngine = fhirEngine,
//        sharedPreferences = sharedPreferencesHelper,
//      )
//  }
//}
