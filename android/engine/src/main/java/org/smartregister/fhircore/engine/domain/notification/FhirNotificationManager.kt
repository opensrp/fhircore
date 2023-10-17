package org.smartregister.fhircore.engine.domain.notification

import org.smartregister.fhircore.engine.domain.model.NotificationData

interface FhirNotificationManager {
    fun showNotification(notification: NotificationData)
    fun scheduleNotification(notification: NotificationData, scheduleTimeInMillis: Int)
}