package org.smartregister.fhircore.engine.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SerializationException
import java.io.InputStream
import java.io.OutputStream

object PractitionerDetailsPreferencesSerializer: Serializer<ProtoDataStoreParams> {
    override val defaultValue: ProtoDataStoreParams
        get() = ProtoDataStoreParams()

    override suspend fun readFrom(input: InputStream): ProtoDataStoreParams {
        return try {
            Json.decodeFromString(
                deserializer = ProtoDataStoreParams.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: ProtoDataStoreParams, output: OutputStream) {
        output.write(
            Json.encodeToString(
                    serializer = ProtoDataStoreParams.serializer(),
                    value = t
            ).encodeToByteArray()
        )
    }
}