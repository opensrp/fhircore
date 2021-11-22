package org.smartregister.fhircore.anc.di.config

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.smartregister.fhircore.anc.AncConfigService
import org.smartregister.fhircore.engine.configuration.app.ConfigService

@InstallIn(SingletonComponent::class)
@Module
abstract class ConfigServiceModule {
    @Binds
    abstract fun provideConfigService(ancConfigService: AncConfigService): ConfigService
}