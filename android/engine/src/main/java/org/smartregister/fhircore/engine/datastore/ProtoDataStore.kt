package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import org.smartregister.fhircore.engine.datastore.mockdata.SerializablePractitionerDetails
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import org.smartregister.fhircore.engine.datastore.serializers.PractitionerDetailsDataStoreSerializer
import org.smartregister.fhircore.engine.datastore.serializers.UserInfoDataStoreSerializer
import timber.log.Timber
import java.io.IOException
import javax.inject.Singleton

private const val PRACTITIONER_DETAILS_DATASTORE_JSON = "practitioner_details.json"
private const val USER_INFO_DATASTORE_JSON =  "user_info.json"
private const val TAG = "Proto DataStore"

val Context.practitionerProtoStore: DataStore<SerializablePractitionerDetails> by dataStore(
        fileName = PRACTITIONER_DETAILS_DATASTORE_JSON,
        serializer = PractitionerDetailsDataStoreSerializer
)

val Context.userInfoProtoStore: DataStore<SerializableUserInfo> by dataStore(
        fileName = USER_INFO_DATASTORE_JSON,
        serializer = UserInfoDataStoreSerializer
)
@Singleton
class ProtoDataStore @Inject constructor(@ApplicationContext val context: Context) {

    val practitioner = context.practitionerProtoStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.tag(TAG).e(exception, "Error reading practitioner details preferences.")
                    emit(SerializablePractitionerDetails())
                } else {
                    throw exception
                }
            }

    suspend fun writePractitioner(serializablePractitionerDetails: SerializablePractitionerDetails) {
        context.practitionerProtoStore.updateData { practitionerData ->
            practitionerData.copy(
                    name = serializablePractitionerDetails.name,
                    id = serializablePractitionerDetails.id
            )
        }
    }

    val userInfo = context.userInfoProtoStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.tag(TAG).e(exception, "Error reading practitioner details preferences.")
                    emit(SerializableUserInfo())
                } else {
                    throw exception
                }
            }

    suspend fun writeUserInfo(serializableUserInfo: SerializableUserInfo) {
        context.userInfoProtoStore.updateData { userInfo ->
            userInfo.copy(
                    name = serializableUserInfo.name
            )
        }
    }

}
