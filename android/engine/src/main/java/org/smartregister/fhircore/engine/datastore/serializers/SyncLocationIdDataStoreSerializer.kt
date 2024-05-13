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

package org.smartregister.fhircore.engine.datastore.serializers

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.SerializationException
import org.smartregister.fhircore.engine.domain.model.SyncLocationToggleableState
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.json
import timber.log.Timber

object SyncLocationIdDataStoreSerializer : Serializer<List<SyncLocationToggleableState>> {

  override val defaultValue: List<SyncLocationToggleableState>
    get() = emptyList()

  override suspend fun readFrom(input: InputStream): List<SyncLocationToggleableState> {
    return try {
      json.decodeFromString<List<SyncLocationToggleableState>>(input.readBytes().decodeToString())
    } catch (serializationException: SerializationException) {
      Timber.e(serializationException)
      defaultValue
    }
  }

  override suspend fun writeTo(t: List<SyncLocationToggleableState>, output: OutputStream) {
    withContext(Dispatchers.IO) { output.write(t.encodeJson().encodeToByteArray()) }
  }
}
