package org.smartregister.fhircore.engine.task

import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.junit.Assert.assertEquals

import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import javax.inject.Inject

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 24-11-2022.
 */
@HiltAndroidTest
class FhirTaskExpireJobTest : RobolectricTest() {

    @BindValue var fhirTaskExpireUtil : FhirTaskExpireUtil = mockk()
    @Inject lateinit var fhirTaskExpireJob: FhirTaskExpireJob

    @Test
    fun doWorkShouldFetchTasksAndMarkAsExpired() {
        val date1 = DateTime()
            .minusDays(4)
            .toDate()
        val date2 = Date()
        val firstBatchTasks = mutableListOf(Task())
        val secondBatchTasks = mutableListOf(Task())

        coEvery { fhirTaskExpireUtil.fetchOverdueTasks() } returns Pair(date1, firstBatchTasks)
        coEvery { fhirTaskExpireUtil.fetchOverdueTasks(from = date1) } returns Pair(date2, secondBatchTasks)
        coEvery { fhirTaskExpireUtil.fetchOverdueTasks(from = date2) } returns Pair(null, mutableListOf())

        val result = runBlocking { fhirTaskExpireJob.doWork() }

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)


        coVerify { fhirTaskExpireUtil.fetchOverdueTasks() }
        coVerify { fhirTaskExpireUtil.fetchOverdueTasks(from = date1) }
        coVerify { fhirTaskExpireUtil.fetchOverdueTasks(from = date2) }

        coVerify { fhirTaskExpireUtil.markTaskExpired(firstBatchTasks) }
        coVerify { fhirTaskExpireUtil.markTaskExpired(secondBatchTasks) }
    }
}