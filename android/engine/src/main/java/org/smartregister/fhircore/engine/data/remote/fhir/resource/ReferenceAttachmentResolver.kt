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

package org.smartregister.fhircore.engine.data.remote.fhir.resource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Binary

interface AttachmentResolver {

  suspend fun resolveBinaryResource(uri: String): Binary?

  suspend fun resolveImageUrl(uri: String): Bitmap?
}

@Singleton
class ReferenceAttachmentResolver
@Inject
constructor(val fhirEngine: FhirEngine, val fhirResourceService: FhirResourceService) :
  AttachmentResolver {

  override suspend fun resolveBinaryResource(uri: String): Binary {
    return uri.substringAfter("Binary/").substringBefore("/").run { fhirEngine.get(this) }
  }

  override suspend fun resolveImageUrl(uri: String): Bitmap? {
    return fhirResourceService.fetchImage(uri).execute().run {
      if (this.body() != null) {
        BitmapFactory.decodeStream(this.body()?.byteStream())
      } else {
        null
      }
    }
  }
}
