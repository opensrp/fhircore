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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewItem
import com.google.android.material.button.MaterialButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class CustomPhotoCaptureFactoryTest : RobolectricTest() {

  @Test
  fun testCameraResultLauncherShouldBeRegistered() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))
    val callback = slot<ActivityResultCallback<ActivityResult>>()

    every {
      fragment.registerForActivityResult(
        any<ActivityResultContract<Intent, ActivityResult>>(),
        capture(callback)
      )
    } returns mockk()

    every { photoCaptureFactory.loadThumbnail(any(), any()) } returns Unit

    photoCaptureFactory.registerCameraResultLauncher(fragment)

    val bitmap = mockk<Bitmap>()
    val result = ActivityResult(Activity.RESULT_OK, Intent().putExtra("data", bitmap))
    callback.captured.onActivityResult(result)

    verify { photoCaptureFactory.loadThumbnail(fragment, bitmap) }
    verify { photoCaptureFactory.populateCameraResponse(fragment, bitmap) }
  }

  @Test
  fun testQuestionnaireResponseShouldReturnAttachmentTypeValue() {
    val fragment = FhirCoreQuestionnaireFragment()
    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))
    val file = "file".toByteArray()

    every { photoCaptureFactory.questionnaireResponse } returns
      QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
        value =
          Attachment().apply {
            contentType = "image/jpg"
            data = file
          }
      }

    val response = photoCaptureFactory.questionnaireResponse
    assertEquals("image/jpg", response.valueAttachment.contentType)
    assertEquals(file, response.valueAttachment.data)
  }

  @Test
  fun testQuestionnaireItemShouldBindWhenPrefixIsNotNull() {
    val fragment = FhirCoreQuestionnaireFragment()
    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))
    val questionnaireItemViewItem =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          prefix = "1."
          text = "Photo of device"
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent()
      ) {}

    val tvPrefix = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvPrefix } returns tvPrefix

    val tvHeader = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvHeader } returns tvHeader

    val btnTakePhoto = mockk<MaterialButton>(relaxed = true)
    every { photoCaptureFactory.btnTakePhoto } returns btnTakePhoto

    photoCaptureFactory.getQuestionnaireItemViewHolderDelegate().bind(questionnaireItemViewItem)

    verify { tvPrefix.text = "1." }
    verify { tvPrefix.visibility = View.VISIBLE }
    verify { tvHeader.text = "Photo of device" }
  }

  @Test
  fun testQuestionnaireItemShouldBindWhenPrefixIsNull() {
    val fragment = FhirCoreQuestionnaireFragment()
    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))
    val questionnaireItemViewItem =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { text = "Photo of device" },
        QuestionnaireResponse.QuestionnaireResponseItemComponent()
      ) {}

    val tvPrefix = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvPrefix } returns tvPrefix

    val tvHeader = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvHeader } returns tvHeader

    val btnTakePhoto = mockk<MaterialButton>(relaxed = true)
    every { photoCaptureFactory.btnTakePhoto } returns btnTakePhoto

    photoCaptureFactory.getQuestionnaireItemViewHolderDelegate().bind(questionnaireItemViewItem)

    verify { tvPrefix.visibility = View.GONE }
    verify { tvHeader.text = "Photo of device" }
  }
}
