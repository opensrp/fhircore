package org.smartregister.fhircore.engine.data.remote.model.response;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Organization;
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

@DatatypeDef(name = "FhirOrganizationExtension")
public class FhirOrganizationExtension extends Organization {

  @Override
  public Organization copy() {
    Organization organization = new Organization();
    Bundle bundle = new Bundle();
    List<Bundle.BundleEntryComponent> theEntry = new ArrayList<>();
    Bundle.BundleEntryComponent entryComponent = new Bundle.BundleEntryComponent();
    entryComponent.setResource(new Bundle());
    theEntry.add(entryComponent);
    bundle.setEntry(theEntry);
    this.copyValues(organization);
    return organization;
  }

  public FhirOrganizationExtension mapValues(Organization organization) {
    FhirOrganizationExtension fhirOrganizationExtension = new FhirOrganizationExtension();
    if (organization != null) {
      if (organization.getId() != null) {
        fhirOrganizationExtension.setId(organization.getId());
      }
      if (organization.getIdentifier() != null) {
        fhirOrganizationExtension.setIdentifier(organization.getIdentifier());
      }

      fhirOrganizationExtension.setActive(organization.getActive());
      if (organization.getType() != null) {
        fhirOrganizationExtension.setType(organization.getType());
      }
      if (organization.getName() != null) {
        fhirOrganizationExtension.setName(organization.getName());
      }
      if (organization.getAlias() != null) {
        fhirOrganizationExtension.setAlias(organization.getAlias());
      }
      if (organization.getTelecom() != null) {
        fhirOrganizationExtension.setTelecom(organization.getTelecom());
      }
      if (organization.getAddress() != null) {
        fhirOrganizationExtension.setAddress(organization.getAddress());
      }
      if (organization.getPartOf() != null) {
        fhirOrganizationExtension.setPartOf(organization.getPartOf());
      }
      if (organization.getContact() != null) {
        fhirOrganizationExtension.setContact(organization.getContact());
      }

      if (organization.getEndpoint() != null) {
        fhirOrganizationExtension.setEndpoint(organization.getEndpoint());
      }
    }

    return fhirOrganizationExtension;
  }
}
