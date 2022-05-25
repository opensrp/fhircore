**Introduction** 
This Documentation on CQL is to give some insights on
1. How to author the CQL script
2. How to load and execute CQL scripts
3. Some sample use cases where we have used CQL 


**What is CQL** 
CQL­ is a Health Level Seven International® (HL7®) authoring language standard that’s intended to be human readable. It is part of the effort to harmonize standards used for electronic clinical quality measures (eCQMs) and clinical decision support (CDS). CQL provides the ability to express logic that is human readable yet structured enough for processing a query electronically. CQL is the expression logic used in Health Quality Measure Format (HQMF) beginning with the eCQMs implemented in calendar year 2019. CQL replaces the logic expressions previously defined in the Quality Data Model (QDM). Beginning with v5.3, QDM includes only the conceptual model for defining the data elements (the data model). Measure authors with access to the Measure Authoring Tool (MAT) can use the tool to author measures using CQL­. Visit the MAT webpage for more information.  

CQL allows for a more modular, flexible, and robust expression of the logic. It allows logic to be shared between measures and with clinical decision support.

**How are we using CQL?**

**G6PD**
1. Threshold management 
2. G6PD Test Device Calibration   

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

**In app reporting of Indicators**
1. Notice D - mADX 

Based on the WHO Implementation Guide (IG) for ANC http://build.fhir.org/ig/WorldHealthOrganization/smart-anc/ and SMART ANC Indicators  - https://github.com/WorldHealthOrganization/smart-anc/tree/master/input/cql. These indicators are based on the Global ANC monitoring framework (25) and the WHO–UNICEF guidance for RMNCAH programme managers on the analysis and use of health facility data (10). These indicators may be aggregated automatically from the digital tracking tool to populate a digital HMIS, such as DHIS2.

Indicator definitions are represented using the FHIR Measure resource (CPGMetric profile) and is available here These indicators are based on the Global ANC monitoring framework (25) and the WHO–UNICEF guidance for RMNCAH programme managers on the analysis and use of health facility data (10). These indicators may be aggregated automatically from the digital tracking tool to populate a digital HMIS, such as DHIS2.

Indicator definitions are represented using the FHIR Measure resource (CPGMetric profile) listed here http://build.fhir.org/ig/WorldHealthOrganization/smart-anc/documentation.html#indicators

2. eCBIS  - Measure Reporting of Indicators

Indicators listed here  - https://docs.google.com/spreadsheets/d/1Kfp0rRYlksrBoSIecAuovZ8Y3k8yH9mkmgHcpQbMtXI/edit#gid=446158820 


**Sample Measure Reporting CQL (Household count)** 

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