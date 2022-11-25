# Testing FHIR Resources

## Step 1 - Creating Patient Resources

### Pre-requisite

1. VS Code
2. Browser
3. Insomnia (https://insomnia.rest/)or Postman (https://www.postman.com/downloads/)

### Exercise

1. Convert the below patient record (unstructured) to a FHIR patient resource

Name: Pablo Emilio Escobar Gaviria
Gender: Male
DOB: 1, December 1949
Phone: 2908409234
Email: pablo@narcos.mx
Address : 213, Rionegro, Medellín, Colombia.

### Setup VSCode to begin FHIR

1. Go to the FHIR website  - https://www.hl7.org/fhir. Everything you need to know about FHIR standard and also the source of truth
2. Download the FHIR JSON Schema here - JSON Schema https://www.hl7.org/fhir/fhir.schema.json.zip
3. Add a File with fhir.schema.json to your project folder
4. Congifgure the workspace settings by clicking ctr+shift+p or cmd+shit+p then type workspace settings, then add the following code

```
{
  "json.schemas": [
      {
          "fileMatch": [
              "*.fhir.json"
          ],
          "url": "./fhir.schema.json"
      }
  ]
}
```

### Creating a patient resource

1. In the project folder, create a resource (patient.fhir.json)
2. Go to the patient resource to learn more about the patient resources  - https://www.hl7.org/fhir/patient.html
3. Start by adding the braces {}
4. Use autocomplete to fill in the resource (patient) attributes - Press ctrl+space and press enter once you get the key for the resource
5. Manually add/type value for the key
7. VSCode will let you know of any errors/validation/type for the key selected in the problems pane

Creating a patient resource extensions & profiles   - https://www.hl7.org/fhir/patient-profiles.html

> NB. The use of extensions should be controlled to avoid duplication or corruption of the file

Also check out https://simplifier.net/ to do it online :)

## Step 2 - Searching

### Pre-requisite

1. HL7 has publicly available servers  - https://wiki.hl7.org/Publicly_Available_FHIR_Servers_for_testing
2. We shall be using  - http://hapi.fhir.org/baseR4 for the API test
3. You can use this GUI to do the same  - http://hapi.fhir.org/resource?encoding=null&pretty=true&resource=Patient (Select the Health Intersection R4 server)
3. We can use Insomnia to test our resources

### Exercise

1. In Insomnia/Postman you can create a FHIR project folder
2. You can use the following operations on the base url : in this case using publicly available HAPI server url
        OPTIONS - Get the capability statement for the FHIR server - http://hapi.fhir.org/baseR4
        GET - get resource or search  - http://hapi.fhir.org/baseR4/Patient/id
        POST - create a resource  - http://hapi.fhir.org/baseR4/Patient
        PUT - Update a resource - http://hapi.fhir.org/baseR4/Patient/id
3. Resources to create
        1. Create a Patient
        2. Create a Patient with extension
        3. Get Patient
        4. Create Patient Observation
        5. Update Patient Observation
        6. Search Patient Observation

### Search Exercise

> NB. Add your patient_id where id is given or value for xxx

We can also perform search examples via URL. More info on params here  - https://www.hl7.org/fhir/searchparameter.html

1. All patient whose name starts with X - http://hapi.fhir.org/baseR4/Patient?name=xxx
2. All patient whose family name starts with X - http://hapi.fhir.org/baseR4/Patient?family=xxx
3. All patient observations by patient name  - http://hapi.fhir.org/baseR4/Observation?subject.name=xxx
4. All patient observations by patient id  - http://hapi.fhir.org/baseR4/Observation?patient=Patient/id
5. All BP observations by code - http://hapi.fhir.org/baseR4/Observation?patient=Patient/id&code=http://lonic.org|15074-8 NB. the BP code used was http://lonic.org|15074-8

## Step 3 - Bundles

We are going to work on the Transaction Bundle  with a conditional update - https://www.hl7.org/fhir/bundle.html

This takes multiple resources to POST concurrently to the FHIR server. It's an all or nothing success scenario.

Example TBC
