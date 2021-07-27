# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Gender input changed to radio selection on client info screen
- Display message on empty list along with new client register button
- Client count display in drawer
- Login and Keycloak authentication for FHIR APIs
- Added user logout functionality
- Vaccine status stylign in list view and details view
- Show progress bar on login attempt

### Fixed

- Select initial/default value of Radio Button on client info screen
- Fixed patient list pagination button position to static on bottom of list
- Fixed patient list fragment adapter count
- Fixed bug on search result on patient list
- Fixed bug on barcode crash
- Fixed bug on record vaccine
- Fixed bug on patient list last seen date

### Changed
- Reduce font size and spacing on patient list screen
- removed covax prefix and postfix in general use-case

## [0.0.3] - 2021-05-24
### Added

- Extract and assign Patient ID from Questionnaire field
- Build barcode scanner
- Track patient vaccine status
- Display immunization info on the Patient Profile 

