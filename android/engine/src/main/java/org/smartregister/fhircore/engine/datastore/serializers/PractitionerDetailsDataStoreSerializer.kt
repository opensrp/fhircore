package org.smartregister.fhircore.engine.datastore.serializers

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SerializationException
import org.smartregister.fhircore.engine.datastore.mockdata.SerializablePractitionerDetails
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

object PractitionerDetailsDataStoreSerializer: Serializer<SerializablePractitionerDetails> {
    override val defaultValue: SerializablePractitionerDetails
        get() = SerializablePractitionerDetails()

    override suspend fun readFrom(input: InputStream): SerializablePractitionerDetails {
        return try {
            Json.decodeFromString(
                deserializer = SerializablePractitionerDetails.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            Timber.tag(SerializerConstants.PROTOSTORE_SERIALIZER_TAG).d(e)
            defaultValue
        }
    }

    override suspend fun writeTo(t: SerializablePractitionerDetails, output: OutputStream) {
        output.write(
            Json.encodeToString(
                    serializer = SerializablePractitionerDetails.serializer(),
                    value = t
            ).encodeToByteArray()
        )
    }
}