# Geowidget configuration

Configurations used to configure map view. FHIR Core uses a mapbox generated UI to position households in a map depending on the location of the household.

:::info There can be multiple instances of this configuration type in the application; each should have a unique `id`. :::

## Sample JSON

```json
{
    "appId": "map",
    "id": "householdRegistrationMap",
    "configType": "geoWidget",
    "profileId": "householdProfile",
    "registrationQuestionnaire": {
        "id": "82952-geowidget",
        "title": "{{add.family}}",
        "saveButtonText": "ADD FAMILY",
        "setPractitionerDetails": true,
        "setOrganizationDetails": true
    }
}
```

## Config properties

| Property                  | Description                                                                                               | Required |   Default   |
| ------------------------- | --------------------------------------------------------------------------------------------------------- | :------: | :---------: |
| appId                     | Unique identifier for the application                                                                     |    Yes   |             |
| configType                | Type of configuration                                                                                     |    Yes   | `geoWidget` |
| id                        | A unique identifier for this multi-config type                                                            |    Yes   |             |
| profileId                 | The identifier for the profile to be opened when a point on the map (representing a household) is clicked |    Yes   |             |
| registrationQuestionnaire | Configuration for the register questionnaire                                                              |    Yes   |             |
