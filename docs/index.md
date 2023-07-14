---
sidebar_position: 1
sidebar_label: Overview
---

# Overview

OpenSRP is a FHIR-native medical record system for health workers to manage their patients for child health, maternal health and more. OpenSRP is developed by Ona, a Kenyan social enterprise, with contributions from organizations around the world.

OpenSRP 2, released in 2023, combines WHO health workflows with FHIR to transform how healthcare is delivered and managed.

OpenSRP 2 has three parts: a mobile app for Android devices, a web-based Admin Dashboard, and an Analytics Dashboard.

#### OpenSRP Android App

![](/img/overview-app.png)

The OpenSRP Android app is used by health workers to:
1. Enroll community members to a medical record system.
2. Turn community members into patients by adding them to a care plan associated with their condition.
3. Set future services a patient should receive based on their care plan, and assign making sure the services happen to health team members as tasks.
4. Clearly visualize overdue patients so the health team can return them to care.
5. Track performance of providing services to patients at individual health care practitioner and facility levels.

[Android App features](/about-opensrp/app-features)


#### OpenSRP Admin Dashboard

The OpenSRP Admin Dashboard is used by health administrators and project managers to:
1. Add, edit and remove health worker user accounts.
2. Manage health team organization structure such. as locations, facilities, and line of reporting.
3. View patient information.

[Admin Dashboard features](/about-opensrp/admin-dashboard-features)

#### OpenSRP Analytics Dashboard

The OpenSRP Analytics Dashboard is used by health administrators, project managers, and M&E analysts to:
1. View reporting at many aggregation levels, from system wide down to a single health worker.
2. Access to data warehouse.

#### Technology

The Android app is primarily written in Kotlin​, architected as a FHIR-native platform, and powered by [Android FHIR SDK](https://github.com/google/android-fhir).

OpenSRP on Github: 
- [Android App Github](https://github.com/opensrp/fhircore)
- [Admin Dashboard Github](https://github.com/opensrp/web)

In addition to the Android App, Admin Dashboard, and Analytics Dashboard, OpenSRP uses a number of tools as described in the diagram below:

![](/img/opensrp-platform-flow.png)