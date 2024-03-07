# Bundling Configurations to Target Specific App Versions

|||
---|---
Date Submitted | March 7, 2024
Date Approved | TBD
Status | In review

## Background

There is a need to ensure compatibility between FHIR configs downloaded from the server and the version of OpenSRP app. With OpenSRP still in active development, with ongoing changes to how configs are defined, implementing version-based content limitations is crucial to ensuring that the application functions correctly. This allows for streamlining of user experience and maintaining consistency across different versions of the application.

## Switch from Composition to ImplementationGuide

OpenSRP currently uses a composition resource to define resources that map out a OpenSRP application. Storing versioning information in the composition resource is non-trivial.

An ImplementationGuide (IG) is designed with versioning support and rich metadata such as licensing information, authors, publication status, etc.

An IG has a `useContext` field whose data type is a `UsageContext` that has `Range` as one of the allowed types. Range has `low` and `high` values which can be used to set the app’s version range supported by the configs. It also has a `version` field of the content.

The `useContext.valueRange` defines the lowest and highest APK versions it is compatible with.

IG’s `definition` field maps to `section` field of the composition.

* `resource.reference` maps to `section.focus.reference`
* `resource.name` maps to `section.focus.indentifier.value`

ImplementationGuide

```
  - version
  - useContext (valueRange)
    - low
    - high
  - definition
    - resource
      - reference
      - name
```

ImplementationGuides are used to package all related resources to manage workflows e.g. immunization IG, malaria IG, HIV IG, etc. To align with how others use IGs, the ideal approach inOpenSRP would be to link all resources referenced in the composition config’s section in the implementation guide and fully switch to using an IG instead of a composition resource. 

For the first iteration of the switch, an implementation guide will be created and the existing composition config referenced in the IG.

## Sequenced Work Plan

1. Add an ImplementationGuide that references the existing composition config
    * Create an IG with at least the fields below:
        1. `version` - sequential version number of the config
        2. `useContext.valueRange` - a range of lowest and highest supported APK versions of the app
        3. `definition.resource` - a reference to the existing composition resource
    * Update OpenSRP to support syncing using both IG and composition configs
        4. For apps that do not have an IG, follow the current sync flow using the composition config
        5. For apps that have an IG configured:
            1. Fetch the highest version of the IG config from the server whose useContext range applies for the app’s version.
            2. Use the composition config referenced in the IG and follow the standard sync using composition config.
        6. In cases where both an IG and a composition config are defined for an app, the IG takes precedence over the composition. The flow in (ii) applies.
    * Document how to set IG’s `version`, `useContext`, etc. follow SEMVER etc
        7. To ensure there are no missing or improperly referenced configs, and that correct versioning is done, validation will be required in fhir-tooling:
* FHIR content verification CI tool
* Additional checks for missing configs, invalid versioning, etc. to be done when uploading using fhir-tooling
2. Display version and useContext info in app
    * Add IG version and useContext values to the application’s settings screen
3. **[TBD, review with PM/TPM/Dev, requires product owner sign-off]** Tag generated content with version of IG. This can be valuable when troubleshooting. Below are some of the considerations to guide the decision on whether to do this 
* Pros
    * Useful for debug purposes - provides crucial information during debugging sessions. It allows developers to quickly identify which version of the IG was used to generate specific content, aiding in diagnosing and resolving issues more efficiently. It is also easy to correlate inconsistencies or errors directly to the version of the IG tagged in the resources
    * Track failure back to version of content - a clear audit trail of content changes and their corresponding IG versions is maintained
* Cons
    * Increases data size - introduces additional metadata which can slightly increase the overall data size, albeit minimally.
    * Adds code complexity to do tagging
4. **[TBD]** Restrict the ability to sync IG based on useContext within version of app doing the syncing eg. get the latest IG version valid for app version
    * There may be multiple versions of an IG for a given app. How should OpenSRP pick the version of the IG to fetch and use to sync?
      8. Select the most recent version, i.e., IG with highest version number for the given app version
    * How should the app handle cases where a valid IG does not exist for the version of the app?
        9. The app should provide appropriate feedback to the user, indicating that IG syncing is not available for their current app version. This could be accompanied by instructions on how to update the app to a version that is supported
        10. The app could also offer fallback functionality or access to alternative resources if IG syncing is not possible.
    * Do the version filters only apply to configs and not to content generated in the app?
        11. Version filters should primarily apply to configs
        12. Content generated within the app may not necessarily be restricted by version filters unless it directly interacts with IG-related functionalities. However, it's essential to ensure that any generated content remains compatible with the selected IG version to maintain data integrity and interoperability.
