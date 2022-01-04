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

@DatatypeDef(name = "KeycloakUserDetails")
public class KeycloakUserDetails extends Type implements ICompositeType {
  @Child(
      name = "user-bio",
      type = {StringType.class},
      order = 0,
      min = 0,
      max = -1,
      modifier = false,
      summary = false)
  private UserBioData userBioData;

  @Child(
      name = "user-roles",
      type = {StringType.class},
      order = 1,
      min = 0,
      max = -1,
      modifier = false,
      summary = false)
  private List<StringType> roles;

  public UserBioData getUserBioData() {
    return userBioData;
  }

  public void setUserBioData(UserBioData userBioData) {
    this.userBioData = userBioData;
  }

  public List<StringType> getRoles() {
    return roles;
  }

  public void setRoles(List<StringType> roles) {
    this.roles = roles;
  }

  @Override
  public Type copy() {
    KeycloakUserDetails keycloakUserDetails = new KeycloakUserDetails();
    copyValues(keycloakUserDetails);
    return keycloakUserDetails;
  }

  @Override
  public boolean isEmpty() {
    return ElementUtil.isEmpty(userBioData);
  }

  @Override
  protected Type typedCopy() {
    return copy();
  }
}
