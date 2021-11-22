package org.smartregister.fhircore.quest.di.config

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.quest.QuestConfigService

@InstallIn(SingletonComponent::class)
@Module
abstract class ConfigServiceModule {
  @Binds abstract fun provideConfigService(questConfigService: QuestConfigService): ConfigService
}
