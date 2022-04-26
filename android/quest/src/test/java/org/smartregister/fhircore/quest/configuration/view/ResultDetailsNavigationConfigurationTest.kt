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

package org.smartregister.fhircore.quest.configuration.view

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.NavigationOption

class ResultDetailsNavigationConfigurationTest {

  @Test
  fun testDetailsNavigationAction() {
    val resultDetailsNavigationConfiguration =
      ResultDetailsNavigationConfiguration(
        appId = "quest",
        classification = "registration",
        navigationOptions =
          listOf(
            NavigationOption(
              id = "homeNav",
              title = "homeTitle",
              icon = "arrow",
              action = TestDetailsNavigationAction(form = "reg-form", readOnly = true)
            )
          )
      )

    Assert.assertEquals("quest", resultDetailsNavigationConfiguration.appId)
    Assert.assertEquals("registration", resultDetailsNavigationConfiguration.classification)
    val navOption: NavigationOption = resultDetailsNavigationConfiguration.navigationOptions.get(0)
    Assert.assertEquals("homeNav", navOption.id)
    Assert.assertEquals("homeTitle", navOption.title)
    Assert.assertEquals("arrow", navOption.icon)
    val navAction: TestDetailsNavigationAction = navOption.action as TestDetailsNavigationAction
    Assert.assertEquals("reg-form", navAction.form)
    Assert.assertTrue(navAction.readOnly)
  }
}
