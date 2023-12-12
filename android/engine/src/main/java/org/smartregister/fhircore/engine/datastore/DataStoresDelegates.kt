package org.smartregister.fhircore.engine.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.preferencesDataStore
import org.smartregister.fhircore.engine.datastore.mockdata.SerializablePractitionerDetails
import org.smartregister.fhircore.engine.datastore.mockdata.SerializableUserInfo
import org.smartregister.fhircore.engine.datastore.serializers.PractitionerDetailsDataStoreSerializer
import org.smartregister.fhircore.engine.datastore.serializers.UserInfoDataStoreSerializer

// Preferences Datastore
const val DATASTORE_NAME = "app_primitive_params"
val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

// Proto Datastore(s)
private val Context.practitioner: DataStore<SerializablePractitionerDetails> by dataStore(
        fileName = fileNames.PRACTITIONER_DETAILS_DATASTORE_JSON,
        serializer = PractitionerDetailsDataStoreSerializer
)

private val Context.userInfoProtoStore: DataStore<SerializableUserInfo> by dataStore(
        fileName = fileNames.USER_INFO_DATASTORE_JSON,
        serializer = UserInfoDataStoreSerializer
)

// Files that the proto dataStore(s) will read and write to
object fileNames{
    val PRACTITIONER_DETAILS_DATASTORE_JSON = "practitioner_details.json"
    val USER_INFO_DATASTORE_JSON =  "user_info.json"
}
