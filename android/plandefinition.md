Plan Definition https://hl7.org/fhir/R4/plandefinition.html
===============

A PlanDefinition is a resource which is used to define an end to end CarePlan for a Patient or Group. The CarePlan can include one or more activities to carry on across the lifecycle of Patient care. The PlanDefinition is usually associated to address a specific workflow e.g. ANC, PNC, Child Vaccination, Household Visits, etc. 

A PlanDefinition is a pre-defined set of actions, often conditional and based on certain decision points (triggers and conditions). Each action defines an activity to be performed often in terms of an ActivityDefinition (via definition and/or transform).

For defining whether the specific action should be taken, the `condition` element can be used to provide an expression that evaluates to true or false to indicate the applicability of the action to the specific context

Applying a PlanDefinition to a specific context yields a CarePlan and Task resources representing the actions that should be performed for given beneficiary. 

The `definition` element specifies an ActivityDefinition which is used to construct a specific resource (Task and CarePlan only for now). Along with definition, a PlanDefinition may also provide a `transform` element for the activity a StructureMap to do the transformation and creation of elements for CarePlan. Another element `dynamicValue` list can also be provided in ActivityDefinition to provide dynamic values for CarePlan using fhirpath.

Below is a detailed description of notable properties of PlanDefinition and its usage in fhircore

### PlanDefinition notable properties:

- resourceType: PlanDefinition
- id: unique identifier of resource, ideally UUID,
- contained: [ ... ] : An array of resources to embed details (ActivityDefinition) about given plan
- name: Human readable name of plan
- title: Title or summary of what plan is about. This is output to generated CarePlan as title
- status: active,
- description: Detailed summary of what plan does. This is output to generated CarePlan as description
- action: [ ... ] : An array action elements to define `condition`, `definition` and `transform` to be applied while generating CarePlan.


### PlanDefinition `action` notable properties:

- priority: high-priority | medium-priority | low-priority
- condition: [ ]
  - kind: only `applicability` is handled for now. This allow to provide details on what aspect of CarePlan should be assessed.
  - expression: The expression which runs on context i.e. the QuestionnaireResponse, Patient, and other generated resources by QuestionnaireResponse extraction
    - language: text/fhirpath can be handled only for now
    - expression: The fhirpath expression that runs on context resources. 
      - $this : Patient or Group on which Plan is running
      - %resource.entry : The Bundle which was generated as a result of extraction of QuestionnaireResponse. QuestionnaireResponse which resulted into this CarePlan generation is also available into this Bundle 
      - example: $this is Patient and %resource.entry.first().resource is Patient and (today() - 60 'months') <= $this.birthDate
     
  - definitionCanonical: The ActivityDefinition `id` in `contained` resources into this PlanDefinition prefixed by `#`
    - example : #careplan-init-activity
    - transform: The StructureMap to run on this action definition to do the resource generation. If this is not provided only `dynamicValue` component of ActivityDefinition is used to modify the resources
      - The values available in to be used StructureMap are
        - definition: Current ActivityDefinition
        - depends-on: The context resource QuestionnaireResponse which resulted this PlanDefintion
        - subject : The Patient or Group for which this Plan is running
        - period: The evaluated `executionPeriod` for ActivityDefinition
        - version: The index of Task evaluated by ActivityDefinition `timing` or 1 by default
      - example: https://fhir.labs.smartregister.org/fhir/StructureMap/12345

### PlanDefinition `definition` notable properties:

- resourceType: ActivityDefinition
- id: the unique id of resource into contained resource of this PlanDefinition.
- title: title or summary of this definition
- status: active
- description: Detailed summary of what activity does
- kind: `CarePlan` | `Task` supported for now
- dynamicValue: [] an array of fhirpath expression which defines the value for each property of resource of given kind
  - path: the name of property to set the value
    - example: title | status | subject.reference | period.start etc for CarePlan
  - expression: The expression for retrieveing the value from context resources i.e. the QuestionnaireResponse, Patient, and other generated resources by QuestionnaireResponse extraction
    - language: text/fhirpath supported for now
    - expression: The fhirpath expression that runs on context resources.
      - %rootResource : Current PlanDefinition
      - $this : Patient or Group on which Plan is running
      - %resource.entry : The Bundle which was generated as a result of extraction of QuestionnaireResponse. QuestionnaireResponse which resulted into this CarePlan generation is also available into this Bundle
      - example: 
        - %rootResource.id.replaceMatches('/_history/.*', '')
        - $this.generalPractitioner.first()
        - %resource.entry.where(resource is QuestionnaireResponse).resource.descendants().where(linkId='lmp').answer.value
- code : CodeableConcept for standard code for this activity. 
- productCodeableConcept: The CodeableConcept for product for which current activity is running. For Medication or Immunization this is helpful to run ActivityDefinition for specific type of Drug
- dosage: The dosages applicable for this `product`. This defines the `timing` for each dose of given product.
  - timing: The timing for given dose
  - sequence : The sequence of dose 
- timing: If dosage is define this can not be use. This is an alternate way of defining timing for Task. The timing can define a complete schedule
  - repeat: The repeat schedule
    - count: Number of Tasks to be generated. If count is defined the `executionPeriod` timing reference is Careplan.period.start. Otherwise if missing the reference of this timing is today
    duration: The duration of Task to be active for with reference to `executionPeriod.start`
    durationUnit: Duration time unit. mo | d | wk | y etc
    period: The `executionPeriod.start` reference based of current task index. If count is defined the `executionPeriod` timing reference is Careplan.period.start. Otherwise if missing the reference of this timing is today
    periodUnit: Period time unit. mo | d | wk | y etc
  example: 
    - ANC Plan Definition for all at once https://github.com/opensrp/fhircore/pull/2046/files?short_path=672e403#diff-e9cde65b67b65483ab5fead61bf848f945f267cb23cfd5fd4663e9e39f1c8712
    - ANC Plan Definition on demand https://github.com/opensrp/fhircore/pull/2046/files?short_path=672e403#diff-772c2cc9dd9b08326f6d7ded2c462341516d9f1b5f2f3ab674b5437c76621471
    - Child Immunization schedule PlanDefinition https://github.com/opensrp/fhircore/pull/2046/files?short_path=672e403#diff-b08442399d4e5cc5beb14aa05300db7de363095ea17f3800a5510a8d960e0d3a