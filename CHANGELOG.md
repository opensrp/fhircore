# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - 2021-xx-xx

### Added
- ANC | Update Family Profile view
- ANC | Migrated loading of measure report libraries to the report module from anc details page. Also did some optimizations.
- ANC | Report Measures List Items, Filter Selection, Patient Selection, Compose UI unit tests
- ANC | Report Patient Date Range Selection with year, Patient Search compose UI
- ANC | Report Result Page Individual Patient Item UI update plus Unit Tests
- ANC | Report Result Page For All Population compose UI update plus Unit Tests

- Quest | Add the ability to search for patients by id
- Quest | Add ability to view previous QuestionnaireResponse
- Quest | Added workflow configurations
- Quest | Add photo capture questionnaire widget
- Quest | Add ability to edit questionnaire responses

- EIR | Added workflow configurations
- EIR | Add Adverse Events feature to log any reactions after the first dose of vaccine using Structure map

### Fixed
- EIR | Resolved a bug in the vaccination flow : App crush when saving vaccine
- EIR | Ability for a complete immunixation flow from Record 1st Vaccine to Full Immunization


### Changed
- EIR | Updated overdue trigger flow for Vaccine Due date
- EIR | Updated on save vaccine received alert dialogue


## [0.0.2 Quest, 0.0.3 - ANC] - 2021-10-27

### Added
- ANC | Optimized memory use for measure reporting by using String builder File forgotten in PR 646
- ANC | Optimized memory use for measure reporting by using String builder and loading only once cql and measure reporting libraries
- ANC | Added progress bar to cql and measure reporting in ANC patien details page
- ANC | Added measure reporting to ANC application
- Engine |Added class for Measure report evaluation which will be used in ANC application
- ANC | Added Condition resource to sync params list
- Moved Token to secure storage from AccountManager
- Expose [custom user attribute](https://www.keycloak.org/docs/latest/server_admin/index.html#_user-attributes) `questionnaire_publisher` available in SharedPreferences with key `USER_QUESTIONNAIRE_PUBLISHER` (#607)
- Quest | Filter [Questionnaires](http://hl7.org/fhir/questionnaire.html) by [publisher](http://hl7.org/fhir/questionnaire-definitions.html#Questionnaire.publisher) using user attribute as per above. (#571)
- Quest | Patient List, Load Config from server
- Quest | Added Patient Profile View
- Quest | Patient Registration Questionnaire
- Quest | Test Results Questionnaire
- Quest | Feedback on UI
- Quest | Remove custom activity
- Quest | Add tags to patient via questionnaire
- Engine | AlertDialog on questionnaire activity load, back, submit, progress
- Engine | Block questionnaire submit if validation errors exist
- Quest | Bottom Navigation updated with new structure
- Quest | User actions, profile screen implemented
- Engine | print patient age as #y #m or #m #d
- ANC | Navigation structure changes implemented
- ANC | Bottom sheet for switching registers added
- ANC | User actions, profile screen implemented
- ANC | Drawer menu removed

### Fixed

- Added dependecies that were missing and causing CQL Evaluation to fail
- Out of memory issue on few tests
- Authentication toekn expiry issue
- Fhir Resource Converter issue after resource update
- Inteceptor handling for missing account
- Engine: Fixes Structure map based resource extraction not working | Remove the menu icon on the login screen
- ANC | Fix crashes due to invalid data
- Engine | Remove the menu icon on the login screen
- Engine: Fixes Structure map based resource extraction not working
- eCBIS | Login page updated

### Changed

-

## [0.0.9 EIR, 0.0.2 - ANC] - 2021-09-27

### Added

- Family Registeration with Tags from SDK fix for tags and profile
- Add edit patient registration feature. This enables you to modify the patient and related person's details
- Add Family Profile View
- Add Past Encounters Screen
- Add missing properties in the COVAX Immunization Resource
- New mockup changes incorporated
- Added class for Measure report evaluation which will be used in ANC application
- Added Logic for CQL Evaluation in ANC App. This include, AncDetailsFragment,ViewModel & Tests
- Added CQL Lib Evaluator class for CQl Expressions

### Changed

- Refactor COVAX Immunization extraction to use StructureMap-based extractio

## [0.0.8 EIR, 0.0.1 - ANC] - 2021-09-11

### Added

- Add related person to patient registration
- Integrate StructureMap-based extraction
- ANC register
- Load Questionnaire from DB
- Search by WHO identifier
- Sync progress loader and datetime
- ANC details activity
- Enroll into ANC and add Encounter, Observation, Episode of Care, Condition

### Fixed

- Fixed empty patient view layout position
- Fixed bug on patients not populated on fresh install
- Fixed the sync process did not let loader open on list patients unless sync is completed

### Changed

- Changed patient registration extraction to use StructureMap-based extraction

## [0.0.7 EIR] - 2021-07-28

### Added

- Show progress bar on login attempt

### Fixed

- Fixed bug on barcode crash
- Fixed bug on record vaccine
- Fixed bug on patient list last seen date

## [0.0.6 EIR] - 2021-07-15

### Added

- Gender input changed to radio selection on client info screen
- Display message on empty list along with new client register button
- Client count display in drawer
- Login and Keycloak authentication for FHIR APIs
- Added user logout functionality
- Vaccine status stylign in list view and details view

### Fixed

- Select initial/default value of Radio Button on client info screen
- Fixed patient list pagination button position to static on bottom of list
- Fixed patient list fragment adapter count
- Fixed bug on search result on patient list

### Changed

- Reduce font size and spacing on patient list screen
- Removed covax prefix and postfix in general use-case

## [0.0.3] - 2021-05-24

### Added

- Extract and assign Patient ID from Questionnaire field
- Build barcode scanner
- Track patient vaccine status
- Display immunization info on the Patient Profile
- Enable patient overdue toggle filter
