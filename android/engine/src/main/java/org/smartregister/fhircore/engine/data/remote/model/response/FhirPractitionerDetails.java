package org.smartregister.fhircore.engine.data.remote.model.response;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.util.ElementUtil;

import java.util.List;

import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

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

@DatatypeDef(name = "fhir")
public class FhirPractitionerDetails extends Type implements ICompositeType {

  @Child(
      name = "careteams",
      type = {FhirCareTeamExtension.class},
      order = 1,
      min = 0,
      max = -1,
      modifier = false,
      summary = false)
  List<FhirCareTeamExtension> fhirCareTeamExtensionList;

  @Child(
      name = "teams",
      type = {FhirOrganizationExtension.class},
      order = 2,
      min = 0,
      max = -1,
      modifier = false,
      summary = false)
  List<FhirOrganizationExtension> fhirOrganizationExtensions;

  @Child(
      name = "locationHierarchyList",
      type = {LocationHierarchy.class},
      order = 3,
      min = 0,
      max = -1,
      modifier = false,
      summary = false)
  private List<LocationHierarchy> locationHierarchyList;

  @Child(
      name = "practitionerId",
      type = {StringType.class},
      order = 4,
      min = 0,
      max = -1,
      modifier = false,
      summary = false)
  private StringType practitionerId;

  public List<FhirCareTeamExtension> getFhirCareTeamExtensionList() {
    return fhirCareTeamExtensionList;
  }

  public void setFhirCareTeamExtensionList(
      List<FhirCareTeamExtension> fhirCareTeamExtensionList) {
    this.fhirCareTeamExtensionList = fhirCareTeamExtensionList;
  }

  public List<FhirOrganizationExtension> getFhirOrganizationExtensions() {
    return fhirOrganizationExtensions;
  }

  public void setFhirOrganizationExtensions(
      List<FhirOrganizationExtension> fhirOrganizationExtensions) {
    this.fhirOrganizationExtensions = fhirOrganizationExtensions;
  }

  public List<LocationHierarchy> getLocationHierarchyList() {
    return locationHierarchyList;
  }

  public void setLocationHierarchyList(List<LocationHierarchy> locationHierarchyList) {
    this.locationHierarchyList = locationHierarchyList;
  }

  public StringType getPractitionerId() {
    return practitionerId;
  }

  public void setPractitionerId(StringType practitionerId) {
    this.practitionerId = practitionerId;
  }

  @Override
  public Type copy() {
    FhirPractitionerDetails fhirPractitionerDetails = new FhirPractitionerDetails();
    copyValues(fhirPractitionerDetails);
    return fhirPractitionerDetails;
  }

  @Override
  public boolean isEmpty() {
    return ElementUtil.isEmpty(practitionerId);
  }

  @Override
  protected Type typedCopy() {
    return copy();
  }
}
