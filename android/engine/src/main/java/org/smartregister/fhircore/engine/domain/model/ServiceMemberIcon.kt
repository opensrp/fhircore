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

@file:OptIn(ExperimentalSerializationApi::class)

package org.smartregister.fhircore.engine.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames
import org.smartregister.fhircore.engine.R

@Suppress("EXPLICIT_SERIALIZABLE_IS_REQUIRED")
enum class ServiceMemberIcon(val icon: Int) {
  @JsonNames("child", "Child") CHILD(R.drawable.ic_kids),
  @JsonNames("pregnant_woman", "PregnantWoman") PREGNANT_WOMAN(R.drawable.ic_pregnant),
  @JsonNames("post_partum_mother", "PostPartumMother")
  POST_PARTUM_MOTHER(R.drawable.ic_post_partum_mother),
  @JsonNames("woman_of_reproductive_age", "WomanOfReproductiveAge")
  WOMAN_OF_REPRODUCTIVE_AGE(R.drawable.ic_woman_of_reproductive_age),
  @JsonNames("elderly", "Elderly") ELDERLY(R.drawable.ic_elderly),
  @JsonNames("baby_boy", "BabyBoy") BABY_BOY(R.drawable.ic_baby_boy),
  @JsonNames("baby_girl", "BabyGirl") BABY_GIRL(R.drawable.ic_baby_girl),
  @JsonNames("sick_child", "SickChild") SICK_CHILD(R.drawable.ic_sick_child),
  @JsonNames("men_service_point", "MenServicePoint")
  MEN_SERVICE_POINT(R.drawable.ic_men_service_point),
  @JsonNames("bsd_service_point", "BSDServicePoint")
  BSD_SERVICE_POINT(R.drawable.ic_bsd_service_point),
  @JsonNames("hospital", "Hospital") HOSPITAL(R.drawable.ic_hospital),
  @JsonNames("csb_service_point", "CSBServicePoint")
  CSB_SERVICE_POINT(R.drawable.ic_csb_service_point),
  @JsonNames("epp_service_point", "EPPServicePoint")
  EPP_SERVICE_POINT(R.drawable.ic_epp_service_point),
  @JsonNames("ngo_partner", "NgoPartner") NGO_PARTNER(R.drawable.ic_ngo_partner),
  @JsonNames("site_communautaire", "siteCommunautaire")
  SITE_COMMUNAUTAIRE(R.drawable.ic_site_communautaire),
  @JsonNames("warehouse", "Warehouse") WAREHOUSE(R.drawable.ic_warehouse),
  @JsonNames("water_point", "WaterPoint") WATER_POINT(R.drawable.ic_water_point),
}
