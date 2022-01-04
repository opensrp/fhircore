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
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

@DatatypeDef(name = "ChildTreeNode")
public class ChildTreeNode extends Type implements ICompositeType {

    @Child(
            name = "childId",
            type = {StringType.class},
            order = 0,
            min = 1,
            max = 1,
            modifier = false,
            summary = false)
    private StringType childId;

    @Child(
            name = "treeNode",
            type = {TreeNode.class})
    private TreeNode treeNode;

    public ChildTreeNode() {
        treeNode = new TreeNode();
    }

    public StringType getChildId() {
        return childId;
    }

    public ChildTreeNode setChildId(StringType childId) {
        this.childId = childId;
        return this;
    }

    public TreeNode getChildren() {
        if (treeNode == null) {
            treeNode = new TreeNode();
        }
        return treeNode;
    }

    public ChildTreeNode setChildren(TreeNode children) {
        this.treeNode = children;
        return this;
    }

    @Override
    public Type copy() {
        ChildTreeNode childTreeNode = new ChildTreeNode();
        copyValues(childTreeNode);
        return childTreeNode;
    }

    @Override
    public boolean isEmpty() {
        return ElementUtil.isEmpty(childId);
    }

    @Override
    protected Type typedCopy() {
        return copy();
    }
}
