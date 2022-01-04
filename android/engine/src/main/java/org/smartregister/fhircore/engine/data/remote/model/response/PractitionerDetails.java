package org.smartregister.fhircore.engine.data.remote.model.response;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ResourceType;

@ResourceDef(
    name = "practitioner-details",
    profile = "http://hl7.org/fhir/profiles/custom-resource")
public class PractitionerDetails extends Practitioner {

  @Child(
      name = "KeycloakUserDetails",
      type = {KeycloakUserDetails.class})
  @Description(
      shortDefinition = "Get Keycloak User details from the Keycloak server",
      formalDefinition = "Get Keycloak User details from the Keycloak server")
  private KeycloakUserDetails keycloakUserDetails;

  @Child(
      name = "fhir",
      type = {FhirPractitionerDetails.class})
  @Description(
      shortDefinition = "Get resources from FHIR Server",
      formalDefinition = "Get resources from FHIR Server")
  private FhirPractitionerDetails fhirPractitionerDetails;

  @Override
  public Practitioner copy() {
    Practitioner practitioner = new Practitioner();
    Bundle bundle = new Bundle();
    List<Bundle.BundleEntryComponent> theEntry = new ArrayList<>();
    Bundle.BundleEntryComponent entryComponent = new Bundle.BundleEntryComponent();
    entryComponent.setResource(new Bundle());
    theEntry.add(entryComponent);
    bundle.setEntry(theEntry);
    this.copyValues(practitioner);
    return practitioner;
  }

  @Override
  public ResourceType getResourceType() {
    return ResourceType.Bundle;
  }

  public KeycloakUserDetails getUserDetail() {
    return keycloakUserDetails;
  }

  public void setUserDetail(KeycloakUserDetails keycloakUserDetails) {
    this.keycloakUserDetails = keycloakUserDetails;
  }

  public FhirPractitionerDetails getFhirPractitionerDetails() {
    return fhirPractitionerDetails;
  }

  public void setFhirPractitionerDetails(FhirPractitionerDetails fhirPractitionerDetails) {
    this.fhirPractitionerDetails = fhirPractitionerDetails;
  }
}
