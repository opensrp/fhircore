# Bundling Configurations to Target Specific App Versions

|||
---|---
Date Submitted | March 25, 2024
Date Approved | TBD
Status | In review

## Background

We need to ensure compatibility between FHIR configs downloaded from the server and the version of the OpenSRP 2 app. With OpenSRP still in active development and ongoing changes to how configs are defined, implementing version-based content limitations is crucial to ensuring that the application functions correctly. This allows us to streamline the user experience and maintain consistency across different versions of the application.


## Switch from Composition to ImplementationGuide

OpenSRP currently uses a Composition resource to group the resources that define an OpenSRP 2 application. Storing versioning information in the Composition resource is non-trivial and ad hoc.

An ImplementationGuide (IG) is designed with versioning support and rich metadata such as licensing information, authors, publication status, etc. IGs are the typical wrapper for a set of resources that define a healthcare workflow or system.

An IG has a `useContext` field whose data type is a `UsageContext` that has `valueRange` (Range) as one of the allowed types. Range has `low` and `high` values which we will use define the app versions supported by the configs. It also has a `version` field, we will use this to define the version of the content.

The `useContext.valueRange` defines the lowest and highest APK versions it is compatible with.

The IG’s `definition` field maps to the `section` field of the composition.

* `resource.reference` maps to `section.focus.reference`
* `resource.name` maps to `section.focus.identifier.value`

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

ImplementationGuides are used to package all related resources to manage workflows e.g. immunization IG, malaria IG, HIV IG, etc. To align with how others use IGs, the ideal approach in OpenSRP would be to link all resources referenced in the Composition resource’s section in the implementation guide and fully switch to using an IG instead of a Composition resource

For the first iteration of the switch, an implementation guide will be created and the existing Composition resource referenced in the IG.

## Sequenced Work Plan

