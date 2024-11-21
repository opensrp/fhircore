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

package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import org.smartregister.fhircore.engine.datastore.mockdata.PractitionerDetails
import org.smartregister.fhircore.engine.datastore.mockdata.UserInfo
import org.smartregister.fhircore.engine.datastore.serializers.LocationCoordinatesSerializer
import org.smartregister.fhircore.engine.datastore.serializers.PractitionerDetailsDataStoreSerializer
import org.smartregister.fhircore.engine.datastore.serializers.SyncLocationIdDataStoreSerializer
import org.smartregister.fhircore.engine.datastore.serializers.UserInfoDataStoreSerializer
import org.smartregister.fhircore.engine.domain.model.SyncLocationState
import org.smartregister.fhircore.engine.rulesengine.services.LocationCoordinate
import timber.log.Timber

private const val PRACTITIONER_DETAILS_DATASTORE_JSON = "practitioner_details.json"
private const val USER_INFO_DATASTORE_JSON = "user_info.json"
private const val LOCATION_COORDINATES_DATASTORE_JSON = "location_coordinates.json"
private const val SYNC_LOCATION_IDS = "sync_location_ids.json"
private const val DATA_FILTER_LOCATION_IDS = "data_filter_location_ids.json"

val Context.practitionerProtoStore: DataStore<PractitionerDetails> by
  dataStore(
    fileName = PRACTITIONER_DETAILS_DATASTORE_JSON,
    serializer = PractitionerDetailsDataStoreSerializer,
  )

val Context.userInfoProtoStore: DataStore<UserInfo> by
  dataStore(
    fileName = USER_INFO_DATASTORE_JSON,
    serializer = UserInfoDataStoreSerializer,
  )

val Context.locationCoordinatesDatastore: DataStore<LocationCoordinate> by
  dataStore(
    fileName = LOCATION_COORDINATES_DATASTORE_JSON,
    serializer = LocationCoordinatesSerializer,
  )

val Context.syncLocationIdsProtoStore: DataStore<Map<String, SyncLocationState>> by
  dataStore(
    fileName = SYNC_LOCATION_IDS,
    serializer = SyncLocationIdDataStoreSerializer,
  )
val Context.dataFilterLocationIdsProtoStore: DataStore<Map<String, SyncLocationState>> by
  dataStore(
    fileName = DATA_FILTER_LOCATION_IDS,
    serializer = SyncLocationIdDataStoreSerializer,
  )

@Singleton
class ProtoDataStore @Inject constructor(@ApplicationContext val context: Context) {

  val practitioner =
    context.practitionerProtoStore.data.catch { exception ->
      if (exception is IOException) {
        Timber.e(exception, "Error reading practitioner details preferences.")
        emit(PractitionerDetails())
      } else {
        throw exception
      }
    }

  suspend fun writePractitioner(practitionerDetails: PractitionerDetails) {
    context.practitionerProtoStore.updateData { practitionerData ->
      practitionerData.copy(
        name = practitionerDetails.name,
        id = practitionerDetails.id,
      )
    }
  }

  val userInfo =
    context.userInfoProtoStore.data.catch { exception ->
      if (exception is IOException) {
        Timber.e(exception, "Error reading user information details preferences.")
        emit(UserInfo())
      } else {
        throw exception
      }
    }

  suspend fun writeUserInfo(userInfo: UserInfo) {
    context.userInfoProtoStore.updateData { userInfo ->
      userInfo.copy(
        name = userInfo.name,
      )
    }
  }

  val locationCoordinates =
    context.locationCoordinatesDatastore.data.catch { exception ->
      if (exception is IOException) {
        Timber.e(exception, "Error reading location co-ordinates details.")
        emit(LocationCoordinate())
      } else {
        throw exception
      }
    }

  suspend fun writeLocationCoordinates(locationCoordinatesDetails: LocationCoordinate) {
    context.locationCoordinatesDatastore.updateData { locationCoordinatesData ->
      locationCoordinatesData.copy(
        longitude = locationCoordinatesDetails.longitude,
        latitude = locationCoordinatesDetails.latitude,
        altitude = locationCoordinatesDetails.altitude,
      )
    }
  }
}
