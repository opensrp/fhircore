package org.smartregister.fhircore.quest.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.smartregister.fhircore.engine.domain.model.NotificationData
import org.smartregister.fhircore.engine.domain.notification.FhirNotificationManager
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import javax.inject.Inject
import kotlin.random.Random

class QuestNotificationManager @Inject constructor(
    @ApplicationContext val context: Context
): FhirNotificationManager {
    override fun showNotification(notification: NotificationData) {
        sendNotification(notification)
    }

    override fun scheduleNotification(
        notification: NotificationData,
        scheduleTimeInMillis: Int
    ) {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalMaterialApi::class)
    private fun sendNotification(notification: NotificationData) {
        val intent = Intent(context, AppMainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = context.getString(R.string.default_notification_channel_id)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notification.title)
            .setContentText(notification.description)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notification.description))

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)

        manager.notify(Random.nextInt(), notificationBuilder.build())
    }
}