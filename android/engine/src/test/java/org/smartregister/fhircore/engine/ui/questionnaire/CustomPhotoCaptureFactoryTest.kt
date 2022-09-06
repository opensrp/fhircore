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

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewItem
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class CustomPhotoCaptureFactoryTest : RobolectricTest() {

  @get:Rule var hiltRule = HiltAndroidRule(this)
  private val context: Application = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testCameraLauncherShouldBeRegistered() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))

    every { photoCaptureFactory.onAnswerChanged } returns mockk(relaxed = true)

    val callback = slot<ActivityResultCallback<Bitmap>>()
    every {
      fragment.registerForActivityResult(
        any<ActivityResultContract<Void, Bitmap>>(),
        capture(callback)
      )
    } returns mockk()

    photoCaptureFactory.registerCameraLauncher()

    val byteArray = "image".encodeToByteArray()
    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    callback.captured.onActivityResult(bitmap)

    verify { photoCaptureFactory.loadThumbnail(any()) }
    verify { photoCaptureFactory.populateQuestionnaireResponse(any()) }
  }

  @Test
  fun testQuestionnaireResponseShouldMatch() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))
    val imageBytes = "image".encodeToByteArray()

    Assert.assertEquals(photoCaptureFactory.answers.size, 0)

    photoCaptureFactory.populateQuestionnaireResponse(imageBytes)

    Assert.assertEquals(photoCaptureFactory.answers.size, 1)
    Assert.assertEquals(
      CustomPhotoCaptureFactory.CONTENT_TYPE,
      photoCaptureFactory.answers.firstOrNull()?.valueAttachment?.contentType
    )
    Assert.assertEquals(
      imageBytes,
      photoCaptureFactory.answers.firstOrNull()?.valueAttachment?.data
    )
  }

  @Test
  fun testQuestionnaireItemShouldBindWhenPrefixIsNotNull() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))
    val questionnaireItemViewItem =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          prefix = "1."
          text = "Photo of device"
          readOnly = true
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer().value = Attachment().apply { data = "image".encodeToByteArray() }
        },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )

    val tvPrefix = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvPrefix } returns tvPrefix

    val tvHeader = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvHeader } returns tvHeader

    val ivThumbnail = mockk<ImageView>(relaxed = true)
    every { photoCaptureFactory.ivThumbnail } returns ivThumbnail

    val btnTakePhoto = mockk<MaterialButton>(relaxed = true)
    every { photoCaptureFactory.btnTakePhoto } returns btnTakePhoto

    val callback = slot<View.OnClickListener>()
    every { btnTakePhoto.setOnClickListener(capture(callback)) } answers { callback.captured }
    every { photoCaptureFactory.launchCamera() } returns Unit

    photoCaptureFactory.getQuestionnaireItemViewHolderDelegate().bind(questionnaireItemViewItem)

    callback.captured.onClick(btnTakePhoto)

    verify { tvPrefix.text = "1." }
    verify { tvPrefix.visibility = View.VISIBLE }
    verify { tvHeader.text = "Photo of device" }
  }

  @Test
  fun testQuestionnaireItemShouldBindWhenPrefixIsNull() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))

    val questionnaireItemViewItem =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          text = "Photo of device"
          readOnly = false
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer().value = Attachment().apply { data = "image".encodeToByteArray() }
        },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )

    val tvPrefix = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvPrefix } returns tvPrefix

    val tvHeader = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvHeader } returns tvHeader

    val ivThumbnail = mockk<ImageView>(relaxed = true)
    every { photoCaptureFactory.ivThumbnail } returns ivThumbnail

    val btnTakePhoto = mockk<MaterialButton>(relaxed = true)
    every { photoCaptureFactory.btnTakePhoto } returns btnTakePhoto

    val callback = slot<View.OnClickListener>()
    every { btnTakePhoto.setOnClickListener(capture(callback)) } answers { callback.captured }
    every { photoCaptureFactory.launchCamera() } returns Unit

    photoCaptureFactory.getQuestionnaireItemViewHolderDelegate().bind(questionnaireItemViewItem)

    callback.captured.onClick(btnTakePhoto)

    verify { tvPrefix.visibility = View.GONE }
    verify { tvHeader.text = "Photo of device" }
  }

  @Test
  fun testPhotoCaptureShouldDisplayValidationResult() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))

    val tvError = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvError } returns tvError

    val validationResult = Invalid(listOf("Error"))
    photoCaptureFactory
      .getQuestionnaireItemViewHolderDelegate()
      .displayValidationResult(validationResult)

    verify { tvError.text = "Error" }
  }

  @Test
  fun testPhotoCaptureShouldNotDisplayValidationResult() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))

    val tvError = mockk<TextView>(relaxed = true)
    every { photoCaptureFactory.tvError } returns tvError

    val validationResult = Invalid(listOf())
    photoCaptureFactory
      .getQuestionnaireItemViewHolderDelegate()
      .displayValidationResult(validationResult)

    verify { tvError.text = "" }
  }

  @Test
  fun testPhotoCaptureShouldSetReadOnly() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))

    val ivThumbnail = mockk<ImageView>(relaxed = true)
    every { photoCaptureFactory.ivThumbnail } returns ivThumbnail

    val btnTakePhoto = mockk<MaterialButton>(relaxed = true)
    every { photoCaptureFactory.btnTakePhoto } returns btnTakePhoto

    photoCaptureFactory.getQuestionnaireItemViewHolderDelegate().setReadOnly(true)

    verify { ivThumbnail.isEnabled = false }
    verify { btnTakePhoto.isEnabled = false }
    verify { btnTakePhoto.alpha = 0.6F }
  }

  @Test
  fun testPhotoCaptureShouldNotSetReadOnly() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))

    val ivThumbnail = mockk<ImageView>(relaxed = true)
    every { photoCaptureFactory.ivThumbnail } returns ivThumbnail

    val btnTakePhoto = mockk<MaterialButton>(relaxed = true)
    every { photoCaptureFactory.btnTakePhoto } returns btnTakePhoto

    photoCaptureFactory.getQuestionnaireItemViewHolderDelegate().setReadOnly(false)

    verify { ivThumbnail.isEnabled = true }
    verify { btnTakePhoto.isEnabled = true }
    verify { btnTakePhoto.alpha = 1F }
  }

  @Test
  fun testOnAnswerChangedIsInitialized() {

    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))

    every { photoCaptureFactory.onAnswerChanged } returns mockk(relaxed = true)

    val callback = slot<ActivityResultCallback<Bitmap>>()
    every {
      fragment.registerForActivityResult(
        any<ActivityResultContract<Void, Bitmap>>(),
        capture(callback)
      )
    } returns mockk()

    photoCaptureFactory.registerCameraLauncher()

    val byteArray = "image".encodeToByteArray()
    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    callback.captured.onActivityResult(bitmap)

    photoCaptureFactory.onAnswerChanged.invoke()
    //verify { photoCaptureFactory.onAnswerChanged.invoke() }
    verify { photoCaptureFactory.loadThumbnail(any()) }
    verify { photoCaptureFactory.populateQuestionnaireResponse(any()) }
  }
}
