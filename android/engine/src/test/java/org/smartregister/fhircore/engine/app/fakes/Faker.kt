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

package org.smartregister.fhircore.engine.app.fakes

import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.mockk
import io.mockk.spyk
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.FhirResourceConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterContentConfig
import org.smartregister.fhircore.engine.configuration.register.ResourceConfig
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceButton
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.configuration.view.ViewGroupProperties
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.toSha1

object Faker {

  private const val APP_DEBUG = "app/debug"

  val authCredentials =
    AuthCredentials(
      username = "demo",
      password = "51r1K4l1".toSha1(),
      sessionToken = "49fad390491a5b547d0f782309b6a5b33f7ac087",
      refreshToken = "USrAgmSf5MJ8N_RLQODa7rZ3zNs1Sj1GkSIsTsb4n-Y"
    )

  fun buildTestConfigurationRegistry(
    defaultRepository: DefaultRepository = mockk()
  ): ConfigurationRegistry {
    val configurationRegistry =
      spyk(
        ConfigurationRegistry(
          fhirResourceDataSource = mockk(),
          sharedPreferencesHelper = mockk(),
          dispatcherProvider = mockk(),
          repository = defaultRepository
        )
      )

    runBlocking {
      configurationRegistry.loadConfigurations(
        appId = APP_DEBUG,
        context = InstrumentationRegistry.getInstrumentation().targetContext
      ) {}
    }

    return configurationRegistry
  }

  fun buildPatient(
    id: String = "sampleId",
    family: String = "Mandela",
    given: String = "Nelson",
    age: Int = 78,
    gender: Enumerations.AdministrativeGender = Enumerations.AdministrativeGender.MALE
  ): Patient {
    return Patient().apply {
      this.id = id
      this.active = true
      this.identifierFirstRep.value = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.gender = gender
      this.birthDate = DateType(Date()).apply { add(Calendar.YEAR, -age) }.dateTimeValue().value

      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }
    }
  }

  fun generatePatientRegisterConfiguration(): RegisterConfiguration {
    return RegisterConfiguration(
      appId = "app",
      configType = "register",
      id = "patientRegister",
      fhirResource =
        FhirResourceConfig(
          baseResource = ResourceConfig(resource = "Patient"),
          relatedResources =
            listOf(ResourceConfig(resource = "Immunization", searchParameter = "patient"))
        ),
      filter =
        RegisterContentConfig(
          visible = true,
          display = "Show overdue",
          rules = listOf(RuleConfig(name = "filter", condition = "", actions = listOf()))
        ),
      searchBar =
        RegisterContentConfig(
          visible = true,
          display = "Search name or ID",
          computedRules = listOf("patientName")
        ),
      registerCard =
        RegisterCardConfig(
          rules =
            listOf(
              RuleConfig(
                name = "patientName",
                condition = "true",
                actions =
                  listOf(
                    "data.put('patientName', fhirPath.extractValue(Patient, 'Patient.name.given') + ' ' + fhirPath.extractValue(Patient, 'Patient.name.family'))"
                  )
              ),
              RuleConfig(
                name = "patientGender",
                condition = "true",
                actions =
                  listOf(
                    "data.put('patientGender', fhirPath.extractValue(Patient, 'Patient.gender'))"
                  )
              )
            ),
          views =
            listOf(
              ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                  listOf(
                    ServiceCardProperties(
                      viewType = ViewType.SERVICE_CARD,
                      details =
                        listOf(
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "@{patientName}",
                            primaryTextColor = "#000000"
                          ),
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "@{patientGender}",
                            primaryTextColor = "#5A5A5A"
                          )
                        ),
                      showVerticalDivider = true,
                      serviceMemberIcons = "PREGNANT_WOMAN,CHILD,CHILD",
                      serviceButton =
                        ServiceButton(
                          visible = true,
                          text = "1",
                          status = "OVERDUE",
                          smallSized = false
                        )
                    )
                  )
              )
            )
        )
    )
  }

  fun generateHouseholdRegisterConfiguration(): RegisterConfiguration {
    return RegisterConfiguration(
      appId = "app",
      configType = "register",
      id = "householdRegister",
      fhirResource =
        FhirResourceConfig(
          baseResource =
            ResourceConfig(
              resource = "Group",
              dataQueries =
                listOf(
                  DataQuery(
                    id = "householdQueryByType",
                    filterType = Enumerations.SearchParamType.TOKEN,
                    key = "type",
                    valueType = Enumerations.DataType.CODING,
                    valueCoding = Code(system = "http://hl7.org/fhir/group-type", code = "person")
                  ),
                  DataQuery(
                    id = "householdQueryByCode",
                    filterType = Enumerations.SearchParamType.TOKEN,
                    key = "code",
                    valueType = Enumerations.DataType.CODEABLECONCEPT,
                    valueCoding = Code(system = "https://www.snomed.org", code = "35359004")
                  ),
                )
            ),
          relatedResources =
            listOf(
              ResourceConfig(
                resource = "Patient",
                fhirPathExpression = "Group.member.entity",
                relatedResources =
                  listOf(
                    ResourceConfig(resource = "Condition", searchParameter = "subject"),
                    ResourceConfig(resource = "CarePlan", searchParameter = "subject")
                  )
              ),
              ResourceConfig(
                resource = "CarePlan",
                searchParameter = "subject",
                dataQueries =
                  listOf(
                    DataQuery(
                      id = "filterHouseholdCarePlans",
                      filterType = Enumerations.SearchParamType.TOKEN,
                      key = "_tag",
                      valueType = Enumerations.DataType.CODING,
                      valueCoding = Code(system = "https://www.snomed.org", code = "35359004")
                    )
                  )
              )
            )
        ),
      filter =
        RegisterContentConfig(
          visible = true,
          display = "Show overdue",
          rules = listOf(RuleConfig(name = "filter", condition = "", actions = listOf()))
        ),
      searchBar =
        RegisterContentConfig(
          visible = true,
          display = "Search name or ID",
          computedRules = listOf("familyName")
        ),
      registerCard =
        RegisterCardConfig(
          rules =
            listOf(
              RuleConfig(
                name = "familyName",
                condition = "true",
                actions =
                  listOf("data.put('familyName', fhirPath.extractValue(Group, 'Group.name'))")
              )
            ),
          views =
            listOf(
              ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                  listOf(
                    ServiceCardProperties(
                      viewType = ViewType.SERVICE_CARD,
                      details =
                        listOf(
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "@{familyName} Family",
                            primaryTextColor = "#000000"
                          ),
                          CompoundTextProperties(
                            viewType = ViewType.COMPOUND_TEXT,
                            primaryText = "Village/Address",
                            primaryTextColor = "#5A5A5A"
                          )
                        ),
                      showVerticalDivider = true,
                      serviceMemberIcons = "PREGNANT_WOMAN,CHILD,CHILD",
                      serviceButton =
                        ServiceButton(
                          visible = true,
                          text = "1",
                          status = "OVERDUE",
                          smallSized = false
                        )
                    )
                  )
              )
            )
        )
    )
  }
}
