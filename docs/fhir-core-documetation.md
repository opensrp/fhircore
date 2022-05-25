[![Android CI with Gradle](https://github.com/opensrp/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/opensrp/fhircore/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/opensrp/fhircore/branch/main/graph/badge.svg?token=IJUTHZUGGH)](https://codecov.io/gh/opensrp/fhircore)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ee9b6f38b7294fa3aa668e42e52fdf21)](https://www.codacy.com/gh/opensrp/fhircore/dashboard)

# OpenSRP FHIR Core
**Introduction** 
FHIR Core is a Kotlin application for delivering offline-capable, mobile-first healthcare project implementations from local community to national and international scale using FHIR and the WHO Smart Guidelines on Android.

FHIR Core is architected as a FHIR native digital health platform powered by Google's [Android FHIR SDK](https://github.com/google/android-fhir) and [HAPI FHIR](https://hapifhir.io/). FHIR Core's user experience and module oriented design are based on over a decade of real world experience implementing digital health projects with [OpenSRP](https://smartregister.org/). This repository contains the Android mobile application built to:

- load configuration data as FHIR resources,
- support the WHO Smart Guidelines,
- manage the identities of healthcare workers (HCWs), community health workers (CHWs), care teams, patients, and clients,
- collect, view, and edit healthcare data with dynamic forms using FHIR's [Structured Data Capture](https://hl7.org/fhir/us/sdc/index.html) (SDC) implementation,
- securely store healthcare data encrypted at rest and securely transmit healthcare data using TLS,
- manage location hierarchies defined by community to national and international administrative boundaries.

For remote data storage and login, the mobile application requires:
- a [Keycloak](https://www.keycloak.org/) server to manage identity, authentication, and authorization;
- a [HAPI FHIR](https://hapifhir.io/) server to store operation and configuration data that includes the [HAPI FHIR to Keycloak integration](https://github.com/opensrp/hapi-fhir-keycloak).

FHIR Core also interoperates well with:
- [OpenSRP Web](https://github.com/OpenSRP/web) to access healthcare data from the same HAPI FHIR server.

<img align=center width=400 src="docs/assets/fhircore.png">

## Getting Started
This repository contains the folders
* **[android](android)**: for building the Android application.
* **[docs](docs)**: a library of documents describing the FHIR Core solution.

We recommend reviewing the [docs](docs) before setting up the Android Studio Project in the [android](android) folder.

**Repository** 
FHIR Core codebase  -  [https://github.com/opensrp/fhircore](https://github.com/opensrp/fhircore) 

**Progrmamming Language** 
Kotlin v1.6.10

**Code**
1. Hilt - [https://developer.android.com/training/dependency-injection/hilt-multi-module](https://developer.android.com/training/dependency-injection/hilt-multi-module)  
2. Build time vs Run time 
3. Dependencies Injection 

**UI**
1. Jetpack compose for navigation API  - [https://developer.android.com/jetpack/compose/navigation](https://developer.android.com/jetpack/compose/navigation) 

**Architecture** 
1. MVVM pattern 
2. Repository pattern to feed the DB view models with data
3. Android Single-Activity Architecture with Navigation Component - [https://oozou.com/blog/reasons-to-use-android-single-activity-architecture-with-navigation-component-36](https://oozou.com/blog/reasons-to-use-android-single-activity-architecture-with-navigation-component-36) 
4. Using composable fragment with navigation API 
5. Screen view model with components
6. View model business logic
7. Container to has different components e.g Screen component 
8. Component to hold shareable  UI pieces  
9. Use navgraph to link different pagers  - [https://developer.android.com/reference/androidx/navigation/NavGraph#:~:text=NavGraph%20is%20a%20collection%20of,added%20to%20the%20back%20stack](https://developer.android.com/reference/androidx/navigation/NavGraph#:~:text=NavGraph%20is%20a%20collection%20of,added%20to%20the%20back%20stack). 

**Data layer / Database** 
1. Access to DB is via FHIR Engine from Android FHIR SDK 
2. We use Room  - [https://developer.android.com/jetpack/androidx/releases/room?gclid=CjwKCAjwp7eUBhBeEiwAZbHwkdmGs4cqYoOSGYdwG_HGxZn63-xcYgSPnwdyP6zzpznRHAwV9rKwaxoCiOEQAvD_BwE&gclsrc=aw.ds](https://developer.android.com/jetpack/androidx/releases/room?gclid=CjwKCAjwp7eUBhBeEiwAZbHwkdmGs4cqYoOSGYdwG_HGxZn63-xcYgSPnwdyP6zzpznRHAwV9rKwaxoCiOEQAvD_BwE&gclsrc=aw.ds) 
3. Database is encrypted  by default but can be un-encrypted in debug mode 
4. Alternatives
    1. Realm  -  https://realm.io/  - Realm is a fast, scalable alternative to SQLite with mobile to cloud data sync that makes building real-time, reactive mobile apps easy. 
    2. SQLite DB 
5. Domain 
    3. Using The DAO Pattern in Java /Kotlin 
        1. Register DAO factory 
            1. Family  
            2. ANC 
            3. HIV 
    4. Register repository 
        2. Load Register
        3. Count Register 
        4. Load Patient 
    5. App feature manager - filter feature 
    6. Side menu factory 
    7. Paging source for patient pagination sent to view model 
    8. 

**App Configs** 
1. App configs  - Composition resource 


**Setup and Sync** 
1. Composition resource 
2. Binary resource 
3. App config ID 


**Authentication** 
1. Practitioner ID 
2. Keycloack  - secondary practitioner identification 


**QA**
1. Unit tests 
2. Integration tests
3. [https://app.codecov.io/gh/opensrp/fhircore](https://app.codecov.io/gh/opensrp/fhircore) 
