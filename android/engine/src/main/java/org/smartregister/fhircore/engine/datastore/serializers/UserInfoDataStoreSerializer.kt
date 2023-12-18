/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.datastore.serializers

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SerializationException
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import timber.log.Timber

object UserInfoDataStoreSerializer : Serializer<SerializableUserInfo> {
  override val defaultValue: SerializableUserInfo
    get() = SerializableUserInfo()

  override suspend fun readFrom(input: InputStream): SerializableUserInfo {
    return try {
      Json.decodeFromString(
        deserializer = SerializableUserInfo.serializer(),
        string = input.readBytes().decodeToString(),
      )
    } catch (e: SerializationException) {
      Timber.tag(SerializerConstants.PROTOSTORE_SERIALIZER_TAG).d(e)
      defaultValue
    }
  }

  override suspend fun writeTo(t: SerializableUserInfo, output: OutputStream) {
    output.write(
      Json.encodeToString(
          serializer = SerializableUserInfo.serializer(),
          value = t,
        )
        .encodeToByteArray(),
    )
  }
}
