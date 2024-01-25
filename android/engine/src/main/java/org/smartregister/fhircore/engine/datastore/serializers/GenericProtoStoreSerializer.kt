package org.smartregister.fhircore.engine.datastore.serializers

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SerializationException
import org.smartregister.fhircore.engine.domain.model.GenericProtoStoreItems
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream


object GenericProtoStoreSerializer : Serializer<GenericProtoStoreItems> {
  override val defaultValue: GenericProtoStoreItems
    get() = GenericProtoStoreItems()

  override suspend fun readFrom(input: InputStream): GenericProtoStoreItems {
    return try {
      Json.decodeFromString(
        deserializer = GenericProtoStoreItems.serializer(),
        string = input.readBytes().decodeToString(),
      )
    } catch (e: SerializationException) {
      Timber.tag(SerializerConstants.PROTOSTORE_SERIALIZER_TAG).d(e)
      defaultValue
    }
  }

  override suspend fun writeTo(t: GenericProtoStoreItems, output: OutputStream) {
    output.write(
      Json.encodeToString(
        serializer = GenericProtoStoreItems.serializer(),
        value = t,
      )
        .encodeToByteArray(),
    )
  }
}
