package org.smartregister.fhircore.engine.app.di.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.smartregister.fhircore.engine.app.AppConfigService
import org.smartregister.fhircore.engine.configuration.app.ConfigService

@InstallIn(SingletonComponent::class)
@Module
abstract class ConfigServiceModule {
  @Binds abstract fun provideConfigService(appConfigService: AppConfigService): ConfigService
}
