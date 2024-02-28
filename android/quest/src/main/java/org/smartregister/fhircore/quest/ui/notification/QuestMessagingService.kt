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

package org.smartregister.fhircore.quest.ui.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.MessageDefinition
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.NotificationData
import org.smartregister.fhircore.engine.domain.notification.FhirNotificationManager
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@AndroidEntryPoint
class QuestMessagingService : FirebaseMessagingService() {

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var notificationManager: FhirNotificationManager

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var gson: Gson

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    sharedPreferencesHelper.write(SharedPreferenceKey.FIREBASE_TOKEN.name, token)
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    remoteMessage.notification?.let { message ->
      handleNotifications(
        msgTitle = message.title ?: "",
        msgDescription = message.body ?: "",
        msgType = "General",
      )
    }

    remoteMessage.data.let { message ->
      if (message.isNotEmpty()) {
        handleNotifications(
          msgTitle = message["title"] ?: "",
          msgDescription = message["body"] ?: "",
          msgType = message["type"] ?: "General",
        )
      }
    }
  }

  private fun handleNotifications(msgTitle: String, msgDescription: String, msgType: String) {
    CoroutineScope(Dispatchers.IO).launch {
      val notificationData =
        NotificationData(
          title = msgTitle,
          description = msgDescription,
          type = msgType,
        )

      notificationManager.showNotification(notificationData)

      MessageDefinition()
        .apply {
          status = Enumerations.PublicationStatus.ACTIVE
          category = MessageDefinition.MessageSignificanceCategory.NOTIFICATION
          name = gson.toJson(notificationData)
          title = notificationData.title
          description = notificationData.description
          purpose = notificationData.type
          date = Date()
        }
        .run { defaultRepository.addOrUpdate(resource = this) }
    }
  }
}
