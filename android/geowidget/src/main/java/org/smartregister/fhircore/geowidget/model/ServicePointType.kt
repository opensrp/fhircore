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
  MAIRIE(R.drawable.ic_csb_service_point, "Mairie"),
  CHRD1(R.drawable.ic_hospital, "chrd1"),
  CHRD2(R.drawable.ic_hospital, "chrd2"),
  CHRR(R.drawable.ic_hospital, "chrr"),
  HOPITAL_COMMUNAUTAIRE(R.drawable.ic_hospital, "Hôpital Communautaire"),
  HOPITAL_DE_DISTRICT(R.drawable.ic_hospital, "Hôpital de district"),
  SDSP(R.drawable.ic_gov, "sdsp"),
  DRSP(R.drawable.ic_gov, "drsp"),
  MSP(R.drawable.ic_gov, "msp"),
  DRPPSPF(R.drawable.ic_gov, "drppspf"),
  DISTRICT_PPSPF(R.drawable.ic_gov, "District PPSPF"),
  DRJS(R.drawable.ic_gov, "drjs"),
  INSTAT(R.drawable.ic_gov, "instat"),
  BSD(R.drawable.ic_gov, "bsd"),
  SITE_DES_REGUFIES(R.drawable.ic_gov, "Site des régufiés"),
  WAREHOUSE(R.drawable.ic_warehouse, "Warehouse"),
  ENTREPOT(R.drawable.ic_warehouse, "Entrepôt"),
  WATER_POINT(R.drawable.ic_water_point, "Water Point"),
  MEAH(R.drawable.ic_water_point, "meah"),
  DREAH(R.drawable.ic_water_point, "dreah"),
  NGO_PARTNER(R.drawable.ic_ngo_partner, "NGO Partner"),
  FARN_FAN(R.drawable.ic_ngo_partner, "FARN/FAN"),
  SITES_DES_DEPLACES(R.drawable.ic_ngo_partner, "Sites des déplacés"),
  CHOLERA_HOT_SPOT(R.drawable.ic_ngo_partner, "Choléra Hot-spot"),
  SITE_COMMUNAUTAIRE(R.drawable.ic_site_communautaire, "Site Communautaire"),
  MEN(R.drawable.ic_men_service_point, "men"),
  BUREAU_DES_PARTENAIRES(R.drawable.ic_men_service_point, "Bureau des partenaires"),
  DIRECTION_COMMUNALE_DE_L_ENSEIGNEMENT(
    R.drawable.ic_men_service_point,
    "Direction Communale de l'Enseignement",
  ),
  MPPSPF(R.drawable.ic_men_service_point, "mppspf"),
  BUREAU_DE_DISTRICT_SANITAIRE(R.drawable.ic_men_service_point, "Bureau de district sanitaire"),
  DREN(R.drawable.ic_epp_service_point, "dren"),
  ECOLE_COMMUNAUTAIRE(R.drawable.ic_epp_service_point, "Ecole Communautaire"),
  ECOLE_PUBLIQUE(R.drawable.ic_epp_service_point, "École Publique"),
  ECOLE_PRIVÉ(R.drawable.ic_epp_service_point, "Ecole Privé"),
  ECOLE_PRIVE(R.drawable.ic_epp_service_point, "École Privée"),
  LYCÉE(R.drawable.ic_epp_service_point, "Lycée"),
  EPP(R.drawable.ic_epp_service_point, "epp"),
  CEG(R.drawable.ic_epp_service_point, "ceg"),
  PRESCO(R.drawable.ic_epp_service_point, "presco"),
  SALLE_DE_CLASSE(R.drawable.ic_epp_service_point, "Salle de classe"),
  SERVICE_COMMUNAUTAIRE(R.drawable.ic_epp_service_point, "Service communautaire"),
  HOME(R.drawable.ic_home, "home"),
  VISITED(R.drawable.ic_done, "visited"),
  REJECTED(R.drawable.ic_reject, "rejected"),
}
