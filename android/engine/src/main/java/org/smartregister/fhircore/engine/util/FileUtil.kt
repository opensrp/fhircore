/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.util.Properties
import timber.log.Timber

/** This class has method to help load static files, their contents and properties in android */
object FileUtil {

  val ASSET_BASE_PATH_RESOURCES =
    "${System.getProperty("user.dir") ?: throw NullPointerException("user.dir system property is null")}${File.separator}src${File.separator}"

  /**
   * This method loads a property value from a .config file in android assets
   * @param key property key
   * @param context Android application context
   * @param fileName File name
   */
  fun getProperty(key: String?, context: Context, fileName: String): String? {
    val properties = Properties()
    properties.load(context.assets.open(fileName))
    return properties.getProperty(key)
  }

  /**
   * Method to read Json from file and return string
   * @param fileName File name
   */
  @Throws(IOException::class)
  fun readJsonFile(fileName: String): String {
    val fileNameFinal = ASSET_BASE_PATH_RESOURCES + fileName
    return BufferedReader(InputStreamReader(FileInputStream(fileNameFinal))).use { it.readText() }
  }

  fun writeFileOnInternalStorage(
    context: Context,
    fileName: String,
    body: String = "",
    dirName: String
  ) {
    val dir = File(context.filesDir, dirName)
    if (!dir.exists()) {
      dir.mkdir()
    }
    FileWriter(File(dir, fileName)).use {
      it.append(body)
      it.flush()
    }
  }

  fun readFileFromInternalStorage(context: Context, fileName: String, dirName: String): String {
    val dir = File(context.filesDir, dirName)
    return FileInputStream(File(dir, fileName)).use { fileInputStream ->
      InputStreamReader(fileInputStream).use { inputStreamReader ->
        BufferedReader(inputStreamReader).use { it.readText() }
      }
    }
  }

  /**
   * Method for returning all files in a dir
   * @param dir Directory to recurse
   */
  @Throws(IOException::class)
  fun recurseFolders(dir: File): List<String> {
    val returnedFiles = mutableListOf<String>()
    try {
      val files: Array<out File> = dir.listFiles() ?: return returnedFiles
      files.forEach { file ->
        if (file.isDirectory) {
          returnedFiles += recurseFolders(file)
        } else {
          returnedFiles += file.canonicalPath
        }
      }
    } catch (ioException: IOException) {
      Timber.e(ioException)
    }
    return returnedFiles
  }
}
