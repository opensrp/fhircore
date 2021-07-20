[![Android CI with Gradle](https://github.com/OpenSRP/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/OpenSRP/fhircore/actions/workflows/ci.yml)
[![Coverage Status](https://coveralls.io/repos/github/opensrp/fhircore/badge.svg?branch=main)](https://coveralls.io/github/opensrp/fhircore?branch=main)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b8c108d9cd4c40aeb379cbcd3c3b2400)](https://www.codacy.com/gh/opensrp/fhircore/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=opensrp/fhircore&amp;utm_campaign=Badge_Grade)

# OpenSRP FHIR Core

FHIRcore is a Kotlin application for delivering offline-capable, mobile-first healthcare project implementations from local community to national and international scale using FHIR and WHO Smart Guidelines on Android.

FHIRcore is architected as a FHIR native digital health platform powered by Google's [Android FHIR SDK](https://github.com/google/android-fhir). FHIRcore's user experience and module oriented design are based on [OpenSRP](https://smartregister.org/). This repository contains the Android mobile application built to load configuration data as FHIR resources and support the WHO Smart Guidelines.

For remote data storage and login, the mobile application requires:
- a [Keycloak](https://www.keycloak.org/) server to manage identity, authentication, and authorization
- a [HAPI FHIR](https://hapifhir.io/) server to store operation and configuration data that includes the [HAPI FHIR to Keycloak integration](https://github.com/opensrp/hapi-fhir-keycloak).

FHIRcore also interoperates well with:
- [OpenSRP Web](https://github.com/OpenSRP/web) to access health data from the same HAPI FHIR server

<img align=center width=400 src="docs/assets/fhircore.png">

## Getting Started
This repository contains the folders
* **[android](android)**: for building the Android application.
* **[docs](docs)**: a library of documents describing the FHIRcore solution.

We recommend reviewing the [docs](docs) before setting up the Android Studio Project in the [android](android) folder.
