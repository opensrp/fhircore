## MEASURE
The [Measure](http://hl7.org/fhir/R4/measure.html) is a FHIR resource which represents definition- for calculation of an indicator. The Measure uses a logic Library (CQL) that contains the calculation logic for measure.

A basic measure report can comprise of following components

```
{
  "url": "http://fhir.org/guides/who/anc-cds/Measure/HOUSEHOLDIND01",
  "name": "HOUSEHOLDIND01",
  "relatedArtifact": [ {
    "type": "depends-on",
    "label": "FHIRHelpers|4.0.1",
    "resource": "Library/1753"
   } ],
  "library": [ "http://fhir.org/guides/cqf/common/Library/HOUSEHOLDIND01" ],
  "scoring": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/measure-scoring",
      "code": "proportion"
    }]
  },
  "group": [ {
    "id": "males",
    "population": [ {
      "id": "initial-population",
      "code": {
        "coding": [ { 
          "system": "http://terminology.hl7.org/CodeSystem/measure-population",
          "code": "initial-population"
      } ]
      },
      "criteria": {
        "language": "text/cql-identifier",
        "expression": "Patients"
      }
    }, {
      "id": "denominator",
      "code": {
        "coding": [ { 
          "system": "http://terminology.hl7.org/CodeSystem/measure-population",
          "code": "denominator"
        } ]
      },
      "criteria": {
        "language": "text/cql-identifier",
        "expression": "Group Member"
      }
    }, {
      "id": "numerator",
      "code": {
        "coding": [ { 
          "system": "http://terminology.hl7.org/CodeSystem/measure-population",
          "code": "numerator"
        } ]
      },
      "criteria": {
        "language": "text/cql-identifier",
        "expression": "Males"
      }
    } ],
      "stratifier": [ {
        "id": "by-age",
        "criteria": {
            "language": "text/cql-identifier",
            "expression": "Age Stratifier"
        }
      }, 
      { ... ... ... } ]
  }, 
  { ... ... ... } ],
  "supplementalData": [
      {
          "description": "groups",
          "criteria": {
              "language": "text/cql-identifier",
              "expression": "Group"
          }
      }
  ]
}
```

Some notable fields

**url**: The complete url of Measure with measure.name i.e. http://fhir.org/guides/who/anc-cds/Measure/HOUSEHOLDIND01

**name**: A unique name for Measure i.e. HOUSEHOLDIND01. The Measure is loaded by name and url into measure processor

**relatedArtifact**: Helper libraries to load before running Measure to be used by measure logic libraries i.e. FHIRHelpers|4.0.1. The Library is loaded by its canonical url i.e. Library/123

**library**: The CQL logic libraries used by Measure for calculation i.e. http://fhir.org/guides/cqf/common/Library/HOUSEHOLDIND01. The logic library is loaded by its url. The url must end with /Library/name 

**scoring**: proportion | ratio | continuous-variable | cohort

**group**: The section of report defining the stats for a specific indicator (may be disaggregated by stratifier). Each group has following components
- id: Group name/id.
- population: The calculations (or name of variable in CQL that defines the value) for each of population components i.e. initial-population, denominator, numerator
- stratifier: The disaggregations for given population numerator. i.e. by age, by month, by education etc 
- supplementalData: Any extra data or intermediate calculation to be output to final report


## MEASURE LIBRARY

```
library HOUSEHOLDIND01 version '1'
...
include "FHIRHelpers" version '4.0.1' called FHIRHelpers
...
// The Measure interval has closed boundaries. Read details https://cql.hl7.org/02-authorsguide.html#interval-values
parameter "Measurement Period" Interval<DateTime>
context Patient
...
define "All Groups": [Group] G where G.type = 'person'
define "All Group Members": flatten("All Groups" G return (G.member M return M.entity))
define "Group": "All Groups" G return G.id
define "Group Member": "All Group Members" G where "Patient Id" = Split(G.reference, '/')[1]
define "Patients": {Patient}

define "Males": Patient.gender='male'
define "Females": Patient.gender='female'

define "Age": CalculateAgeInYearsAt(Patient.birthDate, ToDate("Measurement Period".high))

define "Age Stratifier":
  case
    when "Age" < 1 then 'P0Y'
    ....
  end
```

The CQL is referenced by url into Measure. The CQL is translated into elm-json and uploaded a Library on server. There are multiple ways to get elm for CQL. (elm-xml is also a valid standard but it is not recognized by android fhir libraries yet)

**CQL to ELM REST Translator**:
A [elm REST app](https://github.com/cqframework/cql-translation-service/blob/master/README.md) that can be used to run elm microservice and convert CQL via a REST API. 


**CQL to ELM JAVA Translator**:
A [elm java app](https://github.com/cqframework/clinical_quality_language/blob/master/Src/java/READM.md) that can be used to elm translator on files and get an output. Instructions can be found [here](https://github.com/cqframework/cql-execution#to-execute-your-cql)

**Note**: Above approaches output a json elm which then need to be base64 decoded and copied to the [Library](http://hl7.org/fhir/R4/library.html) content as Attachment.

**Fhir-Resource on FhirCore**:
The [fhir-resources](https://github.com/opensrp/fhir-resources/fhircore-testing/src/test/resources/measure-report/household-members.feature) repository has a testing module which allows you to not only get the complete Library resource to directly save to server but also allows to test the Measure output and make changes on the fly. Check the cucumber tests fhircore-testing/src/test/resources/measure-report/household-members.feature. 

**FhirCore Unit Tests**
The CQL can also be translated to [Library] using an approach like fhircore as in unit tests org.smartregister.fhircore.quest.CqlContentTest#runCqlLibraryTestForPqMedication. A complete Library resource is output to console as a result.

## MEASURE REPORT
A [MeasureReport](http://hl7.org/fhir/R4/measurereport.html) is a FHIR resource which represents the outcome of calculation of a [Measure](http://hl7.org/fhir/R4/measure.html) for a particular subject or population of subjects.

A basic measure report can comprise of following components
```
{
  "resourceType": "MeasureReport",
  "contained": [
    {
      "resourceType": "Observation", ...
      "extension": [ {
          "url": "http://hl7.org/fhir/StructureDefinition/cqf-measureInfo",
          "extension": [...., {
              "url": "populationId",
              "valueString": "group"
           } ]
      } ],
      "code": {
        "coding": [ {
            "code": "Group/1818d503-7226-45cb-9ac7-8c8609dd37c0/_history/3"
        } ] 
      }, ...
  } ],
  "type": "summary",
  "measure": "http://fhir.org/guides/who/anc-cds/Measure/HOUSEHOLDIND01",
  "date": "2022-06-28T12:28:28+05:00",
  "period": {
    "start": "2022-01-01T00:00:00+05:00",
    "end": "2022-06-28T23:59:59+05:00"
  },
  "group": [ {
      "id": "males",
      "population": [ {
          "id": "initial-population",
          "code": { ... },
          "count": 136
        }, {
          "id": "denominator",
          "code": { ... },
          "count": 7
        }, {
          "id": "numerator",
          "code": { ... },
          "count": 3
      } ],
      "measureScore": { "value": 0.42857142857142855 },
      "stratifier": [ {
          "id": "by-age",
          "stratum": [ {
              "value": { "text": "P0Y" },
              "population": [ {
                  "id": "initial-population",
                  "code": { ... },
                  "count": 31
                }, {
                  "id": "denominator",
                  "code": { ... },
                  "count": 4
                }, {
                  "id": "numerator",
                  "code": { ... },
                  "count": 2
              } ],
              "measureScore": { "value": 0.5 }
            }, { ... ... ... ...}
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

Some notable fields coming/cacluated from Measure

**contained**: The Measure [property](http://hl7.org/fhir/R4/measure-definitions.html#Measure.supplementalData) `supplementalData` is calculated for each measure `subject` and output as an [Observation](http://hl7.org/fhir/R4/observation.html) having extension http://hl7.org/fhir/StructureDefinition/cqf-measureInfo with inner extension defining the variable requested i.e. `group` in case above. The code.coding.code has the value of given variable `Group/1818d503-7226-45cb-9ac7-8c8609dd37c0/_history/3` in example above

**measure**: The Measure.url for which this report was generated i.e. http://fhir.org/guides/who/anc-cds/Measure/HOUSEHOLDIND01

**period**: Measure period which was sent for date filter i.e. reporting period start and end. The Measure interval has closed boundaries. Read details [here](https://cql.hl7.org/02-authorsguide.html#interval-values) 

**group**: The calculated value for each Measure indicator with
 - id: id/name of group/indicator/stratifier as defined in Meaure. 
 - count: Calculated value from CQL for given variable
 - measureScore: The percent/ratio of calculated value i.e. numerator/denomintor. Note that for stratifier the score denominator is stratifier denomintor rather than group denominator
 - 
**stratifier**: Count for given indicator disaggregated by each type. The stratifier misses the values where counts are zero. Hence if stratifier has predefined criteria, each should be a calculated as separate group. 
