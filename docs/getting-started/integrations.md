# Integrations

## FHIR Data Store

OpenSRP relies on a FHIR data store to serve as a source of truth for all data, both configuration data and transactional health record data.

### HAPI

OpenSRP can be made to work with any FHIR data store but out of the box assumes extensions to the default FHIR API defined by a set of [HAPI FHIR extensions](https://github.com/opensrp/hapi-fhir-opensrp-extensions).

### Google Cloud Healthcare API

When extended by these [HAPI FHIR extensions](https://github.com/opensrp/hapi-fhir-opensrp-extensions) the Cloud Healthcare API can be used as the data and configuration store for OpenSRP.

## Data warehouse and analytics database

Although not required, we highly recommend replicating your transactional health data to a data warehouse to monitor and explore the data in your OpenSRP Healthcare projects.

### Google Cloud Healthcare API and Big Query

A simple way and efficient way to move FHIR data from a transaction system into an analytics system is to replicate your data to the Cloud Healthcare API and then connect this data to Big Query.

### Parquet and Spark SQL

For on premise implementations the [FHIR Data Pipes](https://github.com/google/fhir-data-pipes) library

## Identity Management



### Keycloak

### FHIR Access Proxy

## Web data viewer

### FHIR Web
