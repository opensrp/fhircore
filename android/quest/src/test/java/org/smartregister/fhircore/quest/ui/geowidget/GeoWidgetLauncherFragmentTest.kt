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

package org.smartregister.fhircore.quest.ui.geowidget

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.launcher.GeoWidgetLauncherFragment
import org.smartregister.fhircore.quest.ui.launcher.GeoWidgetLauncherViewModel
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.profile.ProfileViewModel
import javax.inject.Inject


@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class GeoWidgetLauncherFragmentTest : RobolectricTest() {

    @get:Rule(order = 0)
    val hiltAndroidRule = HiltAndroidRule(this)

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @BindValue
    val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

    @BindValue
    lateinit var geoWidgetLauncherViewModel: GeoWidgetLauncherViewModel

    @Inject
    lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor

    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    private lateinit var navController: TestNavHostController
    private lateinit var geoWidgetLauncherFragment: GeoWidgetLauncherFragment
    private lateinit var mainActivity: AppMainActivity
    private lateinit var geoWidgetLauncherFragmentMock: GeoWidgetLauncherFragment
    private val activityController = Robolectric.buildActivity(AppMainActivity::class.java)

    @BindValue
    val defaultRepository: DefaultRepository = mockk(relaxUnitFun = true, relaxed = true)

    @BindValue
    lateinit var profileViewModel: ProfileViewModel

    private val resourceConfig = mockk<FhirResourceConfig>()

    @Before
    fun setUp() {
        hiltAndroidRule.inject()
        geoWidgetLauncherViewModel =
            spyk(
                GeoWidgetLauncherViewModel(
                    defaultRepository = defaultRepository,
                    dispatcherProvider = dispatcherProvider,
                    resourceDataRulesExecutor = resourceDataRulesExecutor,
                    sharedPreferencesHelper = sharedPreferencesHelper
                )
            )

        geoWidgetLauncherFragmentMock = mockk()
        geoWidgetLauncherFragment = GeoWidgetLauncherFragment().apply {
            arguments =
                bundleOf(
                    Pair(NavigationArg.GEO_WIDGET_ID, "locationMap"),
                    Pair(NavigationArg.TOOL_BAR_HOME_NAVIGATION, ToolBarHomeNavigation.OPEN_DRAWER),
                )
        }
        activityController.create().resume()
        mainActivity = activityController.get()
        navController =
            TestNavHostController(mainActivity).apply {
                setGraph(org.smartregister.fhircore.quest.R.navigation.application_nav_graph)
            }
        Navigation.setViewNavController(mainActivity.navHostFragment.requireView(), navController)
        mainActivity.supportFragmentManager.run {
            commitNow {
                add(
                    geoWidgetLauncherFragment,
                    GeoWidgetLauncherFragment::class.java.simpleName
                )
            }
            executePendingTransactions()
        }
    }


    @Test
    fun testGeoWidgetLauncherFragmentCreation() {
        Assert.assertTrue(geoWidgetLauncherFragment.view is ComposeView)
        activityController.destroy()
    }


}