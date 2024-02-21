package org.smartregister.fhircore.quest.cucumber

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity


class ComposeRuleHolder{

  @get:Rule
  val composeRule = createAndroidComposeRule<AppSettingActivity>()
}

