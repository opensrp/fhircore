package org.smartregister.fhircore.engine.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

@InstallIn(SingletonComponent::class)
@Module
abstract class DispatcherModule {

  @Binds
  abstract fun bindDefaultDispatcherProvider(
    defaultDispatcherProvider: DefaultDispatcherProvider
  ): DispatcherProvider
}
