[![Android CI with Gradle](https://github.com/opensrp/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/opensrp/fhircore/actions/workflows/ci.yml)
[![Coverage Status](https://coveralls.io/repos/github/opensrp/fhircore/badge.svg?branch=main)](https://coveralls.io/github/opensrp/fhircore?branch=main)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b8c108d9cd4c40aeb379cbcd3c3b2400)](https://www.codacy.com/gh/opensrp/fhircore/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=opensrp/fhircore&amp;utm_campaign=Badge_Grade)

# FHIR Core

<img align=center width=400 src="../docs/assets/fhircore.png">

## Build instructions

You need a [GitHub token](https://docs.github.com/en/github/authenticating-to-github/creating-a-personal-access-token#creating-a-token) to pull packages from the [Android FHIR SDK repo](https://github.com/orgs/google/packages?repo_name=android-fhir).

When creating the token, select the `read:packages` permission.

Add your token details to the file `~/.gradle/gradle.properties` in the format below:


```
GITHUB_USER=xxxxxx
GITHUB_PERSONAL_ACCESS_TOKEN=xxxxxx
```

or export them to your system environment variables with the commands:

```sh
export GITHUB_USER=xxxxxx
export GITHUB_PERSONAL_ACCESS_TOKEN=xxxxxx
```

If you would like to log into remote servers and authenticate against remote FHIR APIs, you will need Keycloak credentials. For this, add the following properties to `~/local.properties`:

```
OAUTH_BASE_URL=https://keycloak-stage.smartregister.org/auth/realms/FHIR_Android/
OAUTH_CIENT_ID=fhir-core-client
OAUTH_CLIENT_SECRET=xxxxxx
OAUTH_SCOPE=openid
FHIR_BASE_URL=https://fhir.labs.smartregister.org/fhir/
```

## Project Structure

The project currently consists of two application modules (`anc` and `eir`)  and one shared Android library module (`engine`) . The `engine` module contains shared code for both `anc` and `eir` applications, including: code for authenticating users, shared UI code, application and view configuration contracts, domain models and shared utility functions. The `engine` module project structure and implementation details are discussed intensively on the module's `README.MD` file.

## Package structure

Both `anc` and `eir` Android application modules have their packages grouped based on features. `engine` module on the other hand uses a hybrid approach, combining both layered and feature based package structure.

At a higher level every module is at least organized into three main packages, namely:

- `data`
- `ui`
- `util`

Conventionally, classes are further organized into more cohesive directories within the main packages mentioned above. This should allow for minimal updates in the code base when code is refactored by moving directories.

### Root level package description

#### `data`

This package is used to hold classes/object or any implementations used to interact with local database via the `FhirEngine` implementation provided via the Android FHIR SDK.  This package will mostly hold the `Repository` and `Model` classes for the application modules. The `data` package for `engine` module is further sub-divided into two sub-packages that is `local` and `remote`. `local` directory holds the implementation for accessing the `Sqlite` database. `remote` directory contains implementation for making `http` requests to HAPI FHIR server backend.

#### `ui`

This package mostly contains Android `Activity`, `Fragment`, `ViewModel` `View`  classes or any custom implementations that involve data presentation. The views should conventionally be grouped based on their purposed, e.g. is a family package that has both register and details packages for showing a list of all families and individual family details respectively.

#### `util`
This package is used to hold any internally shared utility methods usually implemented as Kotlin extensions with a few exceptions where Kotlin `object` is used to implement singleton classes.


## Code sharing

The `engine` module provides code that is shared across the supported applications. Every implementation in the `engine` module that is not common should be implemented as abstract base classes and overridden in the application modules. An example is the `BaseLoginActivity` (Most abstract classes in `engine`  module conventionally starts with the word `Base`) that is  extended in both application modules to provide specific configurations for login.

Utility methods are conventionally implemented as Kotlin extensions. Others are implemented as singletons using Kotlin `object`

## Configuration

FHIR Core supports application configurations through contracts. These contracts MUST be implemented by Android `Application` and any user interface related classes like `Activity`, `Fragment` and Android `View` classes. This allows for passing application level configurations synced from the server or configurations that dictate how data is presented to the user, i.e. which parts of the screen to show or hide, which screens to show, which UI elements to disable etc.

### Application configuration

### View configuration
