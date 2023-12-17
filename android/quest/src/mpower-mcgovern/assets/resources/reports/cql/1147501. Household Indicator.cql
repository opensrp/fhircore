library HOUSEHOLDIND01 version '1'

using FHIR version '4.0.1'

include "FHIRHelpers" version '4.0.1' called FHIRHelpers
include "COMMON" version '1' called COMMON

parameter "Measurement Period" Interval<DateTime>

context Patient

// to ensure that Measure Period with closed boundary can be handled
define "Filter Period": Interval("Measurement Period".low - 1 day , "Measurement Period".high + 1 day)

define "All Groups": [Group] G
    where G.type = 'person'
    and First(G.identifier).period.start during "Filter Period"

define "All Group Members": flatten("All Groups" G return (G.member M return M.entity))

define "Group": "All Groups" G
    return G.id

define "Group Member": "All Group Members" G
    where COMMON."IdPart"(Patient.id) = Split(G.reference, '/')[1]


define "Male": Patient.gender = 'male'
define "Female": Patient.gender = 'female'
define "Patients": {Patient}

define "Age": CalculateAgeInYearsAt(Patient.birthDate, ToDate("Measurement Period".high))

define "Age Stratifier":
  case
    when "Age" < 1 then 'P0Y'
    when "Age" in Interval[1,5] then 'P1-5Y'
    when "Age" in Interval[6, 14] then 'P6-14Y'
    when "Age" in Interval[15, 49] then 'P15-49Y'
    when "Age" >= 50 then 'P50Y'
    else 'Unspecified'
  end

/*
define "P0Y": if "Age" < 1 then 'P0Y' else null
define "P1-5Y": if "Age" in Interval[1,5] then 'P1-5Y' else null
define "P6-14Y": if "Age" in Interval[6, 14] then 'P6-14Y' else null
define "P15-49Y": if "Age" in Interval[15, 49] then 'P15-49Y' else null
define "P50Y": if "Age" >= 50 then 'P50Y' else null
*/

/*
define "Male": if(Patient.gender) = 'male' then 'Male' else null
define "Female": if(Patient.gender) = 'female' then 'Female' else null
*/

define "Gender Stratifier":
  case
    when Patient.gender = 'male' then 'Male'
    when Patient.gender = 'female' then 'Female'
    else 'Other'
  end