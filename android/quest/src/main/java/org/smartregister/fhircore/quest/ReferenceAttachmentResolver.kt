package org.smartregister.fhircore.quest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.AttachmentResolver
import org.hl7.fhir.r4.model.Binary

class ReferenceAttachmentResolver(val context: Context) : AttachmentResolver {

  override suspend fun resolveBinaryResource(uri: String): Binary? {
    return uri.substringAfter("Binary/").substringBefore("/").run {
      QuestApplication.getContext().fhirEngine.load(Binary::class.java, this)
    }
  }

  override suspend fun resolveImageUrl(uri: String): Bitmap? {
    /**
     * [fetchImage] needs to be defined in the [QuestFhirService]
     * as below:
     *
     *  @GET fun fetchImage(@Url url: String): Call<ResponseBody?>
     *
     */
    return QuestFhirService.create(FhirContext.forR4().newJsonParser())
      .fetchImage(uri)
      .execute()
      .run {
        if (this.body() != null) {
          BitmapFactory.decodeStream(this.body()?.byteStream())
        } else {
          null
        }
      }
  }
}
