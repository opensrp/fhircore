[![Android CI with Gradle](https://github.com/OpenSRP/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/OpenSRP/fhircore/actions/workflows/ci.yml)
[![Coverage Status](https://coveralls.io/repos/github/opensrp/fhircore/badge.svg?branch=main)](https://coveralls.io/github/opensrp/fhircore?branch=main)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b8c108d9cd4c40aeb379cbcd3c3b2400)](https://www.codacy.com/gh/opensrp/fhircore/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=opensrp/fhircore&amp;utm_campaign=Badge_Grade)

# FHIRcore

<img align=center width=400 src="../docs/assets/fhircore.png">

Next generation OpenSRP FHIR native digital health platform powered by the [Google FHIR SDK](https://github.com/google/android-fhir).


## Build instructions

One needs a [GitHub token](https://docs.github.com/en/github/authenticating-to-github/creating-a-personal-access-token#creating-a-token) to pull packages from https://github.com/orgs/google/packages?repo_name=android-fhir.

While creating the token, check the read:packages permission.

Add your token details in this file ~/.gradle/gradle.properties in the format below:


```
GITHUB_USER=Xxxx
GITHUB_PERSONAL_ACCESS_TOKEN=xxxxxx
```

or export them to your system environment variables:

```
export GITHUB_USER=Xxxx
export GITHUB_PERSONAL_ACCESS_TOKEN=xxxxxx
```

You also need keycloak credentials to authenticate FHIR APIs and login. Following properties should be added to ~/local.properties

```
OAUTH_BASE_URL=https://keycloak-stage.smartregister.org/auth/realms/FHIR_Android/
OAUTH_CIENT_ID=fhir-core-client
OAUTH_CLIENT_SECRET=XXXXXXX
OAUTH_SCOPE=openid
FHIR_BASE_URL=https://fhir.labs.smartregister.org/fhir/
```
