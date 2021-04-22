[![Android CI with Gradle](https://github.com/OpenSRP/fhircore/actions/workflows/ci.yml/badge.svg)](https://github.com/OpenSRP/fhircore/actions/workflows/ci.yml)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/36e296c5bea343e1ac451d66a2331d11)](https://www.codacy.com/app/OpenSRP/fhircore?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=OpenSRP/fhircore&amp;utm_campaign=Badge_Grade)


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
