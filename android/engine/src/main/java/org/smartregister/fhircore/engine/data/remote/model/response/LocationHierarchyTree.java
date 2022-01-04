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
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.util.ElementUtil;
import java.util.List;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

@DatatypeDef(name = "LocationHierarchyTree")
public class LocationHierarchyTree extends Type implements ICompositeType {

    @Child(name = "locationsHierarchy")
    private Tree locationsHierarchy;

    public LocationHierarchyTree() {
        this.locationsHierarchy = new Tree();
    }

    public void addLocation(Location l) {
        StringType idString = new StringType();
        idString.setValue(l.getId());
        if (!locationsHierarchy.hasNode(idString.getValue())) {
            if (l.getPartOf() == null || l.getPartOf().getReference().isEmpty()) {
                locationsHierarchy.addNode(idString.getValue(), l.getName(), l, null);
            } else {
                // get Parent Location
                StringType parentId = new StringType();
                parentId.setValue(l.getPartOf().getReference());
                locationsHierarchy.addNode(
                        idString.getValue(), l.getName(), l, parentId.getValue());
            }
        }
    }

    /**
     * WARNING: Overrides existing locations
     *
     * @param locations
     */
    public void buildTreeFromList(List<Location> locations) {
        for (Location location : locations) {
            addLocation(location);
        }
    }

    public Tree getLocationsHierarchy() {
        return locationsHierarchy;
    }

    public LocationHierarchyTree setLocationsHierarchy(Tree locationsHierarchy) {
        this.locationsHierarchy = locationsHierarchy;
        return this;
    }

    @Override
    public Type copy() {
        LocationHierarchyTree locationHierarchyTree = new LocationHierarchyTree();
        copyValues(locationHierarchyTree);
        return locationHierarchyTree;
    }

    @Override
    public boolean isEmpty() {
        return ElementUtil.isEmpty(locationsHierarchy);
    }

    @Override
    protected Type typedCopy() {
        return copy();
    }
}
