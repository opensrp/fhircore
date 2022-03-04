[![Android CI with Gradle](https://github.com/opensrp/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/opensrp/fhircore/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/opensrp/fhircore/branch/main/graph/badge.svg?token=IJUTHZUGGH)](https://codecov.io/gh/opensrp/fhircore)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ee9b6f38b7294fa3aa668e42e52fdf21)](https://www.codacy.com/gh/opensrp/fhircore/dashboard)

# FHIR Core

<img align=center width=400 src="../docs/assets/fhircore.png">

## Build instructions

### local.properties

If you would like to log into remote servers and authenticate against remote FHIR APIs, you will need Keycloak credentials. For this, add the following properties to `~/local.properties`:

```
OAUTH_BASE_URL=https://keycloak-stage.smartregister.org/auth/realms/FHIR_Android/
OAUTH_CIENT_ID=fhir-core-client
OAUTH_CLIENT_SECRET=xxxxxx
OAUTH_SCOPE=openid
FHIR_BASE_URL=https://fhir.labs.smartregister.org/fhir/
```

### Keystore credentials

In order for the `assembleRelease` and/or `bundleRelease` Gradle task to work e.g. to generate a signed release version of you APK (or AAB), you need to generate a keystore.

To generate your own release keystore you can use the `keytool` utility (installed as part of the java runtime) by running the the command:

`keytool -genkey -v -keystore fhircore.keystore.jks -alias <my_alias_name> -keyalg RSA -keysize 4096 -validity 1000`

Place the Keystore file in your _user(home)_ directory i.e. `/Users/username/fhircore.keystore.jks` or `~/fhircore.keystore.jks`

You then need to create the following _System variables_ and set the corresponding values `KEYSTORE_ALIAS`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`
**Note:** The values used in generating the keystore will be the values assigned to the system properties above. Also note, if your platform doesn't prompt you for a second password when generating the Keystore (e.g. of type PKCS12) then the KEYSTORE_PASSWORD and KEY_PASSWORD values will be the same.

- For more on the `keytool` utility see: [Java Key and Certificate Management Tool](https://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html)
- For more on signing your application see: [Signing your Android app](https://developer.android.com/studio/publish/app-signing)

## Application architecture

FHIR Core is based on MVVM Android application architecture. It follows the recommended [Repository Pattern](https://developer.android.com/jetpack/guide) in the architecture. The diagram below shows the different layers of the application structure and how they interact with each other. At the core is Android FHIR SDK which provides Data Access API, Search API, Sync API, Smart Guidelines API and Data Capture API. Refer to [FHIR Core Docs](https://github.com/opensrp/fhircore/tree/main/docs) for more information.

<img align="center" width="800" height="600" src="../docs/assets/fhircore-app-architecture.png">

## Project Structure

The project currently consists of two application modules (`anc` and `eir`) and one shared Android library module (`engine`) . The `engine` module contains shared code for both `anc` and `eir` applications, including: code for authenticating users, shared UI code, application and view configuration contracts, domain models and shared utility functions. The `engine` module project structure and implementation details are discussed intensively on the module's `README.MD` file.

## Package structure

Both `anc` and `eir` Android application modules have their packages grouped based on features. `engine` module on the other hand uses a hybrid approach, combining both layered and feature based package structure.

At a higher level every module is at least organized into three main packages, namely:

- `data`
- `ui`
- `util`

Conventionally, classes are further organized into more cohesive directories within the main packages mentioned above. This should allow for minimal updates in the code base when code is refactored by moving directories.

### Root level package description

#### `data`

This package is used to hold classes/object or any implementations used to interact with local database via the `FhirEngine` implementation provided via the Android FHIR SDK. This package will mostly hold the `Repository` and `Model` classes for the application modules. The `data` package for `engine` module is further sub-divided into two sub-packages that is `local` and `remote`. `local` directory holds the implementation for accessing the `Sqlite` database. `remote` directory contains implementation for making `http` requests to HAPI FHIR server backend.

#### `ui`

This package mostly contains Android `Activity`, `Fragment`, `ViewModel` `View` classes or any custom implementations that involve data presentation. The views should conventionally be grouped based on their purposed, e.g. is a family package that has both register and details packages for showing a list of all families and individual family details respectively.

#### `util`

This package is used to hold any internally shared utility methods usually implemented as Kotlin extensions with a few exceptions where Kotlin `object` is used to implement singleton classes.

## Code sharing

The `engine` module provides code that is shared across the supported applications. Every implementation in the `engine` module that is not common should be implemented as abstract base classes and overridden in the application modules. An example is the `BaseLoginActivity` (Most abstract classes in `engine` module conventionally starts with the word `Base`) that is extended in both application modules to provide specific configurations for login.

Utility methods are conventionally implemented as Kotlin extensions. Others are implemented as singletons using Kotlin `object`

## Configuration

FHIR Core supports application configurations through contracts. These contracts MUST be implemented by Android `Application` and any user interface related classes like `Activity`, `Fragment` and Android `View` classes. This allows for passing application level configurations synced from the server or configurations that dictate how data is presented to the user, i.e. which parts of the screen to show or hide, which screens to show, which UI elements to disable etc.

### Application configuration

Application level configuration is provided through `ConfigurableApplication` contract. `ConfigurableApplication` exposes the following properties that need to be implemented.

| Property                   | Description                                                                                                                                                       |
| :------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `applicationConfiguration` | Used to set Application level configuration. Typically a Kotlin data class                                                                                        |
| `authenticationService`    | A singleton instance of `AuthenticationService` used for handling user authorization and authentication using Android `AccountManager` API                        |
| `fhirEngine`               | Provides an instance of `FhirEngine` from Android FHIR SDK                                                                                                        |
| `secureSharedPreference`   | Sets a singleton of `SecureSharedPreference` used to access encrypted shared preference data                                                                      |
| `resourceSyncParams`       | Provides resource sync parameters needed for syncing data to and from FHIR server                                                                                 |
| `syncBroadcaster`          | Sets a singleton instance of `SyncBroadcaster` that is used to react to FHIR server sync states. Any view can register to this broadcaster to receive sync states |
| `workerContextProvider`    | Loads `SimpleWorkerContext` required for StructureMap-based extraction                                                                                            |

`ConfigurableApplication` also exposes a `configureApplication` method that accepts an instance of `ApplicationConfiguration` used to configure the application. This method should be called on the `onCreate` method of the `Application` class.

### View configuration

FHIR Core view configurations are provided through two contracts `ConfigurableComposableView` and `ConfigurableView`. `ConfigurableComposableView` is used to configure Android views created using Jetpack Compose. View configurations are used to show or hide views, change view content, change view background etc. Each of the two contracts exposes `configureViews` method that accepts view `Configuration` as an argument. This method should be called when the `Activity` or `Fragment` is created. It is also recommended to update the view configurations via a `ViewModel` so they can be observed whenever the configurations are changed.

## Resources

- [Code Documentation](https://fhircore.smartregister.org/) - Access FHIR Core code documentation
- [Developer Guidelines](https://github.com/opensrp/fhircore/wiki) - Get started with FHIR Core
- [FHIR Core Docs](https://github.com/opensrp/fhircore/tree/main/docs) - Read FHIR Core Documentation
- [Android App Architecture Guide](https://developer.android.com/jetpack/guide) - Learn more about Android App Architecture
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Learn more about Jetpack Compose
