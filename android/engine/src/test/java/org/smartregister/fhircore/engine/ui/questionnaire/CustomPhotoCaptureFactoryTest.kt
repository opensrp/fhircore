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
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewItem
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import id.zelory.compressor.Compressor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.io.File
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
    val file = mockk<File>(relaxed = true)
    every { file.delete() } returns true
    every { photoCaptureFactory.imageFile } returns file

    mockkObject(Compressor)
    coEvery { Compressor.compress(any(), any()) } returns file

    val callback = slot<ActivityResultCallback<Boolean>>()
    every {
      fragment.registerForActivityResult(
        any<ActivityResultContract<Uri, Boolean>>(),
        capture(callback)
      )
    } returns mockk()

    photoCaptureFactory.registerCameraLauncher()
    photoCaptureFactory.loadThumbnail(mockk())
    photoCaptureFactory.populateQuestionnaireResponse("text".encodeToByteArray())

    callback.captured.onActivityResult(true)

    verify { photoCaptureFactory.loadThumbnail(any()) }
    verify { photoCaptureFactory.populateQuestionnaireResponse(any()) }
  }

  @Test
  fun testQuestionnaireResponseShouldMatch() {
    val fragment = spyk<FhirCoreQuestionnaireFragment>()
    every { fragment.requireContext() } returns context

    val photoCaptureFactory = spyk(CustomPhotoCaptureFactory(fragment))
    val imageBytes = "file".encodeToByteArray()

    photoCaptureFactory.populateQuestionnaireResponse(imageBytes)

    Assert.assertTrue(photoCaptureFactory.questionnaireResponse.hasValueAttachment())
    Assert.assertEquals(
      CustomPhotoCaptureFactory.CONTENT_TYPE,
      photoCaptureFactory.questionnaireResponse.valueAttachment.contentType
    )
    Assert.assertEquals(imageBytes, photoCaptureFactory.questionnaireResponse.valueAttachment.data)
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
        }
      ) {}

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

    verify { photoCaptureFactory.createImageFile() }
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
        }
      ) {}

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

    verify { photoCaptureFactory.createImageFile() }
    verify { tvPrefix.visibility = View.GONE }
    verify { tvHeader.text = "Photo of device" }
  }
}
