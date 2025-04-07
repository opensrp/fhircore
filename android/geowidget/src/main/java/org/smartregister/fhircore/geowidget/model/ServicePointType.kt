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

package org.smartregister.fhircore.geowidget.model

import androidx.annotation.DrawableRes
import org.smartregister.fhircore.engine.R

enum class ServicePointType(
  @param:DrawableRes var drawableId: Int,
  var text: String,
) {
  CSB1(R.drawable.ic_csb_service_point, "csb1"),
  CSB2(R.drawable.ic_csb_service_point, "csb2"),
  CENTRE_DE_SANTE(R.drawable.ic_csb_service_point, "Centre de Santé"),
  CENTRE_DE_TRAITEMENT_DU_CHOLERA(
    R.drawable.ic_csb_service_point,
    "Centre de Traitement du Choléra",
  ),
  HOPITAL_COMMUNAL(R.drawable.ic_csb_service_point, "Hôpital Communal"),
  HOPITAL(R.drawable.ic_csb_service_point, "Hôpital"),
  CHRD1(R.drawable.ic_hospital, "chrd1"),
  CHRD2(R.drawable.ic_hospital, "chrd2"),
  CHRR(R.drawable.ic_hospital, "chrr"),
  SDSP(R.drawable.ic_gov, "sdsp"),
  DRSP(R.drawable.ic_gov, "drsp"),
  MSP(R.drawable.ic_gov, "msp"),
  EPP(R.drawable.ic_epp_service_point, "epp"),
  CEG(R.drawable.ic_epp_service_point, "ceg"),
  WAREHOUSE(R.drawable.ic_warehouse, "Warehouse"),
  WATER_POINT(R.drawable.ic_water_point, "Water Point"),
  PRESCO(R.drawable.ic_epp_service_point, "presco"),
  MEAH(R.drawable.ic_water_point, "meah"),
  DREAH(R.drawable.ic_water_point, "dreah"),
  MPPSPF(R.drawable.ic_men_service_point, "mppspf"),
  DRPPSPF(R.drawable.ic_gov, "drppspf"),
  NGO_PARTNER(R.drawable.ic_ngo_partner, "NGO Partner"),
  SITE_COMMUNAUTAIRE(R.drawable.ic_site_communautaire, "Site Communautaire"),
  DRJS(R.drawable.ic_gov, "drjs"),
  INSTAT(R.drawable.ic_gov, "instat"),
  BSD(R.drawable.ic_gov, "bsd"),
  MEN(R.drawable.ic_men_service_point, "men"),
  BUREAU_DES_PARTENAIRES(R.drawable.ic_men_service_point, "Bureau des partenaires"),
  DIRECTION_COMMUNALE_DE_L_ENSEIGNEMENT(
    R.drawable.ic_men_service_point,
    "Direction Communale de l'Enseignement",
  ),
  DREN(R.drawable.ic_epp_service_point, "dren"),
  DISTRICT_PPSPF(R.drawable.ic_gov, "District PPSPF"),
  MAIRIE(R.drawable.ic_csb_service_point, "Mairie"),
  ECOLE_COMMUNAUTAIRE(R.drawable.ic_epp_service_point, "Ecole Communautaire"),
  ECOLE_PUBLIQUE(R.drawable.ic_epp_service_point, "École Publique"),
  ECOLE_PRIVÉ(R.drawable.ic_epp_service_point, "Ecole Privé"),
  ECOLE_PRIVE(R.drawable.ic_epp_service_point, "École Privée"),
  LYCÉE(R.drawable.ic_epp_service_point, "Lycée"),
}
