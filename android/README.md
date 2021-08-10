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
