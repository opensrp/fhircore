library Example.RelatedContextRetrieve version '0.1.0'

using QDM version '5.5'

/*
Consider the example of a "mother-infant" measure that requires data from both a mother
and the infant. If we think about the measure as written from the infants perspective,
then we need some way to be able to access the data from the mother's record.
*/

codesystem "SNOMED": '1.2.3.4.5'
valueset "Estimated Due Date Exam": 'TBD'
valueset "Live Delivery": 'TBD'
code "Mother Relationship": 'Mother' from "SNOMED"

context Patient

/*
From the perspective of the infant, the "Mother" is a "Related Person" (new entity in QDM 5.5)
So we can define a "Mother" expression that lets us talk about who the mother is:
*/
define "Mother": singleton from (["Related Person": "Mother Relationship"])

// More guidance is needed on what codes to use, so QDM and FHIR profiles would 
// need to help in identifying what the terminologies are

/*
However, data coming back from a Retrieve in CQL is always limited to the current 
context, which is the infant's record. So even though we know who the mother is,
we need to specify that data should be retrieve from the Mother's record, rather
than the current patient (the infant). This is known as a related-context retrieve
in CQL and is shown in the following example:

Note the use of the "Mother" -> syntax in the retrieve below:
*/

define "Estimated Due Date":
  Last(
    ["Mother" -> "Physical Exam, Performed": "Estimated Due Date Exam"] Exam
      where Exam.relevantPeriod ends 1 year on or before "Birth Date"
      sort by start of relevantPeriod
  ).result as DateTime

/*
This allows us to use the "Mother" expression as the "context" for just this retrieve.
So the above definition is saying "Return the most recent physical exam performed for estimated
due date from the mother's record"
*/

/*
Implementation Note: Because the "Mother" expression in this example is returning a RelatedPerson,
we need a way to know which attribute of that RelatedPerson type contains the "id" of the linked
patient. The QDM 5.5 model info for RelatedPerson contains a "linkedPatientId" attribute for this
purpose. In addition, the model info specifies a targetContextRelationship that defines this linkedPatientId
as the element to be used. The translator will automatically insert a property reference into the 
ELM to access this information.
*/

/*
We can then use that retrieve, containing the mother's most recent estimated due date result to
perform a calculation in the infant measure, in this case, gestational age at birth:
*/

define "Gestational Age in Days at Birth":
  (280 - (duration in days between "Estimated Due Date" and "Birth Date")) div 7
  
define "Birth Date":
  start of
    Last(
      ["Encounter, Performed": "Live Delivery"] Delivery
        sort by start of relevantPeriod
    ).relevantPeriod