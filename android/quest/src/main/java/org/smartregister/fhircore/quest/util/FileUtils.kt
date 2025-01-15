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

package org.smartregister.fhircore.quest.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.zip.ZipException
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.smartregister.fhircore.engine.R
import timber.log.Timber

object FileUtils {
  fun zipFiles(
    zipFile: File,
    files: List<File>,
    zipPassword: CharArray,
    zipParameters: ZipParameters,
    deleteOriginalFiles: Boolean = false,
  ) {
    val zip = ZipFile(zipFile, zipPassword)
    for (file in files) {
      try {
        zip.addFile(file, zipParameters)
      } catch (e: ZipException) {
        Timber.e(e, "${file.absolutePath} could not be added to zip file")
      }
    }

    if (deleteOriginalFiles) {
      for (file in files) {
        try {
          file.delete()
        } catch (e: IOException) {
          Timber.e(e, "${file.absolutePath} could not be deleted")
        } catch (e: SecurityException) {
          Timber.e(e, "No permissions to delete ${file.absolutePath}")
        }
      }
    }
  }

  fun shareFile(context: Context, file: File, mimeType: String = "text/plain") {
    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

    val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_file))

    if (shareIntent.resolveActivity(context.packageManager) != null) {
      context.startActivity(chooser)
    }
  }
}
