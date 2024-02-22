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

package org.smartregister.fhircore.geowidget

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.mapbox.geojson.Point
import java.util.LinkedList
import org.apache.commons.codec.binary.Base64
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class KujakuFhirCoreConverterTest {

  private lateinit var kujakuConverter: KujakuFhirCoreConverter

  @Before
  fun setUp() {
    kujakuConverter = KujakuFhirCoreConverter()
  }

  @Test
  fun testCheckConversionGuideShouldInitializePrivateConversionGuide() {
    Assert.assertNull(ReflectionHelpers.getField(kujakuConverter, "conversionGuide"))

    Assert.assertNotNull(
      kujakuConverter.checkConversionGuide(ApplicationProvider.getApplicationContext()),
    )
  }

  @Test
  fun testFetchConversionGuide() {
    Assert.assertNull(ReflectionHelpers.getField(kujakuConverter, "conversionGuide"))

    kujakuConverter.checkConversionGuide(ApplicationProvider.getApplicationContext())

    val conversionGuide =
      ReflectionHelpers.getField<HashMap<String, LinkedList<Pair<String, String>>>>(
        kujakuConverter,
        "conversionGuide",
      )

    Assert.assertEquals("name", conversionGuide.get("Group")!!.get(0).first)
    Assert.assertEquals("Group.name", conversionGuide.get("Group")!!.get(0).second)
    Assert.assertEquals("family-id", conversionGuide.get("Group")!!.get(1).first)
    Assert.assertEquals("Group.id", conversionGuide.get("Group")!!.get(1).second)
  }

  @Test
  fun testGenerateFeatureCollectionFromLocationWithBoundaryGeojsonExt() {
    val locationJson =
      """{"resourceType":"Location","id":"136702","meta":{"versionId":"3","lastUpdated":"2022-07-28T18:21:39.739+00:00","source":"#18c074df71ca7366"},"status":"active","name":"Kenyatta Hospital Visitors Parking","description":"Parking Lobby","telecom":[{"system":"phone","value":"020 2726300"},{"system":"phone","value":"(+254)0709854000"},{"system":"phone","value":"(+254)0730643000"},{"system":"email","value":"knhadmin@knh.or.ke"}],"address":{"line":["P.O. Box 20723"],"city":"Nairobi","postalCode":"00202","country":"Kenya"},"physicalType":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/location-physical-type","code":"area","display":"Area"}]},"position":{"longitude":36.80826008319855,"latitude":-1.301070677485388},"managingOrganization":{"reference":"Organization/400"},"partOf":{"reference":"Location/136710"}}"""
    val groupJson =
      """{"resourceType":"Group","id":"1122f50c-5499-4eaa-bd53-a5364371a2ba","meta":{"versionId":"5","lastUpdated":"2022-06-23T14:55:37.217+00:00","source":"#75f9db2107ef0977"},"identifier":[{"use":"official","value":"124"},{"use":"secondary","value":"c90cd5e3-a1c4-4040-9745-433aea9fe174"}],"active":true,"type":"person","code":{"coding":[{"system":"https://www.snomed.org","code":"35359004","display":"Family"}]},"name":"new family","managingEntity":{"reference":"Organization/105"},"member":[{"entity":{"reference":"Patient/7d84a2d0-8706-485a-85f5-8313f16bafa1"}},{"entity":{"reference":"Patient/0beaa1e3-64a9-436f-91af-36cbdaff5628"}},{"entity":{"reference":"Patient/a9e466a6-6237-46e0-bcda-c66036414aed"}},{"entity":{"reference":"Patient/7e62cc99-d992-484c-ace8-a43dba87ed22"}},{"entity":{"reference":"Patient/cd1c9616-bdfd-4947-907a-5f08e2bcd8a9"}}]}"""
    val location =
      FhirContext.forR4Cached().newJsonParser().parseResource(Location::class.java, locationJson)
    val group =
      FhirContext.forR4Cached().newJsonParser().parseResource(Group::class.java, groupJson)

    val resourceGroups: ArrayList<List<Resource>> = ArrayList()
    resourceGroups.add(
      ArrayList<Resource>().apply {
        add(
          Group().apply {
            name = "John Doe Fam"
            id = "90230823"
          },
        )
        add(
          Location().apply {
            position = Location.LocationPositionComponent(DecimalType(1.2), DecimalType(3.5))
            extension =
              listOf(
                Extension(KujakuFhirCoreConverter.BOUNDARY_GEOJSON_EXT_URL).apply {
                  setValue(
                    Attachment().apply {
                      contentType = "application/geo+json"
                      data =
                        Base64.encodeBase64(
                          """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[12.83203125,28.304380682962783]}}"""
                            .encodeToByteArray(),
                        )
                    },
                  )
                },
              )
          },
        )
      },
    )
    resourceGroups.add(listOf(group, location))

    val featureCollection =
      kujakuConverter.generateFeatureCollection(
        ApplicationProvider.getApplicationContext(),
        resourceGroups,
      )

    Assert.assertEquals("John Doe Fam", featureCollection.features()!![0].getStringProperty("name"))
    Assert.assertEquals(
      "90230823",
      featureCollection.features()!![0].getStringProperty("family-id"),
    )
    Assert.assertEquals(
      28.304380682962783,
      (featureCollection.features()!![0].geometry() as Point).latitude(),
      0.0,
    )
    Assert.assertEquals(
      12.83203125,
      (featureCollection.features()!![0].geometry() as Point).longitude(),
      0.0,
    )

    Assert.assertEquals("new family", featureCollection.features()!![1].getStringProperty("name"))
    Assert.assertEquals(
      "1122f50c-5499-4eaa-bd53-a5364371a2ba",
      featureCollection.features()!![1].getStringProperty("family-id"),
    )
    Assert.assertEquals(
      -1.301070677485388,
      (featureCollection.features()!![1].geometry() as Point).latitude(),
      0.0,
    )
    Assert.assertEquals(
      36.80826008319855,
      (featureCollection.features()!![1].geometry() as Point).longitude(),
      0.0,
    )
  }
}
