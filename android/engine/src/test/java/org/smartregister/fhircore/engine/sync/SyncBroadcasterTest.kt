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

package org.smartregister.fhircore.engine.sync

import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.LastSyncJobStatus
import com.google.android.fhir.sync.PeriodicSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.isIn

@ExperimentalCoroutinesApi
@HiltAndroidTest
class SyncBroadcasterTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var configService: ConfigService

  @Inject lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var workManager: WorkManager
  private lateinit var configurationRegistry: ConfigurationRegistry
  private val fhirEngine = mockk<FhirEngine>()
  private lateinit var syncListenerManager: SyncListenerManager
  private lateinit var syncBroadcaster: SyncBroadcaster
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  @Before
  fun setup() {
    hiltAndroidRule.inject()
    MockKAnnotations.init(this)
    configurationRegistry =
      Faker.buildTestConfigurationRegistry(sharedPreferencesHelper, dispatcherProvider)
    syncListenerManager =
      SyncListenerManager(
        configService = configService,
        configurationRegistry = configurationRegistry,
        sharedPreferencesHelper = sharedPreferencesHelper,
        context = ApplicationProvider.getApplicationContext(),
        dispatcherProvider = dispatcherProvider,
      )

    syncBroadcaster =
      spyk(
        SyncBroadcaster(
          configurationRegistry = configurationRegistry,
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          syncListenerManager = syncListenerManager,
          workManager = workManager,
          context = context,
        ),
      )
  }

  @Test
  fun testLoadSyncParamsShouldLoadFromConfiguration() = runTest {
    sharedPreferencesHelper.write(ResourceType.CareTeam.name, listOf("1"))
    sharedPreferencesHelper.write(ResourceType.Organization.name, listOf("2"))
    sharedPreferencesHelper.write(ResourceType.Location.name, listOf("3"))
    val syncParam = syncBroadcaster.syncListenerManager.loadResourceSearchParams()

    Assert.assertTrue(syncParam.isNotEmpty())

    val resourceTypes =
      arrayOf(
          ResourceType.CarePlan,
          ResourceType.Condition,
          ResourceType.Encounter,
          ResourceType.Group,
          ResourceType.Library,
          ResourceType.Measure,
          ResourceType.Observation,
          ResourceType.Patient,
          ResourceType.PlanDefinition,
          ResourceType.Questionnaire,
          ResourceType.QuestionnaireResponse,
          ResourceType.StructureMap,
          ResourceType.Task,
        )
        .sorted()

    Assert.assertEquals(resourceTypes, syncParam.keys.toTypedArray().sorted())

    syncParam.keys
      .asSequence()
      .filter { it.isIn(ResourceType.Binary, ResourceType.StructureMap) }
      .forEach { Assert.assertTrue(syncParam[it]!!.containsKey("_count")) }

    syncParam.keys
      .asSequence()
      .filter { it.isIn(ResourceType.Patient) }
      .forEach {
        Assert.assertTrue(syncParam[it]!!.containsKey("organization"))
        Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
      }

    syncParam.keys
      .asSequence()
      .filter {
        it.isIn(
          ResourceType.Encounter,
          ResourceType.Condition,
          ResourceType.MedicationRequest,
          ResourceType.Task,
          ResourceType.QuestionnaireResponse,
          ResourceType.Observation,
        )
      }
      .forEach {
        Assert.assertTrue(syncParam[it]!!.containsKey("subject.organization"))
        Assert.assertTrue(syncParam[it]!!.containsKey("_count"))
      }

    syncParam.keys
      .asSequence()
      .filter { it.isIn(ResourceType.Questionnaire) }
      .forEach { Assert.assertTrue(syncParam[it]!!.containsKey("_count")) }
  }

  @Test
  fun `loadSyncParams() should load configuration when remote sync preference is missing`() =
    runTest {
      sharedPreferencesHelper.write(ResourceType.CareTeam.name, listOf("1"))
      sharedPreferencesHelper.write(ResourceType.Organization.name, listOf("2"))
      sharedPreferencesHelper.write(ResourceType.Location.name, listOf("3"))
      sharedPreferencesHelper.resetSharedPrefs()

      val syncParam = syncBroadcaster.syncListenerManager.loadResourceSearchParams()

      Assert.assertTrue(syncParam.isNotEmpty())

      val resourceTypes =
        arrayOf(
            ResourceType.CarePlan,
            ResourceType.Condition,
            ResourceType.Encounter,
            ResourceType.Group,
            ResourceType.Library,
            ResourceType.Observation,
            ResourceType.Measure,
            ResourceType.Patient,
            ResourceType.PlanDefinition,
            ResourceType.Questionnaire,
            ResourceType.QuestionnaireResponse,
            ResourceType.StructureMap,
            ResourceType.Task,
          )
          .sorted()

      Assert.assertEquals(resourceTypes, syncParam.keys.toTypedArray().sorted())
    }

  @Test
  fun loadSyncParamsShouldHaveOrganizationId() = runTest {
    val organizationId = "organization-id"
    sharedPreferencesHelper.write(ResourceType.Organization.name, listOf(organizationId))
    val syncParam = syncBroadcaster.syncListenerManager.loadResourceSearchParams()

    // Resource types that can be filtered based on Organization
    val resourceTypes =
      arrayOf(
        ResourceType.CarePlan,
        ResourceType.Condition,
        ResourceType.Encounter,
        ResourceType.Group,
        ResourceType.Observation,
        ResourceType.Patient,
        ResourceType.RelatedPerson,
        ResourceType.QuestionnaireResponse,
        ResourceType.Task,
      )

    Assert.assertTrue(syncParam.isNotEmpty())
    syncParam
      .filterKeys { it.isIn(*resourceTypes) }
      .values
      .forEach { Assert.assertTrue(it.containsValue(organizationId)) }
  }

  // TODO: Not supported yet; need to refactor sync implementation to be based on tags.
  @Test
  fun loadSyncParamsShouldHaveCareTeamIdNotSupported() = runTest {
    val careTeamId = "care-team-id"
    sharedPreferencesHelper.write(ResourceType.CareTeam.name, listOf(careTeamId))
    val syncParam = syncBroadcaster.syncListenerManager.loadResourceSearchParams()

    Assert.assertTrue(syncParam.isNotEmpty())
    syncParam.values.forEach { Assert.assertFalse(it.containsValue(careTeamId)) }
  }

  // TODO: Not supported yet; need to refactor sync implementation to be based on tags.
  @Test
  fun loadSyncParamsShouldNotHaveLocationIdNotSupported() = runTest {
    val locationId = "location-id"
    sharedPreferencesHelper.write(ResourceType.Location.name, listOf(locationId))
    val syncParam = syncBroadcaster.syncListenerManager.loadResourceSearchParams()

    Assert.assertTrue(syncParam.isNotEmpty())
    syncParam.values.forEach { Assert.assertFalse(it.containsValue(locationId)) }
  }

  // TODO: Not supported yet; need to refactor sync implementation to be based on tags.
  @Test
  fun loadSyncParamsShouldNotHavePractitionerIdNotSupported() = runTest {
    val practitionerId = "practitioner-id"
    sharedPreferencesHelper.write(ResourceType.Practitioner.name, listOf(practitionerId))
    val syncParam = syncBroadcaster.syncListenerManager.loadResourceSearchParams()

    Assert.assertTrue(syncParam.isNotEmpty())
    syncParam.values.forEach { Assert.assertFalse(it.containsValue(practitionerId)) }
  }

  private fun periodicStatus(
    current: CurrentSyncJobStatus,
    last: LastSyncJobStatus? = null,
  ): PeriodicSyncJobStatus =
    mockk<PeriodicSyncJobStatus> {
      every { currentSyncJobStatus } returns current
      every { lastSyncJobStatus } returns last
    }

  @Test
  fun periodicStatusToBroadcastSuppressesIdleEnqueuedHeartbeat() {
    val result =
      syncBroadcaster.periodicStatusToBroadcast(
        periodicStatus(CurrentSyncJobStatus.Enqueued, last = null),
        lastKnownTerminalTimestamp = null,
      )
    Assert.assertNull(result)
  }

  @Test
  fun periodicStatusToBroadcastSuppressesRepeatedStaleSucceeded() {
    val timestamp = OffsetDateTime.now()
    val last =
      mockk<LastSyncJobStatus.Succeeded> { every { this@mockk.timestamp } returns timestamp }
    val result =
      syncBroadcaster.periodicStatusToBroadcast(
        periodicStatus(CurrentSyncJobStatus.Enqueued, last = last),
        lastKnownTerminalTimestamp = timestamp,
      )
    Assert.assertNull(result)
  }

  @Test
  fun periodicStatusToBroadcastSurfacesNewSucceededOnce() {
    val timestamp = OffsetDateTime.now()
    val last =
      mockk<LastSyncJobStatus.Succeeded> { every { this@mockk.timestamp } returns timestamp }
    val result =
      syncBroadcaster.periodicStatusToBroadcast(
        periodicStatus(CurrentSyncJobStatus.Enqueued, last = last),
        lastKnownTerminalTimestamp = null,
      )
    Assert.assertTrue(result is CurrentSyncJobStatus.Succeeded)
    Assert.assertEquals(timestamp, (result as CurrentSyncJobStatus.Succeeded).timestamp)
  }

  @Test
  fun periodicStatusToBroadcastSuppressesFailed() {
    val timestamp = OffsetDateTime.now()
    val last = mockk<LastSyncJobStatus.Failed> { every { this@mockk.timestamp } returns timestamp }
    val result =
      syncBroadcaster.periodicStatusToBroadcast(
        periodicStatus(CurrentSyncJobStatus.Enqueued, last = last),
        lastKnownTerminalTimestamp = null,
      )
    Assert.assertNull(result)
  }

  @Test
  fun periodicStatusToBroadcastAlwaysSurfacesRunningProgress() {
    val running =
      CurrentSyncJobStatus.Running(SyncJobStatus.InProgress(SyncOperation.DOWNLOAD, 10, 5))
    val last =
      mockk<LastSyncJobStatus.Succeeded> { every { timestamp } returns OffsetDateTime.now() }
    val result =
      syncBroadcaster.periodicStatusToBroadcast(
        periodicStatus(running, last = last),
        lastKnownTerminalTimestamp = null,
      )
    Assert.assertEquals(running, result)
  }

  @Test
  fun terminalTimestampOfReturnsTimestampForTerminalStatuses() {
    val ts = OffsetDateTime.now()
    val succeeded = mockk<LastSyncJobStatus.Succeeded> { every { timestamp } returns ts }
    val failed = mockk<LastSyncJobStatus.Failed> { every { timestamp } returns ts }
    Assert.assertEquals(ts, syncBroadcaster.terminalTimestampOf(succeeded))
    Assert.assertEquals(ts, syncBroadcaster.terminalTimestampOf(failed))
    Assert.assertNull(syncBroadcaster.terminalTimestampOf(null))
  }

  @Test
  fun startupBaselineSeededFromFirstEmissionSuppressesStaleFailed() {
    val staleTimestamp = OffsetDateTime.now()
    val staleFailed =
      mockk<LastSyncJobStatus.Failed> { every { this@mockk.timestamp } returns staleTimestamp }
    val firstEmission = periodicStatus(CurrentSyncJobStatus.Enqueued, last = staleFailed)

    val seededBaseline = syncBroadcaster.terminalTimestampOf(firstEmission.lastSyncJobStatus)
    Assert.assertEquals(staleTimestamp, seededBaseline)

    val result = syncBroadcaster.periodicStatusToBroadcast(firstEmission, seededBaseline)
    Assert.assertNull(result)
  }

  @Test
  fun startupSeedingStillSurfacesGenuinePeriodicResult() {
    val staleTimestamp = OffsetDateTime.now().minusHours(1)
    val staleFailed =
      mockk<LastSyncJobStatus.Failed> { every { this@mockk.timestamp } returns staleTimestamp }
    val seededBaseline =
      syncBroadcaster.terminalTimestampOf(
        periodicStatus(CurrentSyncJobStatus.Enqueued, last = staleFailed).lastSyncJobStatus,
      )

    val freshTimestamp = OffsetDateTime.now()
    val freshSucceeded =
      mockk<LastSyncJobStatus.Succeeded> { every { this@mockk.timestamp } returns freshTimestamp }
    val result =
      syncBroadcaster.periodicStatusToBroadcast(
        periodicStatus(CurrentSyncJobStatus.Enqueued, last = freshSucceeded),
        lastKnownTerminalTimestamp = seededBaseline,
      )
    Assert.assertTrue(result is CurrentSyncJobStatus.Succeeded)
    Assert.assertEquals(freshTimestamp, (result as CurrentSyncJobStatus.Succeeded).timestamp)
  }

  @Test
  fun isWithinRedundantPeriodicWindowReturnsTrueForEventJustAfterSucceeded() {
    val succeeded = OffsetDateTime.now()
    val event = succeeded.plusSeconds(30)
    Assert.assertTrue(syncBroadcaster.isWithinRedundantPeriodicWindow(event, succeeded))
  }

  @Test
  fun isWithinRedundantPeriodicWindowReturnsFalseBeyondWindow() {
    val succeeded = OffsetDateTime.now()
    val event = succeeded.plusMinutes(3)
    Assert.assertFalse(syncBroadcaster.isWithinRedundantPeriodicWindow(event, succeeded))
  }

  @Test
  fun isWithinRedundantPeriodicWindowReturnsFalseForEventBeforeSucceeded() {
    val succeeded = OffsetDateTime.now()
    val event = succeeded.minusSeconds(30)
    Assert.assertFalse(syncBroadcaster.isWithinRedundantPeriodicWindow(event, succeeded))
  }

  @Test
  fun isWithinRedundantPeriodicWindowReturnsFalseForNullInputs() {
    val timestamp = OffsetDateTime.now()
    Assert.assertFalse(syncBroadcaster.isWithinRedundantPeriodicWindow(null, timestamp))
    Assert.assertFalse(syncBroadcaster.isWithinRedundantPeriodicWindow(timestamp, null))
    Assert.assertFalse(syncBroadcaster.isWithinRedundantPeriodicWindow(null, null))
  }
}
