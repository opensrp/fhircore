package org.smartregister.fhircore.quest.di.config

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.smartregister.fhircore.engine.domain.notification.FhirNotificationManager
import org.smartregister.fhircore.quest.ui.notification.QuestNotificationManager

@InstallIn(SingletonComponent::class)
@Module
abstract class NotificationManagerModule {
    @Binds
    abstract fun provideNotificationManager(questNotificationManager: QuestNotificationManager): FhirNotificationManager
}