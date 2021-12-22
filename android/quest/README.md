Create new register view for an organization as follows
==============================

### Create a User on Keycloak ensuring following
- Attribute questionnaire_publisher (only hyphen or underscore separated)
- The keycloak client and secret which were used when client app was compiled, and user is associated with, has proper mapping to expose the attribute to userinfo (https://stackoverflow.com/a/47681555)

### Create a Questionnaire that registers your patients ensuring following
- Publisher must be set to your pusblisher id
- title and name Must be defined to ensure right name and title is shown
- useContext MUST be defined and should have the coding that uniquely identifies your patients
```
"useContext": {
    "code": [
      {
        "system": "http://hl7.org/fhir/codesystem-usage-context-type.html",
        "code": "focus"
      }
    ],
    "valueCodeableConcept": {
      "coding": {
        "system": "http://fhir.ona.io",
        "code": "P0001",
        "display": "ONA Systems Patient"
      }
    }
  }
```
- The Questionnaire should also have FHIR Path mappings for creating a basic Patient record
- An example can be found here https://github.com/opensrp/fhircore/blob/main/android/quest/src/main/assets/sample_patient_registration.json. Do not forget to replace publisher and useContext coding


### Create your Register View Configuration as a Binary Resource on server as below
- id MUST be of format 'quest-app-patient-register-[your-publisher-id]' example (quest-app-patient-register-ona-systems) where ona-systems is your publisher id set above
- Set the registrationForm id with questionnaire id you got for registration questionnaire created above
- This MUST be a PUT request with id set correctly into request path and the property in body below
```
{
    "id": "quest-app-patient-register-[your-publisher-id]",
    "appTitle": "Clients",
    "filterText": "Show overdue",
    "searchBarHint": "Search for ID or client name",
    "newClientButtonText": "Add new client",
    "newClientButtonStyle": "rounded_corner",
    "showSearchBar": true,
    "showFilter": false,
    "showScanQRCode": false,
    "showNewClientButton": true,
    "showSideMenu": false,
    "showBottomMenu": true,
    "registrationForm": "[your-registration-questionnaire-id]"
  }
```

### Create as many questionnaires as you want to show in profile ensuring that
- Publisher must be set to your pusblisher id
- title and name Must be defined to ensure right name and title is shown
- useContext MUST be defined and should have the coding that is represents your questionnaire business
- If you want to create entities as a result of extraction either use FHIR Path OR use Structure Map
- Make sure to assign pusblisher to Structure Map as well
```
"useContext": {
    "code": [
      {
        "system": "http://hl7.org/fhir/codesystem-usage-context-type.html",
        "code": "focus"
      }
    ],
    "valueCodeableConcept": {
      "coding": {
        "system": "http://fhir.ona.io",
        "code": "T0001",
        "display": "ONA Systems Test Results"
      }
    }
  }
```
  
### Create your Profile View Configuration as below
- id MUST be of format 'quest-app-profile-[your-publisher-id]' example (quest-app-profile-ona-systems) where ona-systems is your publisher id set above
- Set the code and system that matches with Questionnaires you want to show in profile
- This MUST be a PUT request with id set correctly into request path and the property in body below
```
{
  "id": "quest-app-profile-ona-systems",
  "profileQuestionnaireFilter": {
    "key": "context",
    "code": "T0001",
    "system": "http://fhir.ona.io"
  }
}
```

Now open clean installed app, login, and view the customized app (Make sure you were not using same app from some other organization, if so, uninstall and reinstall app, make sure no unsynced data is present before uninstall)
