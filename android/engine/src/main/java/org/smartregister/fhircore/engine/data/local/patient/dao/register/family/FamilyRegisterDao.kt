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

package org.smartregister.fhircore.engine.data.local.patient.dao.register.family

import com.google.android.fhir.FhirEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.idPart
import org.smartregister.fhircore.engine.util.helper.FhirMapperServices.parseProfileMapping
import org.smartregister.fhircore.engine.util.helper.FhirMapperServices.parseRegisterMapping

@Singleton
class FamilyRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider,
  val fhirPathEngine: FHIRPathEngine
) : RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> =
    getRegisterDataFilters()!!.let { familyParam ->
      defaultRepository.loadRegisterListData(familyParam, null, null, currentPage).map { family ->
        // foreach load members and do mapping

        val familyRegisterData =
          parseRegisterMapping(familyParam.name, family, configurationRegistry, fhirPathEngine) as
            RegisterData.FamilyRegisterData

        // handle all referenced params
        familyParam.part
          .filter { it.value.fhirType() == Enumerations.FHIRAllTypes.ID.toCode() }
          .map { idParamRef ->
            val partParam =
              configurationRegistry.retrieveRegisterDataFilterConfiguration(
                idParamRef.castToId(idParamRef.value).value
              )!!

            defaultRepository.loadRegisterListData(partParam, family.main.second, null).apply {
              this
                .map {
                  parseRegisterMapping(partParam.name, it, configurationRegistry, fhirPathEngine)
                }
                .apply { familyRegisterData.addCollectionData(idParamRef.name, this) }
            }
          }

        familyRegisterData
      }
    }

  override suspend fun loadProfileData(appFeatureName: String?, id: String): ProfileData {
    return getProfileDataFilters()!!.let { mainParam ->
      defaultRepository.loadProfileData(mainParam, null, StringType(id.idPart())).first().let { main
        ->
        val mainProfile =
          parseProfileMapping(mainParam.name, main, configurationRegistry, fhirPathEngine)

        mainParam.part
          .filter { it.value.fhirType() == Enumerations.FHIRAllTypes.ID.toCode() }
          .map { idParamRef ->
            val partParam =
              configurationRegistry.retrieveRegisterDataFilterConfiguration(
                idParamRef.castToId(idParamRef.value).value
              )!!

            defaultRepository.loadProfileData(partParam, main.main.second, null).apply {
              this
                .map {
                  parseProfileMapping(partParam.name, it, configurationRegistry, fhirPathEngine)
                }
                .apply { mainProfile.addCollectionData(idParamRef.name, this) }
            }
          }

        mainProfile
      }
    }
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long =
    withContext(dispatcherProvider.default()) {
      getRegisterDataFilters()?.let { defaultRepository.countDataForParam(it) } ?: 0
    }

  suspend fun limit(loadAll: Boolean, appFeatureName: String?): Int {
    return if (loadAll) countRegisterData(appFeatureName).toInt()
    else PaginationConstant.DEFAULT_PAGE_SIZE
  }

  private fun getRegisterDataFilters() =
    configurationRegistry.retrieveRegisterDataFilterConfiguration(HealthModule.FAMILY.name)

  private fun getProfileDataFilters() =
    configurationRegistry.retrieveProfileDataFilterConfiguration(HealthModule.FAMILY.name)
}
