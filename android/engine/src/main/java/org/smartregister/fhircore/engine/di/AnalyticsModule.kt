/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.di

import com.google.firebase.perf.FirebasePerformance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.smartregister.fhircore.engine.trace.FirebasePerformanceReporter
import org.smartregister.fhircore.engine.trace.PerformanceReporter

@Module
@InstallIn(SingletonComponent::class)
class AnalyticsModule {
  @Provides
  fun provideFirebasePerformance(): FirebasePerformance = FirebasePerformance.getInstance()

  @Singleton
  @Provides
  fun providePerformanceReporter(firebasePerformance: FirebasePerformance): PerformanceReporter =
    FirebasePerformanceReporter(firebasePerformance)
}
