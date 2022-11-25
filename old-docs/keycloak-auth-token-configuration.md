## **Keycloak Auth Token Configuration**

### Access token and refresh token lifespans
When making API requests, the app uses an access token that represent authorizaton to access resources on the server. 

When the access token expires, the app will attempt to renew it using a refresh token. 

The access token lifespan is configured on Keycloak as the `Access Token Lifespan`. 

The refresh token lifespan will be equal to the smallest value among (`SSO Session Idle`, `Client Session Idle`, `SSO Session Max`, and `Client Session Max`).

When setting up identity and access managment via Keycloak, the access and refresh token values are required to ensure the access token renewal works as expected in the app. 


