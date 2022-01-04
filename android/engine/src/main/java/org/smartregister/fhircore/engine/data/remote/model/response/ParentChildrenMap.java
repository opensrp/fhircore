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
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

@DatatypeDef(name = "ParentChildrenMap")
public class ParentChildrenMap extends Type implements ICompositeType {

    @Child(
            name = "identifier",
            type = {StringType.class},
            order = 0,
            min = 1,
            max = 1,
            modifier = false,
            summary = false)
    private StringType identifier;

    @Child(
            name = "childIdentifiers",
            type = {StringType.class},
            order = 1,
            min = 0,
            max = -1,
            modifier = false,
            summary = false)
    private List<StringType> childIdentifiers;

    public StringType getIdentifier() {
        return identifier;
    }

    public ParentChildrenMap setIdentifier(StringType identifier) {
        this.identifier = identifier;
        return this;
    }

    public List<StringType> getChildIdentifiers() {
        return childIdentifiers;
    }

    public ParentChildrenMap setChildIdentifiers(List<StringType> childIdentifiers) {
        this.childIdentifiers = childIdentifiers;
        return this;
    }

    @Override
    public Type copy() {
        ParentChildrenMap parentChildrenMap = new ParentChildrenMap();
        copyValues(parentChildrenMap);
        return parentChildrenMap;
    }

    @Override
    public boolean isEmpty() {
        return ElementUtil.isEmpty(identifier);
    }

    @Override
    protected Type typedCopy() {
        return copy();
    }
}
