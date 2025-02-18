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

package org.smartregister.fhircore.engine.di

import android.content.Context
import ca.uhn.fhir.parser.IParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Bundle
import org.smartregister.fhircore.engine.configuration.customsearch.ISearchParametersConfigStore
import org.smartregister.fhircore.engine.configuration.customsearch.SearchParametersConfigService
import org.smartregister.fhircore.engine.util.DispatcherProvider

@InstallIn(SingletonComponent::class)
@Module
class CustomSearchModule {

  @Singleton
  @Provides
  @SearchParametersFileStore
  fun provideCustomSearchParameterConfigStore(
    @ApplicationContext context: Context,
    parser: IParser,
    dispatcherProvider: DispatcherProvider,
  ): ISearchParametersConfigStore {
    val searchParametersFileName = "customSearchParameters.json"
    val searchParametersFile = File(context.filesDir, searchParametersFileName)

    return object : ISearchParametersConfigStore {
      override suspend fun write(bundle: Bundle) {
        withContext(dispatcherProvider.io()) {
          FileOutputStream(searchParametersFile).use {
            it.write(parser.encodeResourceToString(bundle).toByteArray())
          }
        }
      }

      override fun read(): Bundle? {
        if (!searchParametersFile.exists()) return null
        return FileInputStream(searchParametersFile).bufferedReader().use {
          parser.parseResource(Bundle::class.java, it)
        }
      }
    }
  }

  @Singleton
  @Provides
  fun provideCustomSearchParameterService(
    @SearchParametersFileStore searchParametersStore: ISearchParametersConfigStore,
  ): SearchParametersConfigService {
    return SearchParametersConfigService(searchParametersStore)
  }
}
