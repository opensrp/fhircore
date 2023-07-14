---
sidebar_position: 3
---


# Platform Components

### FHIR Data Store

OpenSRP relies on a FHIR data store to serve as a source of truth for all data, both configuration data and transactional health record data.

#### HAPI

OpenSRP can be made to work with any FHIR data store but out of the box assumes extensions to the default FHIR API defined by a set of open source [HAPI FHIR extensions](https://github.com/opensrp/hapi-fhir-opensrp-extensions).

#### Google Cloud Healthcare API

When extended by these [HAPI FHIR extensions](https://github.com/opensrp/hapi-fhir-opensrp-extensions) the Cloud Healthcare API can be used as the data and configuration store for OpenSRP.

### Data warehouse and analytics database

Although not required, we highly recommend replicating your transactional health data to a data warehouse to monitor and explore the data in your OpenSRP Healthcare projects.

#### Google Cloud Healthcare API and Big Query

A simple way and efficient way to move FHIR data from a transaction system into an analytics system is to replicate your data to the Cloud Healthcare API and then connect this data to Big Query.

#### Parquet and Spark SQL

For on premise implementations the [FHIR Data Pipes](https://github.com/google/fhir-data-pipes) is an open source library can synchronize FHIR resources from HAPI to flat-files then transform those files to a relational format that is queryable using SQL.

### Identity and Access Management

OpenSRP connects to a third party identity and access management (IAM) system to authenticate and authorize users of the system.

#### FHIR Access Proxy

The [FHIR Access Proxy](https://github.com/google/fhir-access-proxy) is an open source endpoint agnostic interface between an IAM and a health store.

#### Keycloak

[Keycloak](https://www.keycloak.org/) is an open source IAM that stores users, groups, and the access roles of those groups.

### Web data viewer

This allows the user to view data in a health data store and perform common editing and management tasks on this data.

#### FHIR Web

The open source [FHIR Web](https://github.com/opensrp/web) web application allows you to view FHIR resources, create new FHIR resources, and manage user access to FHIR systems through FHIR Practitioner resources associated with an IAM.
