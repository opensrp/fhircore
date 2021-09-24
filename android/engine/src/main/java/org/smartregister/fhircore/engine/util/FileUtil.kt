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

package org.smartregister.fhircore.engine.util

import android.content.Context
import android.content.res.AssetManager
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Properties

/** This class has method to help load static files, their contents and properties in android */
class FileUtil {

  /**
   * This method loads a property value from a .config file in android assets
   * @param key property key
   * @param context Android application context
   * @param fileName File name
   */
  fun getProperty(key: String?, context: Context, fileName: String): String? {
    val properties = Properties()
    val assetManager: AssetManager = context.getAssets()
    val inputStream: InputStream = assetManager.open(fileName)
    properties.load(inputStream)
    return properties.getProperty(key)
  }

  /**
   * Method to read Json from file and return string
   * @param fileName File name
   */
  @Throws(IOException::class)
  fun readJsonFile(fileName: String): String {
    var fileNameFinal = ASSET_BASE_PATH_RESOURCES + fileName
    val br = BufferedReader(InputStreamReader(FileInputStream(fileNameFinal)))
    val sb = StringBuilder()
    var line = br.readLine()
    while (line != null) {
      sb.append(line)
      line = br.readLine()
    }
    return sb.toString()
  }

  /**
   * Method for returning all files in a dir
   * @param dir Directory to recurse
   */
  @Throws(IOException::class)
  fun recurse(
    dir:File
  ): List<String> {
    val returnFiles: MutableList<String> = ArrayList()
    try {
      val files: Array<File> = dir.listFiles()
      for (file in files) {
        if (file.isDirectory()) {
          recurse(file)
        } else {
          returnFiles.add(file.getCanonicalPath())
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return returnFiles
  }

  companion object {
    val ASSET_BASE_PATH_RESOURCES =
      (System.getProperty("user.dir") + File.separator + "src" + File.separator)
  }
}
