package org.dtree.fhircore.dataclerk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.dtree.fhircore.dataclerk.DataClerkConfigService
import org.smartregister.fhircore.engine.configuration.app.ConfigService

@InstallIn(SingletonComponent::class)
@Module
abstract class ConfigServiceModule {
    @Binds
    abstract fun provideConfigService(questConfigService: DataClerkConfigService): ConfigService
}
