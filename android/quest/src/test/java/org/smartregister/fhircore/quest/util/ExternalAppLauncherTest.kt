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

package org.smartregister.fhircore.quest.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ExternalAppConfig
import org.smartregister.fhircore.quest.R

class ExternalAppLauncherTest {

  private val context = mockk<Context>(relaxed = true)
  private val packageManager = mockk<PackageManager>()

  @Before
  fun setUp() {
    every { context.packageManager } returns packageManager
    mockkStatic(Toast::class)
  }

  @After
  fun tearDown() {
    unmockkStatic(Toast::class)
  }

  @Test
  fun openExternalAppLaunchesAppWhenInstalled() {
    val config =
      ExternalAppConfig(
        id = "elearning",
        label = "Go to eLearning",
        packageName = "com.example.app",
        playStoreUrl = "https://play.google.com/store/apps/details?id=com.example.app",
      )

    val mockLaunchIntent = mockk<Intent>(relaxed = true)
    every { packageManager.getLaunchIntentForPackage("com.example.app") } returns mockLaunchIntent
    every { context.startActivity(mockLaunchIntent) } returns Unit

    openExternalApp(context, config)

    verify { context.startActivity(mockLaunchIntent) }
  }

  @Test
  fun openExternalAppOpensPlayStoreWhenNotInstalledAndUrlProvided() {
    val config =
      ExternalAppConfig(
        id = "elearning",
        label = "Go to eLearning",
        packageName = "com.example.app",
        playStoreUrl = "https://play.google.com/store/apps/details?id=com.example.app",
      )

    every { packageManager.getLaunchIntentForPackage("com.example.app") } returns null

    openExternalApp(context, config)

    verify(exactly = 1) { context.startActivity(any()) }
  }

  @Test
  fun openExternalAppShowsToastWhenNotInstalledAndNoUrlProvided() {
    val config =
      ExternalAppConfig(
        id = "elearning",
        label = "Go to eLearning",
        packageName = "com.example.app",
        playStoreUrl = null,
      )

    val mockToast = mockk<Toast>(relaxed = true)
    every { packageManager.getLaunchIntentForPackage("com.example.app") } returns null
    every { context.getString(R.string.external_app_not_installed) } returns
      "The app is not installed. Please contact your administrator."
    every {
      Toast.makeText(
        context,
        "The app is not installed. Please contact your administrator.",
        Toast.LENGTH_SHORT,
      )
    } returns mockToast

    openExternalApp(context, config)

    verify {
      Toast.makeText(
        context,
        "The app is not installed. Please contact your administrator.",
        Toast.LENGTH_SHORT,
      )
    }
    verify { mockToast.show() }
  }

  @Test
  fun openExternalAppShowsToastWhenPackageNameIsBlank() {
    val config =
      ExternalAppConfig(
        id = "elearning",
        label = "Go to eLearning",
        packageName = "",
        playStoreUrl = "https://play.google.com/store/apps/details?id=com.example.app",
      )

    val mockToast = mockk<Toast>(relaxed = true)
    every { context.getString(R.string.external_app_invalid_config) } returns
      "Invalid app configuration. Please contact your administrator."
    every {
      Toast.makeText(
        context,
        "Invalid app configuration. Please contact your administrator.",
        Toast.LENGTH_SHORT,
      )
    } returns mockToast

    openExternalApp(context, config)

    verify {
      Toast.makeText(
        context,
        "Invalid app configuration. Please contact your administrator.",
        Toast.LENGTH_SHORT,
      )
    }
    verify { mockToast.show() }
    verify(exactly = 0) { context.startActivity(any()) }
  }
}
