---
sidebar_label: Custom Endpoints
---

# OpenSRP Custom Endpoints

OpenSRP has developed custom endpoints to extend the functionality provided by HAPI and other FHIR servers. These endpoints address specific challenges faced during the development and deployment of OpenSRP's Mobile and Web clients. The two primary custom endpoints are:

- **`LocationHierarchy`**: Enhances location management by providing hierarchical structures and additional metadata for locations.
- **`PractitionerDetail`**: Offers detailed information about practitioners, including roles, affiliations, and other relevant data.

These endpoints are designed to solve unique problems encountered in OpenSRP's ecosystem, ensuring seamless integration and improved performance for client applications.

## Where are they implemented

The custom endpoints, **`LocationHierarchy`** and **`PractitionerDetail`**, are implemented as **plugins** on the **FHIR Info Gateway**. These plugins can be found in the [FHIR Gateway Extension repository](https://github.com/onaio/fhir-gateway-extension).
To ensure consistency and interoperability between the clients (Mobile and Web) and the FHIR Info Gateway, OpenSRP has developed a shared [library](https://github.com/opensrp/fhir-common-utils). This library provides common utilities and data models, enabling seamless communication and data exchange across the system.

## Accessing FHIR and Custom Endpoints with the New Gateway

With the recent refactor in the gateway-plugin repository, accessing FHIR and custom endpoints through the new gateway has undergone changes. This section outlines the updated approach for accessing different types of endpoints.

### FHIR Endpoints

When using the gateway, it is now mandatory to include the `/fhir/` part in the URL when accessing FHIR endpoints. This adjustment aligns our structure with Google's gateway.

Example: `https://gateway.example.com/fhir/Patient`

### Custom Endpoints

For custom endpoints such as `/PractitionerDetail` and `/LocationHierarchy`, there is no need to include the `/fhir/` part. Directly use the endpoint in the URL:

This approach ensures consistency and clarity when accessing various endpoint types through the gateway.