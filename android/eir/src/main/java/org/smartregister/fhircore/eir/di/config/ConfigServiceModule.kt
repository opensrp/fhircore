package org.smartregister.fhircore.eir.di.config

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.smartregister.fhircore.eir.EirConfigService
import org.smartregister.fhircore.engine.configuration.app.ConfigService

@InstallIn(SingletonComponent::class)
@Module
abstract class ConfigServiceModule {

  @Binds abstract fun bindConfigService(eirConfigService: EirConfigService): ConfigService
}
