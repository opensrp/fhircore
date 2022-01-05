package org.smartregister.fhircore.engine.data.remote.model.response;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.CareTeam;
import org.hl7.fhir.r4.model.Bundle;

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

@DatatypeDef(name = "FhirCareTeamExtension")
public class FhirCareTeamExtension extends CareTeam {

  @Override
  public CareTeam copy() {
    CareTeam careTeam = new CareTeam();
    Bundle bundle = new Bundle();
    List<Bundle.BundleEntryComponent> theEntry = new ArrayList<>();
    Bundle.BundleEntryComponent entryComponent = new Bundle.BundleEntryComponent();
    entryComponent.setResource(new Bundle());
    theEntry.add(entryComponent);
    bundle.setEntry(theEntry);
    this.copyValues(careTeam);
    return careTeam;
  }

  public FhirCareTeamExtension mapValues(CareTeam careTeam) {
    FhirCareTeamExtension fhirCareTeamExtension = new FhirCareTeamExtension();
    if (careTeam != null) {
      if (careTeam.getId() != null) {
        fhirCareTeamExtension.setId(careTeam.getId());
      }
      if (careTeam.getIdentifier() != null) {
        fhirCareTeamExtension.setIdentifier(careTeam.getIdentifier());
      }
      if (careTeam.getStatus() != null) {
        fhirCareTeamExtension.setStatus(careTeam.getStatus());
      }
      if (careTeam.getCategory() != null) {
        fhirCareTeamExtension.setCategory(careTeam.getCategory());
      }
      if (careTeam.getName() != null) {
        fhirCareTeamExtension.setName(careTeam.getName());
      }
      if (careTeam.getSubject() != null) {
        fhirCareTeamExtension.setSubject(careTeam.getSubject());
      }
      if (careTeam.getEncounter() != null) {
        fhirCareTeamExtension.setEncounter(careTeam.getEncounter());
      }
      if (careTeam.getPeriod() != null) {
        fhirCareTeamExtension.setPeriod(careTeam.getPeriod());
      }
      if (careTeam.getParticipant() != null) {
        fhirCareTeamExtension.setParticipant(careTeam.getParticipant());
      }

      if (careTeam.getReasonCode() != null) {
        fhirCareTeamExtension.setReasonCode(careTeam.getReasonCode());
      }
      if (careTeam.getReasonReference() != null) {
        fhirCareTeamExtension.setReasonReference(careTeam.getReasonReference());
      }
      if (careTeam.getManagingOrganization() != null) {
        fhirCareTeamExtension.setManagingOrganization(careTeam.getManagingOrganization());
      }
      if (careTeam.getTelecom() != null) {
        fhirCareTeamExtension.setTelecom(careTeam.getTelecom());
      }
      if (careTeam.getNote() != null) {
        fhirCareTeamExtension.setNote(careTeam.getNote());
      }
    }

    return fhirCareTeamExtension;
  }
}

