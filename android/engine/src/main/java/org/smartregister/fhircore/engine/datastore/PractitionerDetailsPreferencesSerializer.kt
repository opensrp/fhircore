package org.smartregister.fhircore.engine.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SerializationException
import java.io.InputStream
import java.io.OutputStream

object PractitionerDetailsPreferencesSerializer: Serializer<SerializablePractitionerDetails> {
    override val defaultValue: SerializablePractitionerDetails
        get() = SerializablePractitionerDetails()

    override suspend fun readFrom(input: InputStream): SerializablePractitionerDetails {
        return try {
            Json.decodeFromString(
                deserializer = SerializablePractitionerDetails.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
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