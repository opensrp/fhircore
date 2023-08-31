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

package org.smartregister.fhircore.quest.ui.main

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.os.Bundle
import androidx.compose.material.ExperimentalMaterialApi
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.nio.file.Files
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.io.path.Path
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalMaterialApi
open class TestAppMainActivity : AppMainActivity() {

  @Inject lateinit var registerRepository: RegisterRepository

  @Inject lateinit var workManager: WorkManager

  override fun onCreate(savedInstanceState: Bundle?) {
    inject()
    runBlocking {
      registerRepository.configurationRegistry.loadConfigurations(
        "app/debug",
        this@TestAppMainActivity,
      )
    }

    // Import the resources.db
    importTheDB()

    // This prevents the app from trying to sync
    setSyncTime()

    super.onCreate(savedInstanceState)

    workManager.cancelAllWork()
  }

  fun setSyncTime() {
    val today = Calendar.getInstance()
    sharedPreferencesHelper.write(
      SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
      formatLastSyncTimestamp(today.time),
    )
  }

  fun formatLastSyncTimestamp(date: Date): String {
    // MMM d, hh:mm aa
    val simpleDateFormat =
      SimpleDateFormat(AppMainViewModel.SYNC_TIMESTAMP_OUTPUT_FORMAT, Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
      }
    return simpleDateFormat.format(date)
  }

  fun importTheDB() {
    val appContext = this.applicationContext

    // Copy over the db
    val resourcesDbInputStream = assets.open("resources.db")

    // Delete the database files
    "/data/data/${appContext.packageName}/databases/resources.db".deleteFileIsExists()
    "/data/data/${appContext.packageName}/databases/resources.db-shm".deleteFileIsExists()
    "/data/data/${appContext.packageName}/databases/resources.db-wal".deleteFileIsExists()

    val databasesFolder = File("/data/data/${appContext.packageName}/databases/")

    if (!databasesFolder.exists()) {
      databasesFolder.mkdirs()
    }

    // Copy over the db
    Files.copy(
      resourcesDbInputStream,
      Path("/data/data/${appContext.packageName}/databases/resources.db"),
    )

    Timber.i("Finished importing the resources.db for UI tests")
    System.out.println("Finished importing the resources.db for UI tests")
  }

  private fun String.deleteFileIsExists() {
    try {
      if (File(this).exists()) Files.delete(Path(this))
    } catch (ex: NoSuchFileException) {
      System.out.println(ex.message)
      ex.printStackTrace()
    }
  }
}
