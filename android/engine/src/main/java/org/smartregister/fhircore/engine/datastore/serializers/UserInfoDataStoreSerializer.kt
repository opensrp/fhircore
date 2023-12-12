package org.smartregister.fhircore.engine.datastore.serializers

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SerializationException
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import java.io.InputStream
import java.io.OutputStream

object UserInfoDataStoreSerializer: Serializer<SerializableUserInfo> {
    override val defaultValue: SerializableUserInfo
        get() = SerializableUserInfo()

    override suspend fun readFrom(input: InputStream): SerializableUserInfo {
        return try {
            Json.decodeFromString(
                    deserializer = SerializableUserInfo.serializer(),
                    string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: SerializableUserInfo, output: OutputStream) {
        output.write(
                Json.encodeToString(
                        serializer = SerializableUserInfo.serializer(),
                        value = t
                ).encodeToByteArray()
        )
    }
}