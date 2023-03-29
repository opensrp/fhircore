# Configurations

## Jump to section
* [Introduction](#introduction)
* [Configuration Types](#configuration-types)
* [Config Conventions](#config-conventions)
* [FAQs](#faqs)
* [Troubleshooting](#troubleshooting)

## Introduction

A FHIR Core application needs configurations to work properly. These configurations are used to define application workflows (or events), user interface content, which user interface component to display and which data (FHIR Resources) to load.

FHIR Core configs are represented using two types of FHIR resources ([Composition](https://build.fhir.org/composition.html) and [Binary](https://build.fhir.org/binary.html)) which are stored in JSON format.  A Binary  resource can contain any content including custom JSONs modeled for FHIR Core's use case. These Binary resources are referenced in the `Composition` resource sections; each Binary resource representing a valid configuration. A `Composition` section can nested in other sections; commonly used to group related configs. 

A `Composition` resource acts as the application  config manifest. It MUST have a unique identifier to differentiate the application from the rest of the applications sharing the same server.


## Configuration Types

FHIR Core supports the following types of configurations:

|Config type| Function  | Cardinality |
|--|--|:--:|
|composition| Uniquely identifiers the application. References other configurations | 1..1 |
| application | Provides application level configurations e.g. languages used in the app, title of the app | 1..1 |
| navigation | Configures the app's navigation side menu (drawer). e.g. how many registers to display | 1..1|
| register | Configures a register (what resources to use, which views to render etc) | 1..* |
|profile | Configures a profile (what resources to use and how the views are to be rendered) | 1..* |
|sync | Configures the resources to be synced to and from the server | 1..1|
|geoWidget | Configures the geowidget map view | 1..* |
| measureReport | Configures the various measure report indicators used in the app | 1..1 |

>NOTE: Cardinality chart: 1..1 ( only one required); 1..* (can be multiple);  0..1 (optional)


## Config Conventions

 - Config attributes and types  should be in camelCase e.g. `application.deviceToDeviceSync`
 - Some config values are expected to be `enum classes`  constants. They MUST be provided in UPPER_SNAKE_CASE e.g the valid `ServiceStatus` values include: `DUE`, `OVERDUE`, `UPCOMING` and `COMPLETED`
 - Computed configuration values use `@{placeholder}` format to reference the name of the computed rule. The placeholder will be replaced with an actual value at runtime.

## FAQs
1. How do I come up with a name for the app id: _app Ids should be short, human readable and easy to remember. Different application should not share the same app id. Examples of good app Ids: quest, g6pd etc. You can derive an app Id after the name of the client project._

2. Can I share configurations between two apps?: _yes and no. Yes when the configs are available in the same server. This is however not recommended_

## Troubleshooting
- `Missing Resource ` - Check whether the referenced Binary resource exists in the server. Could be a typo in the Composition resource or you forgot include the Binary resource to the Composition section.
- `Error loading configuration. Details : Key application is missing in the map` - Composition resource for the given application ID is missing.