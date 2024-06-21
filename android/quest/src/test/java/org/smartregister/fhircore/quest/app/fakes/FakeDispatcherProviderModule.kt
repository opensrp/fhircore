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

package org.smartregister.fhircore.quest.app.fakes

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import org.smartregister.fhircore.engine.di.DispatcherModule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.app.testDispatcher

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DispatcherModule::class])
class FakeDispatcherProviderModule {

  @Provides
  @Singleton
  fun provideDispatcherProvider(): DispatcherProvider =
    object : DispatcherProvider {

      override fun main(): CoroutineDispatcher = testDispatcher

      override fun default(): CoroutineDispatcher = testDispatcher

      override fun io(): CoroutineDispatcher = testDispatcher

      override fun unconfined(): CoroutineDispatcher = testDispatcher
    }
}
