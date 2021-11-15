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

package org.smartregister.fhircore.quest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.VisibleForTesting
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.AttachmentResolver
import org.hl7.fhir.r4.model.Binary
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService

class ReferenceAttachmentResolver(val application: QuestApplication) : AttachmentResolver {

  override suspend fun resolveBinaryResource(uri: String): Binary? {
    return uri.substringAfter("Binary/").substringBefore("/").run {
      application.fhirEngine.load(Binary::class.java, this)
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun getFhirService(): FhirResourceService {
    return FhirResourceService.create(FhirContext.forR4().newJsonParser(), application)
  }

  override suspend fun resolveImageUrl(uri: String): Bitmap? {
    return getFhirService().fetchImage(uri).execute().run {
      if (this.body() != null) {
        BitmapFactory.decodeStream(this.body()?.byteStream())
      } else {
        null
      }
    }
  }
}
