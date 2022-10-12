[![Android CI with Gradle](https://github.com/opensrp/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/opensrp/fhircore/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/opensrp/fhircore/branch/main/graph/badge.svg?token=IJUTHZUGGH)](https://codecov.io/gh/opensrp/fhircore)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ee9b6f38b7294fa3aa668e42e52fdf21)](https://www.codacy.com/gh/opensrp/fhircore/dashboard)

# FHIR Core

<center><img width=400 src="../docs/assets/fhircore.png"></center>

## Build instructions

### local.properties

If you would like to log into remote servers and authenticate against remote FHIR APIs, you will need Keycloak credentials. For this, add the following properties to `local.properties`:

```
OAUTH_BASE_URL=https://keycloak-stage.smartregister.org/auth/realms/FHIR_Android/
OAUTH_CIENT_ID="provide client id"
OAUTH_CLIENT_SECRET="provide client secret"
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

You can also choose to store the above credentials in a file named `keystore.properties`

```
KEYSTORE_PASSWORD=xxxxxx
KEYSTORE_ALIAS=xxxxxx
KEY_PASSWORD=xxxxxx
```

**Note** When using this approach to store credentials please remember to add a `keystore.properties` entry to the `.gitignore` file to prevent versioning on git

- For more on the `keytool` utility see: [Java Key and Certificate Management Tool](https://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html)
- For more on signing your application see: [Signing your Android app](https://developer.android.com/studio/publish/app-signing)


## Application architecture

FHIR Core is based on MVVM Android application architecture. It follows the recommended [Repository Pattern](https://developer.android.com/jetpack/guide) in the architecture. The diagram below shows the different layers of the application structure and how they interact with each other. At the core is Android FHIR SDK which provides Data Access API, Search API, Sync API, Smart Guidelines API and Data Capture API. Refer to [FHIR Core Docs](https://github.com/opensrp/fhircore/tree/main/docs) for more information.

<center><img width="800" height="600" src="../docs/assets/fhircore-app-architecture.png"></center>


## Project Structure

The project currently consists of single application module (`quest`)and an two Android library modules (`engine` and `geowidget`). 


## Package structure

`quest` application module packages are grouped based on features. `engine` module on the other hand uses a hybrid approach, combining both layered and feature based package structure.

At a higher level every module is at least organized into three main packages, namely:

- `data`
- `ui`
- `util`

Conventionally, classes are further organized into more cohesive directories within the main packages mentioned above. This should allow for minimal updates in the code base when code is refactored by moving directories.

### Root level package description

#### `data`

This package is used to hold classes/object or any implementations used to interact with local database via the `FhirEngine` implementation provided via the Android FHIR SDK. This package will mostly hold the `Repository` and `Model` classes for the application modules. The `data` package for `engine` module is further sub-divided into two sub-packages that is `local` and `remote`. `local` directory holds the implementation for accessing the `Sqlite` database. `remote` directory contains implementation for making `http` requests to HAPI FHIR server backend.

#### `ui`

This package mostly contains Android `Activity`, `Fragment`, `ViewModel`, and `Composable` functions for rendering UI.

#### `util`

This package is used to hold any internally shared utility methods usually implemented as Kotlin extensions with a few exceptions where Kotlin `object` is used to implement singleton classes.


## Resources

Refer to the following links for more details:

- [Code Documentation](https://fhircore.smartregister.org/) - Access FHIR Core code documentation
- [Developer Guidelines](https://github.com/opensrp/fhircore/wiki) - Get started with FHIR Core
- [FHIR Core Docs](https://github.com/opensrp/fhircore/tree/main/docs) - Read FHIR Core Documentation
- [Android App Architecture Guide](https://developer.android.com/jetpack/guide) - Learn more about Android App Architecture
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Learn more about Jetpack Compose
