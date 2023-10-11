# Setting up Keycloak

## Keycloak auth token configuration

When making API requests, the app uses an access token that represent authorization to access resources on the server.

When the access token expires, the app will attempt to renew it using a refresh token.

The access token lifespan is configured on Keycloak as the `Access Token Lifespan`.

The refresh token lifespan will be equal to the smallest value among (`SSO Session Idle`, `Client Session Idle`, `SSO Session Max`, and `Client Session Max`).

When setting up identity and access management via Keycloak, the access and refresh token values are required to ensure the access token renewal works as expected in the app.

## Configuring Keycloak with fhir-web

- Navigate to the Keycloak Admin UI, e.g `http://keycloak:8080`
- Create realm with name `fhir` or anything else
- In the realm previously created, create new Client Scope "fhir_core_app_id", set it as Default, and add mapper user attribute `fhir_core_app_id`
- Create client for OpenSRP v2.0 client-side application, e.g. `opensrp-v2-app-client`
  - In "Capability config", turn on "Client authentication".
    - When it is ON, the OIDC type is set to confidential access type. When it is OFF, it is set to public access type)
  - Set "Valid redirect URIs" to `*` or other as needed
  - Copy & paste the client secret from "Credentials" tab
- Create client for `fhir-web`
  - In "Capability config", turn on "Client authentication".
  - Set "Valid redirect URIs" to the domain name of your `fhir-web` installation + wildcard as suffix, e.g. `https://fhir-web.example.org/*` or other as needed
  - Set "Valid post logout redirect URIs" to the domain name of your `fhir-web` installation + wildcard as suffix, e.g. `https://fhir-web.example.org/*` or other as needed
  - Set "Web origins" to the domain name of your `fhir-web` installation, e.g. `https://fhir-web.example.org` or other as needed
  - Copy the client secret from the "Credentials" tab, paste it wherever needed
- Create realm roles based on [OpenSRP V2 RBAC ROLES](https://docs.google.com/document/d/1MEw41Rtfdmos9gqqDamQ31_Y58E8Thgo_8i9UXD8ET4/edit)
  - **TODO:** Add script + payload to load all these to the Keycloak instance
- Create the groups "Super Admin" and "Provider"
  - Add roles in the "Role Mapping"
  - Add attribute `fhir_core_app_id` in the group, or in each user

### Extra notes

- Create users via `fhir-web`. This helps by automatically creating the additional required FHIR resources of "[Practitioner](http://hl7.org/fhir/R4/practitioner.html)" and "[PractitionerRole](http://hl7.org/fhir/R4/practitionerrole.html)" for new users / healthcare workers.
