**Introduction** 

This Documentation on CQL is to give some insights on
1. How to author the CQL script
2. How to load and execute CQL scripts
3. Some sample use cases where we have used CQL 


**What is CQL** 

CQL­ is a Health Level Seven International® (HL7®) authoring language standard that’s intended to be human readable. It is part of the effort to harmonize standards used for electronic clinical quality measures (eCQMs) and clinical decision support (CDS). CQL provides the ability to express logic that is human readable yet structured enough for processing a query electronically. CQL is the expression logic used in Health Quality Measure Format (HQMF) beginning with the eCQMs implemented in calendar year 2019. CQL replaces the logic expressions previously defined in the Quality Data Model (QDM). Beginning with v5.3, QDM includes only the conceptual model for defining the data elements (the data model). Measure authors with access to the Measure Authoring Tool (MAT) can use the tool to author measures using CQL­. Visit the MAT webpage for more information.  

CQL allows for a more modular, flexible, and robust expression of the logic. It allows logic to be shared between measures and with clinical decision support.

**How are we using CQL?**

**Use Case 1 : G6PD**
1. Threshold management 
2. G6PD Test Device Calibration   

**Additional context**
![456](https://user-images.githubusercontent.com/69383347/149340339-10a723d2-707f-4ef8-b18e-ffc638c21ed7.png)

<img width="200" height="400" src ="https://user-images.githubusercontent.com/4829880/150296890-9ee0ef91-3158-4c90-8b4d-10a83e2f3f1d.jpg"/> <img width="200" height="400" src ="https://user-images.githubusercontent.com/4829880/150296898-cfa97ad8-eee4-4cb2-9552-46f67b9765b2.jpg"/> <img width="200" height="400" src ="https://user-images.githubusercontent.com/4829880/150296901-ebc17aa2-b8d4-4695-88a1-d6ec8e6d7ab9.jpg"/>

**Sample G6PD Threshold Evaluation CQL**

````
//Declare name and version of lib
library TestArtifact version '1'

//FHIR as a data model and version of FHIR
using FHIR version '4.0.1'

//functions to help FHIR vs CQL primitives
include "FHIRHelpers" version '4.0.1' called FHIRHelpers

//System declarations  : Codable concept codes systems
codesystem "SNOMED": 'http://snomed.info/sct'
codesystem "ConditionClinicalStatusCodes": 'http://terminology.hl7.org/CodeSystem/condition-clinical'
codesystem "ConditionVerificationStatusCodes": 'http://hl7.org/fhir/ValueSet/condition-ver-status'
codesystem "ServiceReqCategoryCodes": 'http://hl7.org/fhir/ValueSet/servicerequest-category'
codesystem "DiagReportCategoryCodes": 'http://hl7.org/fhir/ValueSet/diagnostic-service-sections'

	
//Code used as identifers
code "Pregnancy code": '77386006' from "SNOMED" display 'Pregnant'
code "G6PD code": '86859003' from "SNOMED" display 'G6PD Enzyme'
code "Haemoglobin code": '259695003' from "SNOMED" display 'Haemoglobin'
code "active": 'active' from "ConditionClinicalStatusCodes"
code "confirmed": 'confirmed' from "ConditionVerificationStatusCodes"
code "g6pdStatusCode": '9024005' from "SNOMED" display 'G6PD Status'
code "normalResultCode": '280413001' from "SNOMED" display 'Normal'
code "deficientResultCode": '260372006' from "SNOMED" display 'Deficient'
code "intermediateResultCode": '11896004' from "SNOMED" display 'Intermediate'
code "serviceReqCategory": '108252007' from "ServiceReqCategoryCodes" display 'Laboratory procedure'
code "diagReportCategory": 'HM' from "DiagReportCategoryCodes" display 'Hematology'



//CQL is written from a perspective of a single patient
//can be  run in a context for a Single patient or population
context Patient

// TODO see if we need to do any more filtering and sorting; rightnow only one encounter is passed
define "Encounter": 
  Last([Encounter] E  
    where E.status ~ 'finished' )

define "AgeRange":
  AgeInYears() >= 16

define "Female":
  Patient.gender = 'female'

define "Male":
  Patient.gender = 'male'

define "is Pregnant":
  Last([Condition: "Pregnancy code"] O
    where O.clinicalStatus ~ "active"
      sort by recordedDate) is not null

define "Registerd Haem":
  Last([Observation: "Haemoglobin code"] H
    where H.status ~ 'registered')

define "Registerd G6PD":
  Last([Observation: "G6PD code"] G
    where G.status ~ 'registered')


define "Hi G6PD":
  Last([Observation: "G6PD code"] H
    where H.value.value > 20.0
        return  true)

      
define "Abnormal Haemoglobin":
  Last([Observation: "Haemoglobin code"] H
    where  H.value.value <= 8.0
        return  true)

define "NA Lo G6PD":
  Last([Observation: "Haemoglobin code"] H
    where  H.value.value < 4.0
        return  true)  

define "NA Hi G6PD":
  Last([Observation: "Haemoglobin code"] H
    where H.value.value > 25.0
        return  true)  
    


//Define Thresholds
define "Deficient":
  Last([Observation: "G6PD code"] G
    where G.value.value <= 3.9
        return true)

define "Intermediate":
  Last([Observation: "G6PD code"] G
    where "Female" and G.value.value >= 4.1  and G.value.value <= 6.0
      return  true)


define "Normal Male":
    Last([Observation: "G6PD code"] G
        where "Male" and G.value.value >= 4.1
            return  true)

define "Normal Female":
    Last([Observation: "G6PD code"] G
        where "Female" and G.value.value >= 6.1
            return  true)

define "g6pdTestResultCode": 
      if "Deficient" then "deficientResultCode"
      else if "Intermediate" then "intermediateResultCode"
      else if "Normal Male" or "Normal Female" then "normalResultCode"
      else null


define "patientRef": ReplaceMatches('Patient/'+Patient.id,'#','')
define "encounterRef": ReplaceMatches('Encounter/'+Encounter.id,'#','')
      
define "condition": if "g6pdTestResultCode" is not null
    then Condition {
      subject: Reference {reference: string { value: "patientRef" } },
      encounter: Reference {reference: string { value: "encounterRef" } },
      clinicalStatus: CodeableConcept { coding: { 
        Coding {
          code: code {value: "active".code },
          system: uri {value: "active".system }
        }  
      } },
      verificationStatus: CodeableConcept { coding: { 
        Coding { 
          code: code {value: "confirmed".code }, 
          system: uri {value: "confirmed".system }
        }
      } },
      category: { CodeableConcept { coding: { 
        Coding {
          code: code {value: "g6pdStatusCode".code },
          system: uri {value: "g6pdStatusCode".system },
          display: string {value: "g6pdStatusCode".display }
        } 
      } } },
      code: CodeableConcept { coding: { 
        Coding {
          code: code {value: "g6pdTestResultCode".code },
          system: uri {value: "g6pdTestResultCode".system },
          display: string {value: "g6pdTestResultCode".display }
        }
      } },
      onset: dateTime{ value: Today() },
      recordedDate: dateTime{ value: Today() }
    } else null
    
define "serviceRequest": if "g6pdTestResultCode" is not null 
   then ServiceRequest{
	  subject: Reference {reference: string { value: "patientRef" } },
      encounter: Reference {reference: string { value: "encounterRef" } },
      status: ServiceRequestStatus { value: 'completed'},
      intent: ServiceRequestIntent {value: 'order' },
      category: { CodeableConcept { coding: { 
        Coding {
          code: code {value: "serviceReqCategory".code },
          system: uri {value: "serviceReqCategory".system },
          display: string {value: "serviceReqCategory".display }
        } 
      } } },
      code: CodeableConcept { coding: { 
        Coding {
          code: code {value: "G6PD code".code },
          system: uri {value: "G6PD code".system },
          display: string {value: "G6PD code".display }
        }
      } },
      authoredOn: dateTime{ value: Today() }
} else null
        
define "diagnosticReport": if "g6pdTestResultCode" is not null
   then DiagnosticReport{
	  subject: Reference {reference: string { value: "patientRef" } },
      encounter: Reference {reference: string { value: "encounterRef" } },
      status: DiagnosticReportStatus { value: 'final'},
      category: { CodeableConcept { coding: { 
        Coding {
          code: code {value: "diagReportCategory".code },
          system: uri {value: "diagReportCategory".system },
          display: string {value: "diagReportCategory".display }
        } 
      } } },
      code: CodeableConcept { coding: { 
        Coding {
          code: code {value: "G6PD code".code },
          system: uri {value: "G6PD code".system },
          display: string {value: "G6PD code".display }
        }
      } },
      effective: dateTime{ value: Today() }
} else null
        
define "OUTPUT": List { "condition", "serviceRequest", "diagnosticReport" }
````


`
{ "resourceType":"MedicationRequest", "status":"active", "intent":"proposal", "subject":{ "reference":"Patient/P1" }, "encounter":{ "reference":"Encounter/E1" }, "authoredOn":"#NOW", "category":[ { "coding":[ { "system":"http://snomed.info/sct", "code":"86859003", "display":"Glucose-6-phosphatedehydrogenasedeficiencyanaemia" } ] } ], "medicationCodeableConcept":{ "coding":[ { "system":"http://snomed.info/sct", "code":"429663004", "display":"Primaquine(substance)" } ] }, "dosageInstruction":[ { "timing":{ "repeat":{ "frequency":1, "period":8.0, "periodUnit":"wk" } }, "route":{"coding":[{"system":"http://snomed.info/sct","code":"26643006","display":"Oraluse"}]}, "doseAndRate":[ { "type":{ "coding":[ { "system":"http://terminology.hl7.org/CodeSystem/dose-rate-type", "code":"ordered", "display":"Ordered" } ] }, "doseQuantity":{ "value":0.75, "unit":"mg/kg", "system":"http://unitsofmeasure.org" } } ] } ] }
`


**Sample G6PD RDT Calibration (Control mode) CQL** 

````
//Declare name and version of lib
library ControlTest version '1'

//FHIR as a data model and version of FHIR
using FHIR version '4.0.1'

//functions to help FHIR vs CQL primitives
include "FHIRHelpers" version '4.0.1' called FHIRHelpers

//System declarations  : Codable concept codes systems
codesystem "SNOMED": 'http://snomed.info/sct'

//Code used as identifers
code "g6pdControl1": '410680006' from "SNOMED" display 'Control-mode 1'
code "g6pdControl2": '405358009' from "SNOMED" display 'Control-mode 2'

define "QR" : Last([QuestionnaireResponse] H)

define "Control1Obs": "QR".item.where(linkId='result_type').answer.value ~ "g6pdControl1"

define "Control2Obs": "QR".item.where(linkId='result_type').answer.value ~  "g6pdControl2"

define "HaemoglobinObs": "QR".item.where(linkId='haemoglobin').answer.first().value

define "G6PDObs": "QR".item.where(linkId='g6pd').answer.first().value

define "G6PDTemp": decimal {value: "G6PDObs".value}
define "G6PDValue": ToString("G6PDTemp")
define "HaemoglobinTemp": decimal {value: "HaemoglobinObs".value}
define "HaemoglobinValue": ToString("HaemoglobinTemp")

define "G6PD Normal":
    if "Control1Obs" is not null and "G6PDObs" between 0.0 and 3.0
    	then true
    else if "Control2Obs" is not null and "G6PDObs" between 6.0 and 10.0
    	then true
    else false

define "G6PD Conclusion":
    if "Control1Obs" is not null and "G6PD Normal"
    	then 'Value ('+ "G6PDValue"+') is in Normal G6PD Range 0-3'
    else if "Control2Obs" is not null and "G6PD Normal"
    	then 'Value ('+ "G6PDValue"+') is in Normal G6PD Range 6-12'
    else 'Value('+"G6PDValue"+') is Non Deterministic G6PD result'

define "Haemoglobin Normal":
    if "Control1Obs" is not null and "HaemoglobinObs" between 8.0 and 12.0
    	then true
    else if "Control2Obs" is not null and "HaemoglobinObs" between 13.0 and 17.0
    	then true
    else false

define "Haemoglobin Conclusion":
    if "Control1Obs" is not null and "Haemoglobin Normal"
    	then 'Value ('+ "HaemoglobinValue"+') is in Normal Haemoglobin Range 8-12'
    else if "Control2Obs" is not null and "Haemoglobin Normal"
    	then 'Value ('+ "HaemoglobinValue"+') is in Normal Haemoglobin Range 13-17'
    else 'Value('+"HaemoglobinValue"+') is Non Deterministic Haemoglobin result'

define "Conclusion":
  if "G6PD Normal" and "Haemoglobin Normal" then 'Correct result'
  else 'Invalid result'

define "Conclusion Details": '\nDetails:\n'+ "G6PD Conclusion" + '\n' + "Haemoglobin Conclusion"


define "OUTPUT": List { "Conclusion" , "Conclusion Details"}
````


**Name of feature to enhance**
As a G6PD app user, I would like to check if the device is properly recalibrated  in either Control Mode 1 or Control Mode 2

**Description of feature**
Add both UI/UX for control mode user journey and leverage current CQL evaluation that can allow us to reuse the current CQL implementation for G6PD evaluation 
Based on a Quest approach added a Questionnaire for test calibration that is independent of the Patient/Clinical workflow.

**Control mode  table** 
<img width="634" alt="Screen Shot 2022-01-13 at 10 19 06 PM" src="https://user-images.githubusercontent.com/4540684/149395158-22a99428-dd1c-4904-a0f2-dd12ffc93945.png">

**Test Kit in normal mode**
<img width="260" alt="Screen Shot 2022-01-13 at 10 23 20 PM" src="https://user-images.githubusercontent.com/4540684/149396047-9c0a51c3-ddd5-4e69-9809-2281396ddac6.png">

**Test kit in control mode** 
<img width="611" alt="Screen Shot 2022-01-13 at 10 25 36 PM" src="https://user-images.githubusercontent.com/4540684/149396063-5b5e8b8d-cf2c-421d-9dfa-e7d3b99a3ae5.png">

More details on testing and control mode can be found here https://www.finddx.org/wp-content/uploads/2020/09/STANDARD-G6PD-test_Training_Bangladesh-FIND-icddr-Menzies_FINAL_26Nov19.pdf 

**Describe the enhancement**

1. Add a menu item to initiate the control mode test 
2. Add a Questionnaire to be able to conduct control mode, similar to the Test Questionnaire, an image of control mode and the 2 entry points for G6PD and Haemoglobin level. This should have the following fields 
> Chip No
> Device Serial number
> G6PD level
> Haemoglobin level 
3. Author and load  a CQL Library evaluation for control mode evaluation 
4. Initiate the control mode evaluation on press submit 
4. Add a test result page for Control mode level 1 or control mode level 2 
5. Terminate control mode flow once the user has calibrated the device 



<img width=200 height=400 src="https://user-images.githubusercontent.com/4829880/149816294-00760977-20c5-4cfd-83ac-5e2b79b30184.jpg"/> <img width=200 height=400 src="https://user-images.githubusercontent.com/4829880/149816315-6f7ebaa1-d7cd-445b-8622-54f52d51780a.jpg"/> <img width=200 height=400 src="https://user-images.githubusercontent.com/4829880/149816323-0afb2558-35a4-47fd-9da5-88cb668f0d14.jpg"/> <img width=200 height=400 src="https://user-images.githubusercontent.com/4829880/149816328-b2841f26-4560-4613-8fd0-cd1bfe707967.jpg"/>



**Use Case 2 : In app reporting of Indicators**

**1. Notice D - mADX**

Based on the WHO Implementation Guide (IG) for ANC http://build.fhir.org/ig/WorldHealthOrganization/smart-anc/ and SMART ANC Indicators  - https://github.com/WorldHealthOrganization/smart-anc/tree/master/input/cql. These indicators are based on the Global ANC monitoring framework (25) and the WHO–UNICEF guidance for RMNCAH programme managers on the analysis and use of health facility data (10). These indicators may be aggregated automatically from the digital tracking tool to populate a digital HMIS, such as DHIS2.

Indicator definitions are represented using the FHIR Measure resource (CPGMetric profile) and is available here These indicators are based on the Global ANC monitoring framework (25) and the WHO–UNICEF guidance for RMNCAH programme managers on the analysis and use of health facility data (10). These indicators may be aggregated automatically from the digital tracking tool to populate a digital HMIS, such as DHIS2.

Indicator definitions are represented using the FHIR Measure resource (CPGMetric profile) listed here http://build.fhir.org/ig/WorldHealthOrganization/smart-anc/documentation.html#indicators

**2. eCBIS  - Measure Reporting of Indicators**


Indicators listed here  - https://docs.google.com/spreadsheets/d/1Kfp0rRYlksrBoSIecAuovZ8Y3k8yH9mkmgHcpQbMtXI/edit#gid=446158820 


**Sample Measure Reporting CQL (Household count)** 

**Family Group**

````
{
  "resourceType": "Group",
  "id": "107759",
  "meta": {
    "versionId": "3",
    "lastUpdated": "2022-04-14T13:54:41.656+00:00",
    "source": "#2f77c0c4a5d11f1e"
  },
  "identifier": [ {
    "use": "official",
    "value": "7bb7fa82-59e4-4744-a6d2-96722afca23b"
  }, {
    "use": "secondary",
    "value": "656766"
  } ],
  "active": true,
  "type": "person",
  "code": {
    "coding": [ {
      "system": "https://www.snomed.org",
      "code": "35359004",
      "display": "Family"
    } ]
  },
  "name": "Kamwana Rao",
  "managingEntity": {
    "reference": "RelatedPerson/2e2c9d4e-b67f-4673-8e83-e3f576046296"
  },
  "member": [ {
    "entity": {
      "reference": "Patient/c535f2e6-a17a-4b2a-913e-30e8b42553d0",
      "display": "Rao Kamwana"
    },
    "inactive": false
  }, {
    "entity": {
      "reference": "Patient/d0612f01-01c3-467e-9fbc-4e6ebe8bf9e6",
      "display": "Magarita Rao"
    },
    "inactive": false
  } ]
}
````


**Measure Reporting CQL Expression**

````
//Declare name and version of lib
library PQMedication version '1'

//FHIR as a data model and version of FHIR
using FHIR version '4.0.1'

//functions to help FHIR vs CQL primitives
include "FHIRHelpers" version '4.0.1' called FHIRHelpers

context Patient

define "All Groups": [Group] G
define "All Person Groups": "All Groups" G where G.type = 'person'
define "All Group Members": flatten("All Person Groups" G return (G.member M return M.entity))

define "All Patients": [Patient] P
    with "All Group Members" G
        such that P.id = Split(G.reference, '/')[1]

define "Males": "All Patients" M where M.gender='male'
define "Females": "All Patients" F where F.gender='female'

define "Age": CalculateAgeInYears(Patient.birthDate)

define "Age Stratifier":
  case
    when "Age" < 1 then 'P0Y'
    when "Age" in Interval[1,5] then 'P1-5Y'
    when "Age" in Interval[6, 14] then 'P6-14Y'
    when "Age" in Interval[15, 49] then 'P15-49Y'
    when "Age" >= 50 then 'P50Y'
    else 'Unspecified'
  end

````


**Measure Report Generated**

````
{
  "resourceType": "MeasureReport",
  "extension": [
    {
      "url": "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.population.description",
      "valueString": "???????????"
    }
  ],
  "status": "complete",
  "type": "summary",
  "measure": "Measure/group-measure",
  "date": "2022-05-20T14:17:20+03:00",
  "period": {
    "start": "2019-01-01T00:00:00+03:00",
    "end": "2022-12-31T23:59:59+03:00"
  },
  "improvementNotation": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/measure-improvement-notation",
        "code": "increase"
      }
    ]
  },
  "group": [
    {
      "id": "groups",
      "population": [
        {
          "id": "initial-population",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "initial-population"
              }
            ]
          },
          "count": 17
        },
        {
          "id": "denominator",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "denominator"
              }
            ]
          },
          "count": 17
        },
        {
          "id": "numerator",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "numerator"
              }
            ]
          },
          "count": 17
        }
      ],
      "measureScore": {
        "value": 1
      }
    },
    {
      "id": "males",
      "population": [
        {
          "id": "initial-population",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "initial-population"
              }
            ]
          },
          "count": 17
        },
        {
          "id": "denominator",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "denominator"
              }
            ]
          },
          "count": 17
        },
        {
          "id": "numerator",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "numerator"
              }
            ]
          },
          "count": 7
        }
      ],
      "measureScore": {
        "value": 0.4117647058823529
      },
      "stratifier": [
        {
          "id": "by-age",
          "stratum": [
            {
              "value": {
                "text": "P0Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 1
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 1
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 1
                }
              ],
              "measureScore": {
                "value": 1
              }
            },
            {
              "value": {
                "text": "P50Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 1
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 1
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 0
                }
              ],
              "measureScore": {
                "value": 0
              }
            },
            {
              "value": {
                "text": "P15-49Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 2
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 2
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 1
                }
              ],
              "measureScore": {
                "value": 0.5
              }
            },
            {
              "value": {
                "text": "P6-14Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 2
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 2
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 0
                }
              ],
              "measureScore": {
                "value": 0
              }
            },
            {
              "value": {
                "text": "P1-5Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 11
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 11
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 5
                }
              ],
              "measureScore": {
                "value": 0.45454545454545453
              }
            }
          ]
        }
      ]
    },
    {
      "id": "females",
      "population": [
        {
          "id": "initial-population",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "initial-population"
              }
            ]
          },
          "count": 17
        },
        {
          "id": "denominator",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "denominator"
              }
            ]
          },
          "count": 17
        },
        {
          "id": "numerator",
          "code": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "numerator"
              }
            ]
          },
          "count": 10
        }
      ],
      "measureScore": {
        "value": 0.5882352941176471
      },
      "stratifier": [
        {
          "id": "by-age",
          "stratum": [
            {
              "value": {
                "text": "P0Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 1
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 1
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 0
                }
              ],
              "measureScore": {
                "value": 0
              }
            },
            {
              "value": {
                "text": "P50Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 1
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 1
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 1
                }
              ],
              "measureScore": {
                "value": 1
              }
            },
            {
              "value": {
                "text": "P15-49Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 2
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 2
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 1
                }
              ],
              "measureScore": {
                "value": 0.5
              }
            },
            {
              "value": {
                "text": "P6-14Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 2
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 2
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 2
                }
              ],
              "measureScore": {
                "value": 1
              }
            },
            {
              "value": {
                "text": "P1-5Y"
              },
              "population": [
                {
                  "id": "initial-population",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "initial-population"
                      }
                    ]
                  },
                  "count": 11
                },
                {
                  "id": "denominator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "denominator"
                      }
                    ]
                  },
                  "count": 11
                },
                {
                  "id": "numerator",
                  "code": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                        "code": "numerator"
                      }
                    ]
                  },
                  "count": 6
                }
              ],
              "measureScore": {
                "value": 0.5454545454545454
              }
            }
          ]
        }
      ]
    }
  ]
}
````
**NB.**

**Issues Identified when executing CQL**
CQL scripts when run first time take too long to run. Look into libraries or classes it loads first time and move those to Application startup

**Solution**

Pre-load libraries

