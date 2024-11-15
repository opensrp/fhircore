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

package org.smartregister.fhircore.engine.util

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import java.io.File
import org.hl7.fhir.r4.model.MetadataResource
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.util.extension.referenceValue

object KnowledgeManagerUtil {
  const val KNOWLEDGE_MANAGER_ASSETS_SUBFOLDER = "km"
  private val fhirContext = FhirContext.forR4Cached()

  /**
   * Util method that creates a physical file and writes the Metadata FHIR resource content to it.
   * Note the filepath provided is appended to the apps private directory as returned by
   * Context.filesDir
   *
   * @param subFilePath the path of the file but within the apps private directory
   *   {Context.filesDir}
   * @param metadataResource the actual FHIR Resource of type MetadataResource
   * @param configService the configuration service
   * @param context the application context
   * @return File the file object after creating and writing
   */
  fun writeToFile(
    subFilePath: String,
    metadataResource: MetadataResource,
    configService: ConfigService,
    context: Context,
  ): File =
    context
      .createFileInPrivateDirectory(subFilePath)
      .also { it.parentFile?.mkdirs() }
      .apply {
        writeText(
          fhirContext
            .newJsonParser()
            .encodeResourceToString(
              metadataResource.overwriteCanonicalURL(configService.provideAuthConfiguration()),
            ),
        )
      }

  private fun Context.createFileInPrivateDirectory(filePath: String) = File(this.filesDir, filePath)

  private fun MetadataResource.overwriteCanonicalURL(authConfiguration: AuthConfiguration) =
    this.apply {
      url =
        url
          ?: """${authConfiguration.fhirServerBaseUrl.trimEnd { it == '/' }}/${this.referenceValue()}"""
    }
}
