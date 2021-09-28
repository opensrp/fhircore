# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


### Added
- Added Logic for CQL Evaluation in ANC App. This include, AncDetailsFragment,ViewModel & Tests
- Added CQL Lib Evaluator class for CQl Expressions
- Gender input changed to radio selection on client info screen
- Display message on empty list along with new client register button
- Client count display in drawer
- Login and Keycloak authentication for FHIR APIs
- Added user logout functionality
- Vaccine status stylign in list view and details view
- Show progress bar on login attempt
- Add related person to patient registration
- Integrate StructureMap-based extraction
- ANC register
- Load Questionnaire from DB
- Search by WHO identifier
- Sync progress loader and datetime
- ANC details activity
- Enroll into ANC and add Encounter, Observation, Episode of Care, Condition
- Family Registeration with Tags from SDK fix for tags and profile
- Add edit patient registration feature. This enables you to modify the patient and related person's details
- Add Family Profile View
- Add Past Encounters Screen 
- Add missing properties in the COVAX Immunization Resource 
- New mockup changes incorporated 

### Fixed

- Select initial/default value of Radio Button on client info screen
- Fixed patient list pagination button position to static on bottom of list
- Fixed patient list fragment adapter count
- Fixed bug on search result on patient list
- Fixed bug on barcode crash
- Fixed bug on record vaccine
- Fixed bug on patient list last seen date
- Fixed empty patient view layout position
- Fixed bug on patients not populated on fresh install
- Fixed the sync process did not let loader open on list patients unless sync is completed

### Changed
- Reduce font size and spacing on patient list screen
- removed covax prefix and postfix in general use-case
- Changed patient registration extraction to use StructureMap-based extraction
- Refactor COVAX Immunization extraction to use StructureMap-based extraction

## [0.0.3] - 2021-05-24
### Added

- Extract and assign Patient ID from Questionnaire field
- Build barcode scanner
- Track patient vaccine status
- Display immunization info on the Patient Profile 
- Enable patient overdue toggle filter

