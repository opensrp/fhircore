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

package org.smartregister.fhircore.geowidget.screens

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.mapbox.geojson.FeatureCollection
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.geowidget.rule.CoroutineTestRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = HiltTestApplication::class)
@HiltAndroidTest
class GeoWidgetViewModelTest {

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  val dispatcherProvider: DispatcherProvider = spyk(DefaultDispatcherProvider())

  lateinit var geowidgetViewModel: GeoWidgetViewModel

  val fhirEngine = mockk<FhirEngine>()

  val defaultRepository = spyk(DefaultRepository(fhirEngine, dispatcherProvider))

  val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Before
  fun setUp() {
    geowidgetViewModel = spyk(GeoWidgetViewModel(defaultRepository, dispatcherProvider))

    coEvery { defaultRepository.save(any()) } just runs
  }

  @Test
  fun getFamiliesFeatureCollectionShouldCallGetFamiliesAndGenerateFeatureCollection() {

    val families: List<Pair<Group, Location>> = emptyList()
    coEvery { geowidgetViewModel.getFamilies() } returns families
    val featureCollection = runBlocking { geowidgetViewModel.getFamiliesFeatureCollection(context) }

    coVerify { geowidgetViewModel.getFamilies() }
    Assert.assertNotNull(featureCollection)
  }

  @Test
  fun getFamiliesFeatureCollectionStreamShouldCallGetFamilyFeaturesCollection() {
    val featureCollection = mockk<FeatureCollection>()

    coEvery { geowidgetViewModel.getFamiliesFeatureCollection(context) } returns featureCollection

    val featureCollectionLiveData = geowidgetViewModel.getFamiliesFeatureCollectionStream(context)

    coVerify { geowidgetViewModel.getFamiliesFeatureCollection(context) }
    Assert.assertEquals(featureCollection, featureCollectionLiveData.value)
  }

  @Test
  fun getFamiliesShouldCallFhirEngineSearchAndPairLocationWithFamily() {
    val locationJson =
      """{"resourceType":"Location","id":"136702","meta":{"versionId":"3","lastUpdated":"2022-07-28T18:21:39.739+00:00","source":"#18c074df71ca7366"},"status":"active","name":"Kenyatta Hospital Visitors Parking","description":"Parking Lobby","telecom":[{"system":"phone","value":"020 2726300"},{"system":"phone","value":"(+254)0709854000"},{"system":"phone","value":"(+254)0730643000"},{"system":"email","value":"knhadmin@knh.or.ke"}],"address":{"line":["P.O. Box 20723"],"city":"Nairobi","postalCode":"00202","country":"Kenya"},"physicalType":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/location-physical-type","code":"area","display":"Area"}]},"position":{"longitude":36.80826008319855,"latitude":-1.301070677485388},"managingOrganization":{"reference":"Organization/400"},"partOf":{"reference":"Location/136710"}}"""
    val groupJson =
      """{"resourceType":"Group","id":"1122f50c-5499-4eaa-bd53-a5364371a2ba","meta":{"versionId":"5","lastUpdated":"2022-06-23T14:55:37.217+00:00","source":"#75f9db2107ef0977"},"identifier":[{"use":"official","value":"124"},{"use":"secondary","value":"c90cd5e3-a1c4-4040-9745-433aea9fe174"}],"active":true,"type":"person","code":{"coding":[{"system":"https://www.snomed.org","code":"35359004","display":"Family"}]},"name":"new family","managingEntity":{"reference":"Organization/105"},"characteristic":[{"valueReference":{"reference":"Location/136702"}}],"member":[{"entity":{"reference":"Patient/7d84a2d0-8706-485a-85f5-8313f16bafa1"}},{"entity":{"reference":"Patient/0beaa1e3-64a9-436f-91af-36cbdaff5628"}},{"entity":{"reference":"Patient/a9e466a6-6237-46e0-bcda-c66036414aed"}},{"entity":{"reference":"Patient/7e62cc99-d992-484c-ace8-a43dba87ed22"}},{"entity":{"reference":"Patient/cd1c9616-bdfd-4947-907a-5f08e2bcd8a9"}}]}"""
    val location =
      FhirContext.forR4().newJsonParser().parseResource(Location::class.java, locationJson)
    val group = FhirContext.forR4().newJsonParser().parseResource(Group::class.java, groupJson)

    coEvery { fhirEngine.search<Group>(any()) } returns listOf(group)
    coEvery { fhirEngine.get(ResourceType.Location, any()) } returns location

    val familiesWithLocations = runBlocking { geowidgetViewModel.getFamilies() }

    Assert.assertEquals(group, familiesWithLocations.get(0).first)
    Assert.assertEquals(location, familiesWithLocations.get(0).second)
  }

  @Test
  fun saveLocationShouldCallDefaultRepositorySave() {
    val locationJson =
      """{"resourceType":"Location","id":"136702","meta":{"versionId":"3","lastUpdated":"2022-07-28T18:21:39.739+00:00","source":"#18c074df71ca7366"},"status":"active","name":"Kenyatta Hospital Visitors Parking","description":"Parking Lobby","telecom":[{"system":"phone","value":"020 2726300"},{"system":"phone","value":"(+254)0709854000"},{"system":"phone","value":"(+254)0730643000"},{"system":"email","value":"knhadmin@knh.or.ke"}],"address":{"line":["P.O. Box 20723"],"city":"Nairobi","postalCode":"00202","country":"Kenya"},"physicalType":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/location-physical-type","code":"area","display":"Area"}]},"position":{"longitude":36.80826008319855,"latitude":-1.301070677485388},"managingOrganization":{"reference":"Organization/400"},"partOf":{"reference":"Location/136710"}}"""
    val location =
      FhirContext.forR4().newJsonParser().parseResource(Location::class.java, locationJson)

    val locationLiveData = geowidgetViewModel.saveLocation(location)

    coVerify { defaultRepository.save(location) }
    Assert.assertTrue(locationLiveData.value!!)
  }
}
