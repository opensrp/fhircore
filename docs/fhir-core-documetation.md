[![Android CI with Gradle](https://github.com/opensrp/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/opensrp/fhircore/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/opensrp/fhircore/branch/main/graph/badge.svg?token=IJUTHZUGGH)](https://codecov.io/gh/opensrp/fhircore)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ee9b6f38b7294fa3aa668e42e52fdf21)](https://www.codacy.com/gh/opensrp/fhircore/dashboard)

# OpenSRP FHIR Core

### Introduction

FHIR Core is a Kotlin application for delivering configurable,offline-capable, mobile-first healthcare project implementations from local community to national and international scale using FHIR and the WHO Smart Guidelines on Android.

FHIR Core is architected as a FHIR native digital health platform powered by Google's [Android FHIR SDK](https://github.com/google/android-fhir) and [HAPI FHIR](https://hapifhir.io/). FHIR Core users experience a module oriented design based on over a decade of real world experience implementing digital health projects with [OpenSRP](https://smartregister.org/). This repository contains the Android mobile application built to:

- Load configuration data as FHIR resources
- Support the WHO Smart Guidelines
- Manage the identities of healthcare workers (HCWs), community health workers (CHWs), care teams, patients, and clients
- Collect, view, and edit healthcare data with dynamic forms using FHIR's [Structured Data Capture](https://hl7.org/fhir/us/sdc/index.html) (SDC) implementation
- Securely store healthcare data encrypted at rest and securely transmit healthcare data using TLS
- Manage location hierarchies defined by community to national and international administrative boundaries

For remote data storage and login, the mobile application requires:
- A [Keycloak](https://www.keycloak.org/) server to manage identity, authentication, and authorization;
- A [HAPI FHIR](https://hapifhir.io/) server to store operation and configuration data that includes the [HAPI FHIR to Keycloak integration](https://github.com/opensrp/hapi-fhir-keycloak).

FHIR Core also interoperates well with:
- [OpenSRP Web](https://github.com/OpenSRP/web) to access healthcare data from the same HAPI FHIR server.

[//]: # (<center><img width=400 src="assets/fhircore.png"/></center>)

### About this repository

This repository contains the following directories:
* **[android](android)**: for building the Android application.
* **[docs](docs)**: a library of documents describing the FHIR Core solution.

We recommend reviewing the [docs](docs) before setting up the Android Studio Project in the [android](android) folder.


#### Programming Language

Kotlin -[v1.7.10](https://kotlinlang.org/)

#### Android libraries used

1. [Hilt](https://developer.android.com/training/dependency-injection/hilt-multi-module)- for dependency injection  
2. [Jetpack Compose](https://developer.android.com/jetpack/compose/documentation) - for building sharable declarative Android UI
3. [Jetpack Compose navigation](https://developer.android.com/jetpack/compose/navigation) - to navigate between compose screens
4. [Android navigation component](https://developer.android.com/guide/navigation) - to navigate between activities and fragments
5. Android Livedata and ViewModel


#### Architecture

The app is architectured in the following manner:
1. The app is built around MVVM architecuture with the data layer implemented using the Repository pattern. 
2. The entry point of the application also follows Single-Activity architecture after the user is logged in.

#### Data access

The application uses FHIR Engine APIs from Google's [Android FHIR SDK](https://github.com/google/android-fhir) (which internally uses 
[Room](https://developer.android.com/jetpack/androidx/releases/room) libary) to access the local Sqlite database.


#### Configurations

The application syncs particular resources (conventionally, Composition and Binary) from the HAPI FHIR server to configure the app. The configurations control application workflows as well as the look and feel of the app.


#### Tests

This repository also includes:
1. Unit tests 
2. UI and integration tests


Review the [docs](docs) for more details on the app architecture, configuration, data access and testing.