1. Add an ImplementationGuide that references the existing composition config
    [GitHub Issue #3150](https://github.com/opensrp/fhircore/issues/3150)

    1. Create an IG with at least the fields below:
        * `url` - globally unique URI for the implementation guide
        * `version` - sequential version number of the config
        * `name` - computer friendly name for the IG
        * `status` - publication status of the IG
        * `packageId` - package name for the IG
        * `fhirVersion` - FHIR version(s) the IG targets
        * `useContext.valueRange` - a range of lowest and highest supported APK version codes. Using the version code over the SEMVER version simplifies filtering by range when fetching from the server
        * `definition.resource` - a reference to the existing Composition resource

    2. Update OpenSRP to support syncing using both IG and Composition resources.
        * For apps that do not have an IG, follow the current sync flow using the Composition config
        * For apps that have an IG configured:
            * Fetch the highest version of the IG config from the server whose useContext range applies for the app’s version.
            * Use the composition config referenced in the IG and follow the standard sync using composition config.
        * In cases where both an IG and a composition config are defined for an app, the IG takes precedence over the composition. The flow in (ii) applies.
        * If both IG and composition resources are not available, the app should fail with a message to the user

    3. Document how to set IG’s version and useContext range values
        * The IG `useContext` value should be a range of the app's supported version codes. Using version codes over the app's semantic version allows us to more easily filter from the server by range.
            * useContext.valueRange.low - minimum supported version code. Skip this value if the support starts from the earliest version
            * useContext.valueRange.high - maximum supported version code. Skip this value if the support starts from low and supports every version above that

            ```
            "useContext": [
              {
                "valueRange": {
                "low": {
                    "value": 1
                },
                "high": {
                    "value": 10
                }
                }
              }
            ]
            ```

        * To ensure there are no missing or improperly referenced configs, and that correct versioning is done, validation will be required in fhir-tooling:
            * FHIR content verification CI tool
            * Additional checks for missing configs, invalid versioning, etc. to be done when uploading using fhir-tooling

2. Display version and useContext info in app
    [GitHub Issue #3151](https://github.com/opensrp/fhircore/issues/3151)
    * Add IG version and useContext values to the application’s settings screen

3. **[TBD, review with PM/TPM/Dev, requires product owner sign-off]** Tag generated content with version of IG. This can be valuable when troubleshooting. Below are some of the considerations to guide the decision on whether to do this
    * Pros
        * Useful for debug purposes - provides crucial information during debugging sessions. It allows developers to quickly identify which version of the IG was used to generate specific content, aiding in diagnosing and resolving issues more efficiently. It is also easy to correlate inconsistencies or errors directly to the version of the IG tagged in the resources
        * Track failure back to version of content - a clear audit trail of content changes and their corresponding IG versions is maintained
        * Tags can be used to identify resources that need to be migrated in case of an issue tied to a specific version of configs
    * Cons
        * Increases data size - introduces additional metadata which can slightly increase the overall data size, albeit minimally.
        * Adds code complexity to do tagging
        * Additional work needs to be done when creating resources to include version information
        * When a resource was created with an earlier version of configs and edit is done with a newer version, the updated resource may need to be upgraded to include fields and other information added (if any) in the newer version

4. **[TBD]** Restrict the ability to sync IG based on useContext within version of app doing the syncing eg. get the latest IG version valid for app version
    * There may be multiple versions of an IG for a given app. How should OpenSRP pick the version of the IG to fetch and use to sync?
        * Select the most recent version, i.e., IG with highest version number for the given app version
    * How should the app handle cases where a valid IG does not exist for the version of the app?
        * The app should provide appropriate feedback to the user, indicating that IG syncing is not available for their current app version. This could be accompanied by instructions on how to update the app to a version that is supported
        * The app could also offer fallback functionality or access to alternative resources if IG syncing is not possible.
    * Do the version filters only apply to configs and not to content generated in the app?
        * Version filters should primarily apply to configs
        * Content generated within the app may not necessarily be restricted by version filters unless it directly interacts with IG-related functionalities. However, it's essential to ensure that any generated content remains compatible with the selected IG version to maintain data integrity and interoperability.

### Questions

1. Why change from a Composition resource to an IG resource?
    * We will have some added LOE to convert from using the Composition to the IG. Do we need to maintain backward compatibility?
        * IGs can be rendered and published - opens up
        * Latest version of the SDK has a KM

        _An IG resource will be created for the versions of the app that they support. Resource.reference field of the IG references the exisiting composition resource._

2. How do we handle bundling of the IG resource using the Workflow manager?
    * An IG is a metadata resource, not a normal resource. It is referenced by a URL rather than an identifier as is the case with composition resources.
    * How will workflow manager discriminate how if processes a content IG vs Workflow IG?
        * The workflow manager should always assume that the IG it is receiving is a workflow IG
    * Also, they are packaged as maven dependencies? TBD
3. IG focuses on bundling workflows as opposed to resources.
    * An app can have multiple IGs that define different workflows.
        * NB: A workflow is a set of resources. For example you can have an antenatal care IG or an Application IG.
4. How does the app know what version of the IG/Composition to load? E.g
    * 0.1.0 APK version uses Composition/IG version 2
    * 2.0.0 - 3.0.5 APK version uses Composition/IG version 4
        * The app points to an IG and the IG points to a composition resource
        * The app should receive the latest IG depending on the range of the version number of the APK.

        _Currently, the composition resource fetched is based on the appId._

        _FHIR spec version R4B does not have an identifier field for IG. This means that we are not able to use the appId to fetch an IG resource. Here are the proposed approaches on how to specify the IG to use:_

            _Configure a URL reference to the IG in the app’s build configurations._

5. Versioning structure to be used
    * Questionnaire/001 => Questionnaire/002 - Use 2 files
    * Questionnaire/001/_history/1 => Questionnaire/001/_history/2 - The same file leverage FHIR version
    * Does your HAPI FHIR server support versioning using __history_

        _As highlighted in bullet (c), as not all FHIR servers support resource versioning, the approach to be applied in this case is use of a different identifier for configs with breaking changes.  _

6. The process of releasing a content IG
    * Any content that changes requires a new identifier
        * Case 1 - Routine, non-breaking changes to configs that do not require an app update
            * Requires no action on the IG
        * Case 2 - Changes made to configs require a specific version of the app to work
            * All updated resources/configs are created with new identifiers
            * A copy of the composition resource is created with a new identifier. References to the changed configs are added to the composition config
            * A copy of the IG is created, with a different URL to the older version. The new IG resource then references the new composition created in (2) above
