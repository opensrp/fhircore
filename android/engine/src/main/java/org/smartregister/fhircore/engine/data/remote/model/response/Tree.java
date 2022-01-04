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

import static org.smartregister.fhircore.engine.util.Constants.SLASH_UNDERSCORE;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.util.ElementUtil;
import timber.log.Timber;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

@DatatypeDef(name = "Tree")
public class Tree extends Type implements ICompositeType {

    @Child(
            name = "listOfNodes",
            type = {SingleTreeNode.class})
    private SingleTreeNode listOfNodes;

    @Child(
            name = "parentChildren",
            type = {ParentChildrenMap.class},
            order = 1,
            min = 0,
            max = -1,
            modifier = false,
            summary = false)
    private List<ParentChildrenMap> parentChildren;


    public SingleTreeNode getTree() {
        return listOfNodes;
    }

    public Tree() {
        listOfNodes = new SingleTreeNode();
        parentChildren = new ArrayList<>();
    }

    private void addToParentChildRelation(String parent, String id) {
        if (parentChildren == null) {
            parentChildren = new ArrayList<>();
        }
        List<StringType> kids = null;
        if (parentChildren != null) {
            for (int i = 0; i < parentChildren.size(); i++) {
                kids =
                        parentChildren.get(i) != null
                                        && parentChildren.get(i).getIdentifier() != null
                                        && StringUtils.isNotBlank(
                                                parentChildren.get(i).getIdentifier().getValue())
                                        && parentChildren
                                                .get(i)
                                                .getIdentifier()
                                                .getValue()
                                                .equals(parent)
                                ? parentChildren.get(i).getChildIdentifiers()
                                : null;
                Timber.i("Kids are : " + kids);
                if (kids != null) {
                    break;
                }
            }
        }

        if (kids == null) {
            kids = new ArrayList<>();
        }
        StringType idStringType = new StringType();
        String idString = id;
        if (idString.contains(SLASH_UNDERSCORE)) {
            idString = idString.substring(0, idString.indexOf(SLASH_UNDERSCORE));
        }
        idStringType.setValue(idString);

        StringType parentStringType = new StringType();
        parentStringType.setValue(parent);
        kids.add(idStringType);
        Boolean setParentChildMap = false;
        for (int i = 0; i < parentChildren.size(); i++) {
            if (parentChildren.get(i) != null
                    && parentChildren.get(i).getIdentifier() != null
                    && StringUtils.isNotBlank(parentChildren.get(i).getIdentifier().getValue())
                    && parentChildren.get(i).getIdentifier().getValue().equals(parent)) {
                parentChildren.get(i).setChildIdentifiers(kids);
                setParentChildMap = true;
            }
        }

        if (!setParentChildMap) {
            ParentChildrenMap parentChildrenMap = new ParentChildrenMap();
            parentChildrenMap.setIdentifier(parentStringType);
            parentChildrenMap.setChildIdentifiers(kids);
            parentChildren.add(parentChildrenMap);
        }
    }

    public void addNode(String id, String label, Location node, String parentId) {
        if (listOfNodes == null) {
            listOfNodes = new SingleTreeNode();
        }

        // if node exists we should break since user should write optimized code and also tree can
        // not have duplicates
        if (hasNode(id)) {
            throw new IllegalArgumentException("Node with ID " + id + " already exists in tree");
        }

        TreeNode treeNode = makeNode(id, label, node, parentId);

        if (parentId != null) {
            addToParentChildRelation(parentId, id);

            TreeNode parentNode = getNode(parentId);

            // if parent exists add to it otherwise add as root for now
            if (parentNode != null) {
                parentNode.addChild(treeNode);
            } else {
                // if no parent exists add it as root node
                String idString = (String) id;
                if (idString.contains(SLASH_UNDERSCORE)) {
                    idString = idString.substring(0, idString.indexOf(SLASH_UNDERSCORE));
                }
                SingleTreeNode singleTreeNode = new SingleTreeNode();
                StringType treeNodeId = new StringType();
                treeNodeId.setValue(idString);
                singleTreeNode.setTreeNodeId(treeNodeId);
                singleTreeNode.setTreeNode(treeNode);
                listOfNodes = singleTreeNode;
            }
        } else {
            // if no parent add it as root node
            String idString = id;
            if (idString.contains(SLASH_UNDERSCORE)) {
                idString = idString.substring(0, idString.indexOf(SLASH_UNDERSCORE));
            }

            SingleTreeNode singleTreeNode = new SingleTreeNode();
            StringType treeNodeId = new StringType();
            treeNodeId.setValue(idString);
            singleTreeNode.setTreeNodeId(treeNodeId);
            singleTreeNode.setTreeNode(treeNode);
            listOfNodes = singleTreeNode;
        }
    }

    private TreeNode makeNode(String id, String label, Location node, String parentId) {
        TreeNode treenode = getNode(id);
        if (treenode == null) {
            treenode = new TreeNode();
            StringType nodeId = new StringType();
            String idString = (String) id;
            if (idString.contains(SLASH_UNDERSCORE)) {
                idString = idString.substring(0, idString.indexOf(SLASH_UNDERSCORE));
            }
            nodeId.setValue((String) idString);
            treenode.setNodeId(nodeId);
            StringType labelString = new StringType();
            labelString.setValue(label);
            treenode.setLabel(labelString);
            treenode.setNode(node);
            StringType parentIdString = new StringType();
            String parentIdStringVar = parentId;

            if (parentIdStringVar != null && parentIdStringVar.contains(SLASH_UNDERSCORE)) {
                parentIdStringVar =
                        parentIdStringVar.substring(0, parentIdStringVar.indexOf(SLASH_UNDERSCORE));
            }
            parentIdString.setValue(parentIdStringVar);
            treenode.setParent(parentIdString);
        }
        return treenode;
    }

    public TreeNode getNode(String id) {
        // Check if id is any root node
        String idString = id;
        if (idString != null && idString.contains(SLASH_UNDERSCORE)) {
            idString = idString.substring(0, idString.indexOf(SLASH_UNDERSCORE));
        }

        if (listOfNodes.getTreeNodeId() != null
                && StringUtils.isNotBlank(listOfNodes.getTreeNodeId().getValue())
                && listOfNodes.getTreeNodeId().getValue().equals(idString)) {
            return listOfNodes.getTreeNode();

        } else {
            if (listOfNodes != null && listOfNodes.getTreeNode() != null) {
                return listOfNodes.getTreeNode().findChild(idString);
            }
        }
        return null;
    }

    public boolean hasNode(String id) {
        return getNode(id) != null;
    }

    public SingleTreeNode getListOfNodes() {
        return listOfNodes;
    }

    public void setListOfNodes(SingleTreeNode listOfNodes) {
        this.listOfNodes = listOfNodes;
    }

    public List<ParentChildrenMap> getParentChildren() {
        return parentChildren;
    }

    public void setParentChildren(List<ParentChildrenMap> parentChildren) {
        this.parentChildren = parentChildren;
    }

    @Override
    public Type copy() {
        Tree tree = new Tree();
        copyValues(tree);
        return tree;
    }

    @Override
    public boolean isEmpty() {
        return ElementUtil.isEmpty(listOfNodes);
    }

    @Override
    protected Type typedCopy() {
        return copy();
    }
}
