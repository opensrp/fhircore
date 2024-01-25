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

package org.smartregister.fhircore.quest

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.test.HiltActivityForTest

/**
 * Inspired by the Hilt Extension in the Android Architectural samples here
 * https://github.com/android/architecture-samples/blob/views-hilt/app/src/androidTest/java/com/example/android/architecture/blueprints/todoapp/HiltExt.kt#L37
 *
 * launchFragmentInContainer from the androidx.fragment:fragment-testing library is NOT possible to
 * use right now as it uses a hardcoded Activity under the hood (i.e. [EmptyFragmentActivity]) which
 * is not annotated with @AndroidEntryPoint.
 *
 * As a workaround, use this function that is equivalent. It requires you to add
 * [HiltActivityForTest] in the debug folder and include it in the debug AndroidManifest.xml file as
 * can be found in this project.
 */
inline fun <reified T : Fragment> launchFragmentInHiltContainer(
  fragmentArgs: Bundle? = null,
  @StyleRes themeResId: Int = R.style.AppTheme,
  navHostController: TestNavHostController? = null,
  crossinline action: Fragment.() -> Unit = {},
) {
  val startActivityIntent =
    Intent.makeMainActivity(
        ComponentName(ApplicationProvider.getApplicationContext(), HiltActivityForTest::class.java),
      )
      .putExtra(
        "androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY",
        themeResId,
      )

  ActivityScenario.launch<HiltActivityForTest>(startActivityIntent).use { scenario ->
    scenario.onActivity { activity ->
      val fragment: Fragment =
        activity.supportFragmentManager.fragmentFactory.instantiate(
          checkNotNull(T::class.java.classLoader),
          T::class.java.name,
        )
      fragment.arguments = fragmentArgs

      fragment.viewLifecycleOwnerLiveData.observeForever {
        if (it != null) {
          navHostController?.let { controller ->
            controller.setGraph(org.smartregister.p2p.R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), controller)
          }
        }
      }

      activity.supportFragmentManager
        .beginTransaction()
        .add(android.R.id.content, fragment, "")
        .commitNow()

      fragment.action()
    }
  }
}
