package org.smartregister.fhircore.engine.task

import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DateUtils
import org.smartregister.fhircore.engine.util.extension.isPastExpiry

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 10-11-2022.
 */
class FhirTaskExpireUtilTest : RobolectricTest() {

    lateinit var fhirTaskExpireUtil: FhirTaskExpireUtil
    lateinit var mockFhirEngine: FhirEngine

    @Before
    fun setup() {
        fhirTaskExpireUtil = spyk<FhirTaskExpireUtil>(FhirTaskExpireUtil(ApplicationProvider.getApplicationContext()))

        mockFhirEngine = mockk<FhirEngine>()
        every { fhirTaskExpireUtil.getFhirEngine() } returns mockFhirEngine
    }

    @Test
    fun fetchOverdueTasks() {
        val taskList = mutableListOf<Task>()

        for (i in 1..4) {
            taskList.add(spyk(Task().apply {
                id = UUID.randomUUID().toString()
                status = TaskStatus.INPROGRESS
                restriction = Task.TaskRestrictionComponent().apply {
                    period = Period().apply {
                        end = DateUtils.today()
                    }
                }
            }))
        }

        val twoDaysFromToday = DateTime().plusDays(2)

        for (i in 1..8) {
            taskList.add(spyk(Task().apply {
                id = UUID.randomUUID().toString()
                status = TaskStatus.INPROGRESS
                restriction = Task.TaskRestrictionComponent().apply {
                    period = Period().apply {
                        end = twoDaysFromToday.toDate()
                    }
                }
            }))
        }

        coEvery { mockFhirEngine.search<Task>(any()) } returns taskList

        val resultTasks = runBlocking {
            fhirTaskExpireUtil.fetchOverdueTasks()
        }

        assertEquals(4, resultTasks.size)

        taskList.forEach {
            verify { it.isPastExpiry() }
        }
        coVerify { mockFhirEngine.search<Task>(any()) }
    }

    @Test
    fun markTaskExpired() {
        val taskList = mutableListOf<Task>()

        for (i in 1..5) {
            taskList.add(Task().apply {
                id = UUID.randomUUID().toString()
                status = TaskStatus.INPROGRESS
                restriction = Task.TaskRestrictionComponent().apply {
                    period = Period().apply {
                        end = DateUtils.today()
                    }
                }
            })
        }

        coEvery { mockFhirEngine.update(any<Task>()) } just runs

        runBlocking {
            fhirTaskExpireUtil.markTaskExpired(taskList)
        }

        taskList.forEach {
            assertEquals(TaskStatus.CANCELLED, it.status)
            coVerify { mockFhirEngine.update(it) }
        }
    }

    @Test
    fun getAppContext() {
    }
}