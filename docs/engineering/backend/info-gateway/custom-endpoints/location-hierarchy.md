---
title: Location Hierarchy
---

# OpenSRP Location Hierarchy Endpoint

The OpenSRP 2.0 Location Hierarchy endpoint is designed to efficiently retrieve and manage hierarchical location data within the OpenSRP system. The location hierarchy is defined by the `Location.partOf` property on the [Location resource](https://hl7.org/fhir/R4B/location.html) in the FHIR specification.

## 1. Data Retrieval Process

### Overview
The endpoint is designed to return data in two formats: a tree (hierarchical) format or a list format. This is controlled by a `mode` parameter. This endpoint is built to return the hierarchy starting from the provided IDs as the root to all its children. Below are the parameters allowed by the endpoint:
- `_id` - This is the ID of the location for which you want the hierarchy. Example: `[GET] /LocationHierarchy?_id=<some-location-id>` 
- `_syncLocations` - This parameter holds IDs of locations selected by the users. The locations are comma-separated. Example: `[GET] /LocationHierarchy?_syncLocations=<some-location-id,some-location-id>`
- `administrativeLevelMin` - Specifies the minimum administrative level to include in the response. Locations at this level and above will be included. Example: `[GET] /LocationHierarchy?_id=<some-location-id>&administrativeLevelMin=2&administrativeLevelMax=4`
- `administrativeLevelMax` - Specifies the maximum administrative level to include in the response. Locations at this level and below will be included. Example: `[GET] /LocationHierarchy?_id=<some-location-id>&administrativeLevelMin=2&administrativeLevelMax=4`
- `filterInventory` - A boolean parameter that specifies whether the response should be filtered by locations with inventories. Example: `[GET] /LocationHierarchy?_id=<some-location-id>&filterInventory=true`
- `_lastUpdated` - This filter allows users to retrieve locations based on the last modification date, making it useful for tracking recent updates or syncing data changes over time. Example: `[GET] /LocationHierarchy?_id=<some-location-id>&mode=list&_lastUpdated=2024-09-22T15%3A13%3A53.014%2B00%3A00`
- `_summary` - This allows users to retrieve the total number of matching resources without returning the resource data. Example: `[GET] /LocationHierarchy?_id=<some-location-id>&mode=list&_summary=count`
- `mode` - A parameter to switch between the two response formats. Example: `[GET] /LocationHierarchy?_id=<some-location-id>&mode=list`


#### LocationHierarchy List Mode

The LocationHierarchy endpoint supports two response formats: tree and list. By default, the response format remains a tree, providing hierarchical location data. In addition, clients can request the endpoint to return location resources in a flat list format by providing a request parameter `mode=list`.

Example: 
```[GET] /LocationHierarchy?_id=<some-location-id>&mode=list&_count=<page-size>&_page=<page-number>&_sort=<some-sort>```


#### LocationHierarchy Dynamic Identifier

The `LocationHierarchy` endpoint has the following supported functionalities when the `_id` is not provided as a parameter:

- Build location hierarchies of the **_User Assigned Locations_**: The `LocationHierarchy` endpoint will build location hierarchies of all user-assigned locations. Example: `[GET] /LocationHierarchy`
- Build location hierarchies of the **_User Selected Locations_**: The `LocationHierarchy` endpoint will build location hierarchies of the locations provided by the user via the `_syncLocations` parameter.
  - ##### Conditions for User Selected Location Hierarchies
    - The deployment/app user should have Related Entity Location as their sync strategy.
    - The deployment/app user should have the `ALL_LOCATIONS` role on Keycloak.
    - The request should have the `_syncLocations` parameter set.

Example: 
```[GET] /LocationHierarchy?_syncLocations=<some-location-id>,<some-location-id>,<some-location-id>```

All other valid parameters can be used on this endpoint.


#### LocationHierarchy Administrative Level Filters

The LocationHierarchy endpoint supports filtering by administrative levels. This is useful for querying locations at specific levels within the hierarchy. The following search parameters are available:

- `administrativeLevelMin`: Specifies the minimum administrative level to include in the response. Locations at this level and above will be included.
- `administrativeLevelMax`: Specifies the maximum administrative level to include in the response. Locations at this level and below will be included. If not set, it defaults to the value of `DEFAULT_MAX_ADMIN_LEVEL` set in the [`Constants.java`](https://github.com/onaio/fhir-gateway-extension/blob/d38a787d082bf1ed8891ec808fd713326c3bfcb1/plugins/src/main/java/org/smartregister/fhir/gateway/plugins/Constants.java#L31) file.

##### Behavior Based on Parameters:
- No Parameters Defined: The endpoint works as it does currently, returning the full hierarchy.
- Only `administrativeLevelMin` Defined: The response will include all locations from the specified minimum administrative level up to the root.
- Only `administrativeLevelMax` Defined: The response will include all locations from the root down to the specified maximum administrative level.
- Both Parameters Defined: The response will include locations only within the specified range of administrative levels.

Example: 
```[GET] /LocationHierarchy?_id=<some-location-id>&administrativeLevelMin=2&administrativeLevelMax=4&_count=<page-size>&_page=<page-number>&_sort=<some-sort>```


#### Inventory Filters

The `LocationHierarchy` endpoint supports filtering by inventory availability, allowing users to specify whether they want to retrieve only locations that have associated inventories. This filter can be particularly useful for narrowing down the results to locations that are actively involved in inventory management. The following search parameter is available:
- `filterInventory`: A boolean parameter that specifies whether the response should be filtered by locations with inventories.
  - `filterInventory=true`: Only locations with inventories will be included in the response.
  - `filterInventory=false` (or not set): Locations with or without inventories will be returned. This effectively disables inventory-based filtering. The response will include all locations, regardless of their inventory status. Both locations with and without inventories will be returned.

Example: 
```[GET] /LocationHierarchy?_id=<some-location-id>&filterInventory=true&_count=<page-size>&_page=<page-number>&_sort=<some-sort>```


#### LastUpdated Filters

The `LocationHierarchy` endpoint supports filtering by the last updated timestamp of locations. This filter allows users to retrieve locations based on the last modification date, making it useful for tracking recent updates or syncing data changes over time. The behavior based on the `_lastUpdated` parameter is as follows:
- `_lastUpdated` Not Defined: The endpoint will include all locations in the response, regardless of when they were last modified.
- `_lastUpdated` Defined: The response will include only those locations that were updated on or after the specified timestamp.

Note: This filter only works when in list mode, i.e., `mode=list` is set as one of the parameters.

Example: 
```[GET] /LocationHierarchy?_id=<some-location-id>&mode=list&_lastUpdated=2024-09-22T15%3A13%3A53.014%2B00%3A00&_count=<page-size>&_page=<page-number>&_sort=<some-sort>```


#### LocationHierarchy Summary Count

The LocationHierarchy endpoint supports the `_summary=count` parameter. This allows users to retrieve the total number of matching resources without returning the resource data. This filter only works when in list mode, i.e., `mode=list` is set as one of the parameters.

Example: 
```[GET] /LocationHierarchy?_id=<some-location-id>&mode=list&_summary=count```

## 2. Location Hierarchy Creation