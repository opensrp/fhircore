### MEASURE REPORT
A [measure report](http://hl7.org/fhir/R4/measurereport.html) is a FHIR resource which represents the outcome of calciulation of a [measure](http://hl7.org/fhir/R4/measure.html) for a particular subject or population of subjects.

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
          "code": {
            "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "initial-population"
            } ]
          },
          "count": 136
        }, {
          "id": "denominator",
          "code": {
            "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "denominator"
            } ]
          },
          "count": 7
        }, {
          "id": "numerator",
          "code": {
            "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                "code": "numerator"
            } ]
          },
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
