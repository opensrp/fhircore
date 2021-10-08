# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - 2021-xx-xx

### Added

- Added measure reporting to ANC application
- Added class for Measure report evaluation which will be used in ANC application
- ANC | Added Condition resource to sync params list
- Moved Token to secure storage from AccountManager
- Expose [custom user attribute](https://www.keycloak.org/docs/latest/server_admin/index.html#_user-attributes) `questionnaire_publisher` available in SharedPreferences with key `USER_QUESTIONNAIRE_PUBLISHER` (#607)
- Quest | Filter [Questionnaires](http://hl7.org/fhir/questionnaire.html) by [publisher](http://hl7.org/fhir/questionnaire-definitions.html#Questionnaire.publisher) using user attribute as per above. (#571)
- Quest | Patient List, Load Config from server
- Quest | Added Patient Profile View

### Fixed

- Added dependecies that were missing and causing CQL Evaluation to fail
- Out of memory issue on few tests
- Authentication toekn expiry issue

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
