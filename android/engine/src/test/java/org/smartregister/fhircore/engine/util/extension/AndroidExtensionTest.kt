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

package org.smartregister.fhircore.engine.util.extension

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType

class AndroidExtensionTest : RobolectricTest() {
  private lateinit var context: Application

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext<Application>()
  }

  @Test
  fun testGetDrawableShouldReturnDefaultIfInvalidNameSpecified() {
    val result = context.getDrawable("invalid")
    val expected = context.getDrawable(R.drawable.ic_app_logo)!!
    assertEquals(expected.constantState, result.constantState)
  }

  @Test
  fun testGetDrawableShouldReturnCorrectDrawable() {
    val result = context.getDrawable("ic_menu")

    val expected = context.getDrawable(R.drawable.ic_menu)!!
    assertEquals(expected.constantState, result.constantState)
  }

  @Test
  fun `Activity#refresh() should call startActivity and finish()`() {
    val activity = spyk(LoginActivity())
    val intentCapture = slot<Intent>()

    every { activity.packageName } returns "package-name"
    every { activity.startActivity(any()) } just runs

    activity.refresh()

    verify { activity.startActivity(capture(intentCapture)) }
    verify { activity.finish() }

    assertEquals(activity.javaClass.name, intentCapture.captured.component?.className)
    assertEquals(activity.packageName, intentCapture.captured.component?.packageName)

    // Fixes a compose and activity test failure
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun launchQuestionnaireCallsStartActivity() {
    val ctx = mockk<Context>()
    every { ctx.packageName } returns context.packageName
    every { ctx.startActivity(any()) } just runs
    ctx.launchQuestionnaire<QuestionnaireActivity>(
      "testQuestionnaire",
      clientIdentifier = null,
      populationResources = arrayListOf()
    )

    verify { ctx.startActivity(any()) }
  }

  @Test
  fun launchQuestionnaireForResultCallsStartActivityForResultWithRequestCode() {
    val ctx = spyk<Activity>()
    every { ctx.packageName } returns context.packageName
    every { ctx.startActivityForResult(any(), any()) } just runs
    ctx.launchQuestionnaireForResult<QuestionnaireActivity>(
      "testQuestionnaire",
      clientIdentifier = null,
      populationResources = arrayListOf()
    )
    verify { ctx.startActivityForResult(any(), withArg { assertEquals(0, it) }) }
  }

  @Test
  fun launchQuestionnaireForResultCallsStartActivityForResultWithNullLaunchContexts() {
    val ctx = spyk<Activity>()
    every { ctx.packageName } returns context.packageName
    every { ctx.startActivityForResult(any(), any()) } just runs
    ctx.launchQuestionnaireForResult<QuestionnaireActivity>(
      "testQuestionnaire",
      launchContexts = null,
      populationResources = arrayListOf()
    )
    verify { ctx.startActivityForResult(any(), withArg { assertEquals(0, it) }) }
  }

  @Test
  fun launchQuestionnaireCallsStartActivityForResultWithNullLaunchContexts() {
    val ctx = spyk<Activity>()
    every { ctx.packageName } returns context.packageName
    every { ctx.startActivity(any()) } just runs
    ctx.launchQuestionnaire<QuestionnaireActivity>(
      "testQuestionnaire",
      launchContexts = null,
      populationResources = arrayListOf()
    )
    verify { ctx.startActivity(any()) }
  }

  @Test
  fun launchQuestionnaire_StartActivityCalledWithAllParameters() {
    val questionnaireId = "questionnaire_id"
    val clientIdentifier = "client_identifier"
    val groupIdentifier = "group_identifier"
    val intentBundle = Bundle.EMPTY
    val questionnaireType = QuestionnaireType.DEFAULT
    val launchContexts = mockk<ArrayList<Resource>>(relaxed = true)
    val populationResources = mockk<ArrayList<Resource>>(relaxed = true)

    val ctx = spyk<Activity>()
    every { ctx.packageName } returns context.packageName
    every { ctx.startActivity(any()) } just runs

    ctx.launchQuestionnaire<QuestionnaireActivity>(
      questionnaireId = questionnaireId,
      clientIdentifier = clientIdentifier,
      groupIdentifier = groupIdentifier,
      intentBundle = intentBundle,
      questionnaireType = questionnaireType,
      launchContexts = launchContexts,
      populationResources = populationResources
    )

    val expectedIntent =
      Intent(ctx, QuestionnaireActivity::class.java)
        .putExtras(intentBundle)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = clientIdentifier,
            groupIdentifier = groupIdentifier,
            formName = questionnaireId,
            questionnaireType = questionnaireType,
            launchContexts = launchContexts,
            populationResources = populationResources
          )
        )

    verify {
      ctx.startActivity(
        withArg {
          assertEquals(
            expectedIntent.getStringExtra("clientIdentifier"),
            it.getStringExtra("clientIdentifier")
          )
          assertEquals(
            expectedIntent.getStringExtra("groupIdentifier"),
            it.getStringExtra("groupIdentifier")
          )
          assertEquals(expectedIntent.getStringExtra("formName"), it.getStringExtra("formName"))
          assertEquals(
            expectedIntent.getStringExtra("questionnaireType"),
            it.getStringExtra("questionnaireType")
          )
          assertEquals(
            expectedIntent.getStringArrayListExtra("launchContexts"),
            it.getStringArrayListExtra("launchContexts")
          )
          assertEquals(
            expectedIntent.getStringArrayListExtra("populationResources"),
            it.getStringArrayListExtra("populationResources")
          )
        }
      )
    }
  }

  @Test
  fun launchQuestionnaireCallsStartActivityForResultWithAllParameters() {
    val questionnaireId = "questionnaire_id"
    val clientIdentifier = "client_identifier"
    val backReference = "back_reference"
    val intentBundle = Bundle.EMPTY
    val questionnaireType = QuestionnaireType.DEFAULT
    val launchContexts = mockk<ArrayList<Resource>>(relaxed = true)
    val populationResources = mockk<ArrayList<Resource>>(relaxed = true)

    val ctx = spyk<Activity>()
    every { ctx.packageName } returns context.packageName
    every { ctx.startActivityForResult(any(), any()) } just runs

    ctx.launchQuestionnaireForResult<QuestionnaireActivity>(
      questionnaireId = questionnaireId,
      clientIdentifier = clientIdentifier,
      questionnaireType = questionnaireType,
      backReference = backReference,
      intentBundle = intentBundle,
      launchContexts = launchContexts,
      populationResources = populationResources
    )

    val expectedIntent =
      Intent(ctx, QuestionnaireActivity::class.java)
        .putExtras(intentBundle)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = clientIdentifier,
            backReference = backReference,
            formName = questionnaireId,
            questionnaireType = questionnaireType,
            launchContexts = launchContexts,
            populationResources = populationResources
          ),
        )

    verify {
      ctx.startActivityForResult(
        withArg {
          assertEquals(
            expectedIntent.getStringExtra("clientIdentifier"),
            it.getStringExtra("clientIdentifier")
          )
          assertEquals(
            expectedIntent.getStringExtra("backReference"),
            it.getStringExtra("backReference")
          )
          assertEquals(expectedIntent.getStringExtra("formName"), it.getStringExtra("formName"))
          assertEquals(
            expectedIntent.getStringExtra("questionnaireType"),
            it.getStringExtra("questionnaireType")
          )
          assertEquals(
            expectedIntent.getStringArrayListExtra("launchContexts"),
            it.getStringArrayListExtra("launchContexts")
          )
          assertEquals(
            expectedIntent.getStringArrayListExtra("populationResources"),
            it.getStringArrayListExtra("populationResources")
          )
        },
        0
      )
    }
  }
}
