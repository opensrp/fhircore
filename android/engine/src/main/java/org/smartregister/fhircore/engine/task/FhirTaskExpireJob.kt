package org.smartregister.fhircore.engine.task

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import javax.inject.Singleton

//@HiltWorker
class FhirTaskExpireJob (val context: Context, workerParams: WorkerParameters) //@AssistedInject constructor(@Assisted val context: Context, @Assisted val workerParams: WorkerParameters, val fhirEngine: FhirEngine) :
  : CoroutineWorker(context, workerParams) {

  @Inject lateinit var fhirEngine: FhirEngine

  override suspend fun doWork(): Result {
    val fhirTaskExpireUtil = FhirTaskExpireUtil(context, fhirEngine)

    var dateTasks = fhirTaskExpireUtil.fetchOverdueTasks()
    var maxDate = dateTasks.first
    var tasks = dateTasks.second

    while (tasks.size > 0) {
      fhirTaskExpireUtil.markTaskExpired(tasks)
      dateTasks = fhirTaskExpireUtil.fetchOverdueTasks(from = maxDate)
      maxDate = dateTasks.first
      tasks = dateTasks.second
    }

    return Result.success()
  }
/*
  override suspend fun getForegroundInfo(): ForegroundInfo {
    return super.getForegroundInfo()
  }*/

  companion object {

    const val FHIR_TASK_EXPIRE_JOB_VERSION = "fhir-task-expire-job-version"
    const val TAG = "FhirTaskExpire"

    fun schedule(
      context: Context,
      sharedPreferencesHelper: SharedPreferencesHelper,
      durationInMins: Long,
      version: Long = 1
    ) {
      val currVersion = sharedPreferencesHelper.read(FHIR_TASK_EXPIRE_JOB_VERSION, 0)
      var existingWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
      if (currVersion != version) {
        existingWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE
        sharedPreferencesHelper.write(FHIR_TASK_EXPIRE_JOB_VERSION, version)
      }

      val periodicWorkRequest =
        PeriodicWorkRequestBuilder<FhirTaskExpireJob>(
            durationInMins,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
          )
          .build()

      WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(TAG, existingWorkPolicy, periodicWorkRequest)
    }
  }
}
