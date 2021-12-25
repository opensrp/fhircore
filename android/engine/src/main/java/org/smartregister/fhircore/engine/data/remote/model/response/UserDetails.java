package org.smartregister.fhircore.engine.data.remote.model.response;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

public class UserDetails extends Resource {

  @Override
  public Resource copy() {
    return null;
  }

  @Override
  public ResourceType getResourceType() {
    return ResourceType.Basic;
  }
}
