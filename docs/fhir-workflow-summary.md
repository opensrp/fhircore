_Below is a brief summary of the FHIR R4 docs review_. This is the foundation on FHIR. It helps understand how to design and use resources in a workflow.

## FHIR Resources and Workflow Summary

FHIR categorizes resources in a clinical workflow as:

1. Definition - Resources that define something that can potentially happen in a patient and time-independent manner
2. Request - Resources that ask for or express a desire/intention for something to be done
3. Event - Resources express something that has been done

A clinical workflow starts with a definition of a activities that should occur, their order, circumstances, conditions and their dependencies. This becomes a `request` that needs to be acted on and ends with an `event` that describes what has been done and the output(s).

This [link](https://www.hl7.org/fhir/workflow.html#list) provides a list of FHIR resources grouped into the three categories

### Task

The `Task` is categorized as a `Request` and `Event` . A `Task` keeps track of a request and contains a link to the request. A Task can be generated but not initially assigned to any entity. This task can be picked(self-assigned) and its status can start changing. The implementation details for the Task are contained inside the attached `Request`.

### PlanDefinition

A `PlanDefinition` contains actions. These actions can link to an `ActivityDefinition` which enables definition more properties/details. `ActivityDefinition` can also be more generic and be re-used by multiple PlanDefinitions. `Action` and `ActivityDefintion` share properties and in cases where an `ActivityDefinition` also exists, the properties in the `Action` override the property values in the `ActivityDefinition`. This is useful in cases where an `ActivityDefinition` is generic and shared but specific details such as title and description for the use-case, time, region, module or purpose are included in the `PlanDefinition`. `ActivityDefinition` is used to create a `Request`. The `Request` progress, assignment and status is tracked by a `Task`.

### CarePlan

Running an apply operation on a `Patient`(Context) + `PlanDefinition` generates a `CarePlan` that is specific to a person.

A `CarePlan` :

- Is a group activities to provide context
- Specific to a particular patient or group
- It does not replace a `PlanDefinition`

It uncomplicates some patient, scheduling and patient-specific details/options in care from the `PlanDefinition` to the `CarePlan` . The `CarePlan` will also be brief enough for computational purposes as compared to a `PlanDefinition`. Furthermore, the `CarePlan` contains the final patient care details derived from the `PlanDefinition` and `ActivityDefinition` dependent on the patient details/medical state.

Ideally, there should always be a task for every clinical `Request` that requests fulfillment of the `Request` and tracks progress. The `Request` is referenced in the `Task.focus` and also contains the operational details. Inputs and outputs are also tracked by the task. A Task could request fulfilment of a `ServiceRequest` by ordering a radiology that would end with an `ImagingStudy`. The `ImagingStudy` is the group of the patient's body. The `ImagingStudy` would be the output and can be tracked on the task. Once the images are ready, another `ServiceRequest` would be generated and a `Task` would be used to track the `ImagingStudy` being analysed with the output being a `DiagnosticReport`.

### CareTeam

CareTeam is a group of practitioners, care takers, patients and organizations who plan to participate in the coordination and delivery of care for a patient(group of patients). CareTeam can be used in different contexts where there can be a subject such as a a patient or within context such as emergency services, type of service provided. The `CareTeam.category`, an optional property, describes the type of care and can be one of the following:

- Event-focused care team
- Encounter-focused care team
- Episode of care-focused care team
- Condition-focused care team
- Longitudinal care-coordination focused care team
- Home & Community Based Services (HCBS)-focused care team
- Clinical research-focused care team
- Public health-focused care team
- Longitudinal care-coordination focused care team

In my opinion, CareTeam can be used to limit access to care details within a domain in cases where we have a global patient directory and global patient history in a country eg. HIV details, PHI(Protected Health Information) and also limit access for different healthcare modules  eg. a practitioner might have access to a location and not be part of a CareTeam in that Location. Therefore, this practitioner should not have access to certain records, health modules. Read HIPAA for US on such laws

### Location

Location enables us to define a jurisdiction or HealthCenter [https://www.hl7.org/fhir/location.html](https://www.hl7.org/fhir/location.html) . The GeoJSON is described as a location boundary extension to the location here [http://build.fhir.org/extension-location-boundary-geojson.html](http://build.fhir.org/extension-location-boundary-geojson.html). The extension is a draft with little information but it enables representing the geojson as a base64 string in the `Attachment.data` or the link to the geojson on the `Attachment.url`

Example 1

```bash
"resource": {
                "resourceType": "Location",
                "id": "16260",
                "extension": [
                    {
                        "url": "http://build.fhir.org/extension-location-boundary-geojson.html",
                        "valueAttachment": {
                            "url": "https://sandbox.caredove.com/api/native_v1/Boundary/16907"
                        }
                    }
                ]
            }
```

Origin of example [Brian-Postlethwaite-Healthcare-Directories-DevDays-2019-Redmond.pdf](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/db724112-1589-469e-b4f6-b5165a7581d8/Brian-Postlethwaite-Healthcare-Directories-DevDays-2019-Redmond.pdf)

Example 2

```json
"resource": {
  "resourceType": "Location",
  "name": "Mwango_House",
  "identifier": [
    {
      "system": "biophics",
      "value": "HL0122"
    }
  ],
  "extension": [
    {
      "url": "http://build.fhir.org/extension-location-boundary-geojson.html",
      "valueAttachment": {
        "data": "IHsKICAgICAgICAgICAgImNvb3JkaW5hdGVzIjogWwogICAgICAgICAgICAgICAgOTkuMTgxNzc5NTExNjc3NTgsCiAgICAgICAgICAgICAgICAxNy4wMTA5ODU4OTE5NzU1MDUKICAgICAgICAgICAgXSwKICAgICAgICAgICAgInR5cGUiOiAiUG9pbnQiCiAgICAgICAgfSw="
      }
    }
  ]
}
```
[Origin of example](https://github.com/OpenSRP/fhircore/blob/main/docs/patient-resource/sample-resources/Location.fhir.json)

### Workflow Management Patterns

FHIR Workflow management patterns page tries to provide multiple strategies for managing the workflow in cases where the there might be multiple systems/entities. For example, we might have 2 systems interacting in a case where we have a hospital and external laboratory all running their own FHIR servers. Another example is a hospital and external pharmacy each running it's own FHIR server. 

The workflow between multiple systems is out-of-scope for now.

For FHIR request placers and fillers working within the same system, below is the proposed strategy

![workflow-option](https://user-images.githubusercontent.com/31766075/114738651-d582dd80-9d50-11eb-832c-174ae708a3b8.png)


### Notes
The [workflow module](https://www.hl7.org/fhir/workflow-module.html) declares that R5 will try to simplify the resources and make them more intuitive for implementers. It is noted that some resources have lost domain-specific context and some elements were introduced instead of being defined as extensions.
