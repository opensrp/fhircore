/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.report.indicator

import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class ReportIndicatorFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Inject lateinit var defaultRepository: DefaultRepository

  private val activityController = Robolectric.buildActivity(HiltActivityForTest::class.java)
  private val testReportId = "test-report-id"

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @After
  override fun tearDown() {
    activityController.destroy()
    super.tearDown()
  }

  @Test
  fun testFragmentCreatesViewModelCorrectly() {
    activityController.create().resume()
    val activity = activityController.get()
    val navHostController = TestNavHostController(activity)

    val fragment =
      ReportIndicatorFragment().apply {
        arguments = bundleOf("reportId" to testReportId)
        viewLifecycleOwnerLiveData.observeForever {
          if (it != null) {
            navHostController.setGraph(
              org.smartregister.fhircore.quest.R.navigation.application_nav_graph,
            )
            Navigation.setViewNavController(requireView(), navHostController)
          }
        }
      }

    activity.supportFragmentManager.run {
      commitNow {
        add(android.R.id.content, fragment, ReportIndicatorFragment::class.java.simpleName)
      }
      executePendingTransactions()
    }

    assertNotNull(fragment.reportIndicatorViewModel)
    assertEquals(defaultRepository, fragment.reportIndicatorViewModel.defaultRepository)
  }

  @Test
  fun testFragmentOnCreateViewReturnsComposeView() {
    activityController.create().resume()
    val activity = activityController.get()
    val navHostController = TestNavHostController(activity)

    val fragment =
      ReportIndicatorFragment().apply {
        arguments = bundleOf("reportId" to testReportId)
        viewLifecycleOwnerLiveData.observeForever {
          if (it != null) {
            navHostController.setGraph(
              org.smartregister.fhircore.quest.R.navigation.application_nav_graph,
            )
            Navigation.setViewNavController(requireView(), navHostController)
          }
        }
      }

    activity.supportFragmentManager.run {
      commitNow {
        add(android.R.id.content, fragment, ReportIndicatorFragment::class.java.simpleName)
      }
      executePendingTransactions()
    }

    assertNotNull(fragment.view)
    assertEquals("androidx.compose.ui.platform.ComposeView", fragment.view!!.javaClass.name)
  }

  @Test
  fun testFragmentReceivesReportIdFromArguments() {
    activityController.create().resume()
    val activity = activityController.get()
    val navHostController = TestNavHostController(activity)

    val fragment =
      ReportIndicatorFragment().apply {
        arguments = bundleOf("reportId" to testReportId)
        viewLifecycleOwnerLiveData.observeForever {
          if (it != null) {
            navHostController.setGraph(
              org.smartregister.fhircore.quest.R.navigation.application_nav_graph,
            )
            Navigation.setViewNavController(requireView(), navHostController)
          }
        }
      }

    activity.supportFragmentManager.run {
      commitNow {
        add(android.R.id.content, fragment, ReportIndicatorFragment::class.java.simpleName)
      }
      executePendingTransactions()
    }

    assertNotNull(fragment.arguments)
    assertEquals(testReportId, fragment.arguments?.getString("reportId"))
  }
}
