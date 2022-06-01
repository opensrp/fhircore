[![Android CI with Gradle](https://github.com/opensrp/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/opensrp/fhircore/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/opensrp/fhircore/branch/main/graph/badge.svg?token=IJUTHZUGGH)](https://codecov.io/gh/opensrp/fhircore)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ee9b6f38b7294fa3aa668e42e52fdf21)](https://www.codacy.com/gh/opensrp/fhircore/dashboard)

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

For starter resources on the FHIR specification:

1. [Intro to FHIR](https://youtu.be/YbQcJj1GqH0) - By James Agnew of Smile CDR
1. [FHIR resource list](http://hl7.org/fhir/resourcelist.html)

For starter resources on the Android FHIR SDK and this repo:

1. Android FHIR SDK Demo - [Link](https://drive.google.com/file/d/1ORjk3pNOKjGyZlbayAViPy4xkI8p-aFB/view?usp=sharing)
1. Android FHIR SDK Intro Slide deck - [Link](https://docs.google.com/presentation/d/1oc6EBJAcXsBwyBgtnra61xoM7naBF0aj8WbfrUT7Y2A)
1. FHIR Core Intro slide deck - [Link](https://docs.google.com/presentation/d/1eFsf9a5dcNqKXlfsEWyNZyoVooIbK3jq6694xarARr8)
1. General FHIR Modelling for LMIC resource mapping- [Link](https://docs.google.com/drawings/d/1mRESJI_YywQG-YjhBy4zvVik28gBj2n_YscWKZZ_wOc)
1. List of commonly used [acronyms](https://docs.google.com/document/d/13FG5QLCcNZEaQGX_9IYcIJc_UbbmllrwQwcp3TMG9Tk/edit?usp=sharing)
