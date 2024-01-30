package org.smartregister.fhircore.engine.datastore.serializers

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SerializationException
import org.smartregister.fhircore.engine.domain.model.TimeStampPreferences
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

object TimeStampDataStoreSerializer: Serializer<TimeStampPreferences> {
  override val defaultValue: TimeStampPreferences
    get() = TimeStampPreferences()

  override suspend fun readFrom(input: InputStream): TimeStampPreferences {
    return try {
      Json.decodeFromString(
        deserializer = TimeStampPreferences.serializer(),
        string = input.readBytes().decodeToString(),
      )
    } catch (e: SerializationException) {
      Timber.tag(SerializerConstants.PROTOSTORE_SERIALIZER_TAG).d(e)
      defaultValue
    }
  }

  override suspend fun writeTo(t: TimeStampPreferences, output: OutputStream) {
    output.write(
      Json.encodeToString(
        serializer = TimeStampPreferences.serializer(),
        value = t,
      )
        .encodeToByteArray(),
    )
  }
}