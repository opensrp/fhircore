/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartregister.fhircore.engine.data.remote.model.response;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.*;

@ResourceDef(name = "LocationHierarchy", profile = "http://hl7.org/fhir/profiles/custom-resource")
public class LocationHierarchy extends Location {

    @Child(
            name = "locationId",
            type = {StringType.class},
            order = 5,
            min = 0,
            max = 1,
            modifier = false,
            summary = true)
    @Description(
            shortDefinition = "Unique id to the location",
            formalDefinition = "Id of the location whose location hierarchy will be displayed.")
    protected StringType locationId;

    @Child(
            name = "LocationHierarchyTree",
            type = {LocationHierarchyTree.class})
    @Description(
            shortDefinition = "Complete Location Hierarchy Tree",
            formalDefinition =
                    "Consists of Location Hierarchy Tree and Parent Child Identifiers List")
    private LocationHierarchyTree locationHierarchyTree;

    @Override
    public Location copy() {
        Location location = new Location();
        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> theEntry = new ArrayList<>();
        Bundle.BundleEntryComponent entryComponent = new Bundle.BundleEntryComponent();
        entryComponent.setResource(new Bundle());
        theEntry.add(entryComponent);
        bundle.setEntry(theEntry);
        this.copyValues(location);
        return location;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Bundle;
    }

    public StringType getLocationId() {
        return locationId;
    }

    public void setLocationId(StringType locationId) {
        this.locationId = locationId;
    }

    public LocationHierarchyTree getLocationHierarchyTree() {
        return locationHierarchyTree;
    }

    public void setLocationHierarchyTree(LocationHierarchyTree locationHierarchyTree) {
        this.locationHierarchyTree = locationHierarchyTree;
    }
}
