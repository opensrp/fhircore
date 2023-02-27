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
    - example: https://fhir.labs.smartregister.org/fhir/StructureMap/12345