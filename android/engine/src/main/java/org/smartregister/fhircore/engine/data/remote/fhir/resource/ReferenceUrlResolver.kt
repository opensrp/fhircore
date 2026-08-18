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

package org.smartregister.fhircore.engine.data.remote.fhir.resource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.UrlResolver
import com.google.android.fhir.get
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Binary
import timber.log.Timber

@Singleton
class ReferenceUrlResolver
@Inject
constructor(val fhirEngine: FhirEngine, val fhirResourceService: FhirResourceService) :
  UrlResolver {

  suspend fun resolveBinaryResource(uri: String): Binary {
    return uri.substringAfter("Binary/").substringBefore("/").run { fhirEngine.get(this) }
  }

  override suspend fun resolveBitmapUrl(url: String): Bitmap? {
    if (url.contains("Binary/")) {
      return try {
        decodeBinaryToBitmap(resolveBinaryResource(url))
          ?: fetchRemoteBitmapIfAbsolute(url)
      } catch (exception: Exception) {
        Timber.e(exception, "Failed to resolve Binary image from $url")
        fetchRemoteBitmapIfAbsolute(url)
      }
    }
    return fetchRemoteBitmap(url)
  }

  private fun decodeBinaryToBitmap(binary: Binary): Bitmap? {
    val bytes = binary.content
    if (bytes == null || bytes.isEmpty()) {
      return null
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  }

  private suspend fun fetchRemoteBitmapIfAbsolute(url: String): Bitmap? {
    return if (url.startsWith("http://") || url.startsWith("https://")) {
      fetchRemoteBitmap(url)
    } else {
      null
    }
  }

  private suspend fun fetchRemoteBitmap(url: String): Bitmap? {
    val response = fhirResourceService.fetchImage(url)
    return if (response != null) {
      BitmapFactory.decodeStream(response.byteStream())
    } else {
      null
    }
  }
}
