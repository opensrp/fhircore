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

package org.smartregister.fhircore.quest.util.extensions

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_REMOTE
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class LoadRemoteImagesTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var registerRepository: RegisterRepository
  private val imageProperties =
    ImageProperties(
      imageConfig =
        ImageConfig(
          type = ICON_TYPE_REMOTE,
          reference = "d60ff460-7671-466a-93f4-c93a2ebf2077",
        ),
    )

  private val profileConfiguration =
    ProfileConfiguration(
      id = "1",
      appId = "a",
      fhirResource =
        FhirResourceConfig(
          baseResource = ResourceConfig(resource = ResourceType.Patient),
          relatedResources =
            listOf(
              ResourceConfig(
                resource = ResourceType.Encounter,
              ),
              ResourceConfig(
                resource = ResourceType.Task,
              ),
            ),
        ),
      views =
        listOf(
          CardViewProperties(
            viewType = ViewType.CARD,
            content =
              listOf(
                ListProperties(
                  viewType = ViewType.LIST,
                  registerCard =
                    RegisterCardConfig(
                      views =
                        listOf(
                          ColumnProperties(
                            viewType = ViewType.COLUMN,
                            children =
                              listOf(
                                RowProperties(
                                  viewType = ViewType.ROW,
                                  children =
                                    listOf(
                                      imageProperties,
                                    ),
                                ),
                              ),
                          ),
                        ),
                    ),
                ),
              ),
          ),
        ),
    )

  private val binaryImage = Faker.buildBinaryResource()

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun testImageBitmapUpdatedCorrectly(): Unit = runBlocking {
    // Create image to be updated
    defaultRepository.create(addResourceTags = true, binaryImage)
    loadImagesRecursively(
      views = profileConfiguration.views,
      computedValuesMap = emptyMap(),
      registerRepository = registerRepository,
    )
    assertNotNull(imageProperties.imageConfig?.decodedBitmap)
  }
}
