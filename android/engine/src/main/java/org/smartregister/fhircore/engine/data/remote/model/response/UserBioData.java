package org.smartregister.fhircore.engine.data.remote.model.response;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.util.ElementUtil;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

import java.io.Serializable;

@DatatypeDef(name = "user-bio")
public class UserBioData extends Type implements ICompositeType, Serializable {

  @Child(
      name = "identifier",
      type = {StringType.class},
      order = 0,
      min = 1,
      max = 1,
      modifier = false,
      summary = false)
  protected StringType identifier;

  @Child(
      name = "userName",
      type = {StringType.class},
      order = 1,
      min = 1,
      max = 1,
      modifier = false,
      summary = false)
  protected StringType userName;

  @Child(
      name = "preferredName",
      type = {StringType.class},
      order = 2,
      min = 1,
      max = 1,
      modifier = false,
      summary = false)
  protected StringType preferredName;

  @Child(
      name = "familyName",
      type = {StringType.class},
      order = 3,
      min = 1,
      max = 1,
      modifier = false,
      summary = false)
  protected StringType familyName;

  @Child(
      name = "givenName",
      type = {StringType.class},
      order = 4,
      min = 1,
      max = 1,
      modifier = false,
      summary = false)
  protected StringType givenName;

  @Child(
      name = "email",
      type = {StringType.class},
      order = 5,
      min = 1,
      max = 1,
      modifier = false,
      summary = false)
  protected StringType email;

  @Child(
      name = "emailVerified",
      type = {StringType.class},
      order = 6,
      min = 1,
      max = 1,
      modifier = false,
      summary = false)
  protected StringType emailVerified;

  public StringType getIdentifier() {
    return identifier;
  }

  public void setIdentifier(StringType identifier) {
    this.identifier = identifier;
  }

  public StringType getUserName() {
    return userName;
  }

  public void setUserName(StringType userName) {
    this.userName = userName;
  }

  public StringType getPreferredName() {
    return preferredName;
  }

  public void setPreferredName(StringType preferredName) {
    this.preferredName = preferredName;
  }

  public StringType getFamilyName() {
    return familyName;
  }

  public void setFamilyName(StringType familyName) {
    this.familyName = familyName;
  }

  public StringType getGivenName() {
    return givenName;
  }

  public void setGivenName(StringType givenName) {
    this.givenName = givenName;
  }

  public StringType getEmail() {
    return email;
  }

  public void setEmail(StringType email) {
    this.email = email;
  }

  public StringType getEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(StringType emailVerified) {
    this.emailVerified = emailVerified;
  }

  @Override
  public Type copy() {
    UserBioData userBioData = new UserBioData();
    copyValues(userBioData);
    return userBioData;
  }

  @Override
  public boolean isEmpty() {
    return ElementUtil.isEmpty(identifier);
  }

  @Override
  protected Type typedCopy() {
    return copy();
  }
}