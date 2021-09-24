[![Android CI with Gradle](https://github.com/opensrp/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/opensrp/fhircore/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/opensrp/fhircore/branch/main/graph/badge.svg?token=IJUTHZUGGH)](https://codecov.io/gh/opensrp/fhircore)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b8c108d9cd4c40aeb379cbcd3c3b2400)](https://www.codacy.com/gh/opensrp/fhircore/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=opensrp/fhircore&amp;utm_campaign=Badge_Grade)

# OpenSRP FHIR Core

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

FHIRcore also interoperates well with:
- [OpenSRP Web](https://github.com/OpenSRP/web) to access healthcare data from the same HAPI FHIR server.

<img align=center width=400 src="docs/assets/fhircore.png">

## Getting Started
This repository contains the folders
* **[android](android)**: for building the Android application.
* **[docs](docs)**: a library of documents describing the FHIR Core solution.

We recommend reviewing the [docs](docs) before setting up the Android Studio Project in the [android](android) folder.
