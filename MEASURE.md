## Measure
The [Measure](http://hl7.org/fhir/R4/measure.html) is a FHIR resource which represents definition- for calculation of an indicator. The Measure uses a logic Library (CQL) that contains the calculation logic for measure.

A working example (Households and Members disaggregated by age) of Measure can be found [here](https://github.com/opensrp/fhir-resources/blob/main/ecbis/measure/household_measure.fhir.json). Notable components in example Measure are

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
  "scoring": { ... },
  "group": [ {
    "id": "males",
    "population": [ {
      "id": "initial-population",
      "code": { ... },
      "criteria": {
        "language": "text/cql-identifier",
        "expression": "Patients"
      }
    }, {
      "id": "denominator",
      "code": { ... },
      "criteria": {
        "language": "text/cql-identifier",
        "expression": "Group Member"
      }
    }, {
      "id": "numerator",
      "code": { ... },
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

Details of notable fields for Measure

**url**: The complete url of Measure with Measure.name i.e. http://fhir.org/guides/who/anc-cds/Measure/HOUSEHOLDIND01

**name**: A unique name for Measure i.e. HOUSEHOLDIND01. The Measure is loaded by name and url into measure processor

**relatedArtifact**: Helper libraries to load before running Measure to be used by measure logic libraries i.e. FHIRHelpers|4.0.1. The Library is loaded by its canonical url i.e. Library/123

**library**: The CQL logic libraries used by Measure for calculation i.e. http://fhir.org/guides/cqf/common/Library/HOUSEHOLDIND01. The logic library is loaded by its url. The url must end with /Library/name 

**scoring**: proportion | ratio | continuous-variable | cohort

**group**: The section of report defining the stats for a specific indicator (may be disaggregated by stratifier). Each group has following components
- id: Group name/id.
- population: The calculations (or name of variable in CQL that defines the value) for each of population components i.e. initial-population, denominator, numerator
- stratifier: The disaggregations for given population numerator. i.e. by age, by month, by education etc 
- supplementalData: Any extra data or intermediate calculation to be output to final report. Current implementation of MeasureEvaluator does not allow running any measure which is not Patient centric i.e. a Measure.subject can always be a Patient. Hence, we are using `supplementalData` to output Group for each indicator and then counting distinct Group to count Households


## CQL Logic/Decision Library for Measure

The Measure needs a [Library](http://hl7.org/fhir/R4/library.html) to get the logic/calculation for ecah variable. This calculation or logic comes from CQL. [Here](https://cql.hl7.org/01-introduction.html) is a detailed guide on CQL. Some example CQL scripts can be found [here](https://github.com/opensrp/fhir-resources/tree/main/ecbis/measure_cql).

- CQL brief authoring guide https://cql.hl7.org/02-authorsguide.html
- CQL operators and functions https://cql.hl7.org/04-logicalspecification.html and https://cql.hl7.org/09-b-cqlreference.html
- Fhirpath mapping in CQL https://cql.hl7.org/16-i-fhirpathtranslation.html
- Examples [Time Interval](https://cql.hl7.org/15-h-timeintervalcalculations.html) and [Detail on Queries](https://cql.hl7.org/03-developersguide.html#conditional-expressions)
- CQL Sandbox is [here](https://sandbox.cqlab.io/ CQL sandbox)
- CQL android editor app is [here](https://github.com/Path-Check/cql-editor-app/)

A working example of used by above Measure is [here](https://github.com/opensrp/fhir-resources/blob/main/ecbis/measure_cql/household_measure_reporting.cql). Some notable lines are

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
The [fhir-resources](https://github.com/opensrp/fhir-resources/blob/main/fhircore-testing) repository has a testing module which allows to not only get the complete Library resource to directly save to server but also allows to test the Measure output and make changes on the fly. Check the cucumber tests [Feature File](https://github.com/opensrp/fhir-resources/blob/main/fhircore-testing/src/test/resources/measure-report/household-members.feature), the [Test Code File](https://github.com/opensrp/fhir-resources/blob/main/fhircore-testing/src/test/kotlin/com/fhircore/resources/testing/measure/HouseholdMembersMeasureTest.kt#L28) and the [Convertor Util Method](https://github.com/opensrp/fhir-resources/blob/main/fhircore-testing/src/test/kotlin/com/fhircore/resources/testing/CqlUtils.kt#L31)

**FhirCore Unit Tests**
The CQL can also be translated to Library using an approach as used by fhircore as in [CQL Content Tests](https://github.com/opensrp/fhircore/blob/main/android/quest/src/test/java/org/smartregister/fhircore/quest/CqlContentTest.kt#L57). A complete Library resource is output to console as a result.

## Testing Measure and CQL Library

To make sure your Measure and Library are working and have been validated data, it is best to thoroughly test the input and output first so that multiple updates to server can be avoided and an easy and quick test driven approach is opted to implement your new functionality. Fhir-Resources [repository](https://github.com/opensrp/fhir-resources) has a testing module which implements Cucumber tests to help facilitate this. 
- Checkout [Fhir-Resources Repository](https://github.com/opensrp/fhir-resources)
- Open module fhircore-testing into Intellij or VS Code
- Install Cucumber plugin (optional and allows to run feature file directly)
- The module has a lot of helper functions to allow creating sample data and testing basic MeasureReport output. if you understand Cucumber you can add a complete custom test as well
- Add you test feature file to fhircore-testing/src/test/resources/measure-report/ i.e. fhircore-testing/src/test/resources/measure-report/household-members.feature
```
Feature: Household Members Count by Age group

  Scenario: Household members in household disaggregated by age group
    Given Household Members CQL is "ecbis/measure_cql/household_measure_reporting.cql"
    Given Household Members Measure is "ecbis/measure/household_measure.fhir.json"
    Given Household Members Sample Bundle has 19 Groups; of them 17 active person
    Given Household Members Sample Bundle has 16 Group Members gender "male" active aged
      | 0 | 1 | 4 | 5 | 6 | 9 | 14 | 15 | 16 | 25 | 34 | 40 | 49 | 50 | 51 | 55 |
    Given Household Members Sample Bundle has 21 Group Members gender "female" active aged
      | 0 | 1 | 2 | 4 | 5 | 6 | 7 | 9 | 14 | 15 | 16 | 21 | 25 | 34 | 40 | 44 | 48 | 49 | 50 | 51 | 55 |
    When Household Members Measure is run
    Then Household Members Measure Report has "evaluatedResource.count()" = "17"
    Then Household Members Measure Report has "group.count()" = "2"
    Then Household Members Measure Report has "group.where(id='males').population[0].count" = "37"
    Then Household Members Measure Report has "group.where(id='males').population[1].count" = "37"
    Then Household Members Measure Report has "group.where(id='males').population[2].count" = "16"
    Then Household Members Measure Report has "group.where(id='females').population[0].count" = "37"
    Then Household Members Measure Report has "group.where(id='females').population[1].count" = "37"
    Then Household Members Measure Report has "group.where(id='females').population[2].count" = "21"
```
 - 'Household Members' is the TAG. This should match your Measure core functionality
 - 'ecbis/measure_cql/household_measure_reporting.cql' is cql path. This should be the path from main dir i.e. fhir-resources where your cql resides
 - 'ecbis/measure/household_measure.fhir.json' is measure path. This should be the path from main dir i.e. fhir-resources where your measure resides
 - Change test data as per your requirements. You can change values into double quotes, int values, or in data tables as per your test requirements
 - Make sure to use same Step definition convention i.e. Given `TAG` CQL is "`cql-path`" OR Given `TAG` Measure is "`measure path`". Otherwise you would need to write your own Steps into YourTagTest.kt file using standard defined in [Cucumber Tutorial](https://medium.com/@mlvandijk/kukumber-getting-started-with-cucumber-in-kotlin-e55112e7309b)
 - The assertions should also use same Step convention. i.e. Then `TAG` Measure Report has "`your-fhirpath-in-measure-report`" = "`expected-size`". Or you can add additional assertions to Test code file as in `Then` section below
- Add your test file to kotlin/com/fhircore/resources/testing/measure/YourMeasureNameTest.kt i.e. kotlin/com/fhircore/resources/testing/measure/HouseholdMembersMeasureTest.kt
- Note that the feature-file name should match with test-file name i.e. household-members.feature corresponds to HouseholdMembersMeasureTest.kt
  
```
@RunWith(Cucumber::class)
@CucumberOptions(
  features = ["src/test/resources/measure-report/household-members.feature"], tags = "not @ignored"
)
class HouseholdMembersMeasureTest : En {
  private val measureContext = MeasureContext().apply { withMeasurePeriod("2022-01-01", "2022-07-15") }

  private val TAG = "Household Members"

  init {
    measureContext.givenCql(TAG) // helper step definition to load and print Library in console
    measureContext.givenMeasure(TAG) // helper step definition to load and print Measure in console

    // add your test data below. you can add custom code as well to load data of your choice
    measureContext.givenGroupsXHavingYActivePerson(TAG) // helper step definition to add households test data
    measureContext.givenGroupMembersXHavingGenderYAndAgesZ(TAG) // helper step definition to add household members, patients test data

    // helper step definition to run measure of given type and print report in console
    measureContext.whenMeasureRun(TAG, MeasureEvalType.POPULATION)

    // helper step definition to add assertions. You can add custom assertions as well to test complex data
    Then("Household Members Measure Report has {string} = {string}") { path: String, value: String ->
      measureContext.thenMeasureReportHas(path, value)

      // Add additional assertions if needed
      // assertEquals(0.5, measureContext.measureReport.group.first().measureScore)
    }
  }
}
```
- The final working Library, Measure, and MeasureReport are printed to console which can be copied and POST/PUT to server. 

## MeasureReport
A [MeasureReport](http://hl7.org/fhir/R4/measurereport.html) is a FHIR resource which represents the outcome of calculation of a [Measure](http://hl7.org/fhir/R4/measure.html) for a particular subject or population of subjects.

The output MeasureReport of above Measure is [here](https://github.com/opensrp/fhir-resources/blob/main/ecbis/measure_report/household_measure_report.fhir.json). Some important components of report are below

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

Details of some notable fields in above MeasureReport (calculated from Measure) are 

**contained**: The Measure [property](http://hl7.org/fhir/R4/measure-definitions.html#Measure.supplementalData) `supplementalData` is calculated for each measure `subject` and output as an [Observation](http://hl7.org/fhir/R4/observation.html) having extension http://hl7.org/fhir/StructureDefinition/cqf-measureInfo with inner extension defining the variable requested i.e. `group` in case above. The code.coding.code has the value of given variable `Group/1818d503-7226-45cb-9ac7-8c8609dd37c0/_history/3` in example above

**measure**: The Measure.url for which this report was generated i.e. http://fhir.org/guides/who/anc-cds/Measure/HOUSEHOLDIND01

**period**: Measure period which was sent for date filter i.e. reporting period start and end. The Measure interval has closed boundaries. Read details [here](https://cql.hl7.org/02-authorsguide.html#interval-values) 

**group**: The calculated value for each Measure indicator with
 - id: id/name of group/indicator/stratifier as defined in Meaure. 
 - count: Calculated value from CQL for given variable
 - measureScore: The percent/ratio of calculated value i.e. numerator/denomintor. Note that for stratifier the score denominator is stratifier denomintor rather than group denominator
 - 
**stratifier**: Count for given indicator disaggregated by each type. The stratifier misses the values where counts are zero. Hence if stratifier has predefined criteria, each should be a calculated as separate group. 


## FhirCore Integration

Once your Measure, and Library is ready add these to sync_config.json to make sure that the Measure and all dependent Library resources are always synced.
- Save/Update Measure on server i.e. POST - https://your.fhir.server/fhir/Measure OR PUT - https://your.fhir.server/fhir/Measure/measure-id 
- Save/Update Library on server i.e. POST - https://your.fhir.server/fhir/Library OR PUT - https://your.fhir.server/fhir/Library/library-id 
- Update the sync_config.json Debug or Binary config for your app with new your measure id, and library id as below
```
  {
    "resource": {
      "resourceType": "SearchParameter",
      "name": "_id",
      "code": "_id",
      "base": [
        "Measure"
      ]
    },
    "type": "token",
    "expression": "133082,133104,{your-measure-id}"
  },
  ... ... ...
  {
    "resource": {
      "resourceType": "SearchParameter",
      "name": "_id",
      "code": "_id",
      "base": [
        "Library"
      ]
    },
    "type": "token",
    "expression": "1753,133081,133105,{your-library-id},{any-helper-library-ids}"
  }
```
- Update the measure_report_config.json Debug or Binary config with your new measure so that it shows up in the list
``` 
"reports": [
  ... ... ...
    {
      "id": "new serial id in list",
      "title": "Household Members",
      "description": "Number of Households, Household members registered, disaggregated by age and gender for each age group",
      "url": "http://fhir.org/guides/who/anc-cds/Measure/HOUSEHOLDIND01"
    },
    ... ... ... 
  ]
```

## Screenshots

<img width="200" height="400" src="https://user-images.githubusercontent.com/4829880/188478590-93474727-4ef2-4ba7-acfc-8eb6ac9e32d5.png"/> <img width="200" height="400" src="https://user-images.githubusercontent.com/4829880/188478602-e3941d73-4582-43a2-bc24-423eb1253fe0.png"/>


