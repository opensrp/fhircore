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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.random.Random
import org.smartregister.fhircore.engine.domain.model.NotificationData
import org.smartregister.fhircore.engine.domain.notification.FhirNotificationManager
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.main.AppMainActivity

class QuestNotificationManager
@Inject
constructor(
  @ApplicationContext val context: Context,
) : FhirNotificationManager {
  override fun showNotification(notification: NotificationData) {
    sendNotification(notification)
  }

  override fun scheduleNotification(
    notification: NotificationData,
    scheduleTimeInMillis: Int,
  ) {
    TODO("Not yet implemented")
  }

  @OptIn(ExperimentalMaterialApi::class)
  private fun sendNotification(notification: NotificationData) {
    val intent =
      Intent(context, AppMainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE,
      )

    val channelName = context.getString(R.string.default_notification_channel_name)
    val channelId = context.getString(R.string.default_notification_channel_id)

    val notificationBuilder =
      NotificationCompat.Builder(context, channelId)
        .setContentTitle(notification.title)
        .setContentText(notification.description)
        .setSmallIcon(R.drawable.ic_launcher)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setStyle(
          NotificationCompat.BigTextStyle().bigText(notification.description),
        )

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel =
      NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
    manager.createNotificationChannel(channel)

    manager.notify(Random.nextInt(), notificationBuilder.build())
  }
}
