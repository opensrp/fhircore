package org.smartregister.fhircore.engine.data.remote.fhir.resource.parser;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.Iterator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParserErrorHandler;
import ca.uhn.fhir.parser.json.BaseJsonLikeArray;
import ca.uhn.fhir.parser.json.BaseJsonLikeObject;
import ca.uhn.fhir.parser.json.BaseJsonLikeValue;
import ca.uhn.fhir.parser.json.JsonLikeStructure;

public class CustomJsonParser extends ca.uhn.fhir.parser.JsonParser {
    /**
     * Do not use this constructor, the recommended way to obtain a new instance of the JSON parser is to invoke
     * {@link FhirContext#newJsonParser()}.
     *
     * @param theContext
     * @param theParserErrorHandler
     */
    public CustomJsonParser(FhirContext theContext, IParserErrorHandler theParserErrorHandler) {
        super(theContext, theParserErrorHandler);
    }

    public <T extends IBaseResource> T doParseResource(Class<T> theResourceType, JsonLikeStructure theJsonStructure) {
        BaseJsonLikeObject object = theJsonStructure.getRootObject();

        BaseJsonLikeValue resourceTypeObj = object.get("resourceType");
        if (resourceTypeObj == null || !resourceTypeObj.isString() || isBlank(resourceTypeObj.getAsString())) {
            throw new DataFormatException(
                    Msg.code(1838) + "Invalid JSON content detected, missing required element: 'resourceType'");
        }

        String resourceType = resourceTypeObj.getAsString();

        CustomParserState<? extends IBaseResource> state =
                CustomParserState.getPreResourceInstance(this, theResourceType, getContext(), true, getErrorHandler());
        state.enteringNewElement(null, resourceType);

        parseChildren(object, state);

        state.endingElement();
        state.endingElement();

        @SuppressWarnings("unchecked")
        T retVal = (T) state.getObject();

        return retVal;
    }

    private void parseChildren(BaseJsonLikeObject theObject, CustomParserState<?> theState) {
        int allUnderscoreNames = 0;
        int handledUnderscoreNames = 0;

        for (Iterator<String> keyIter = theObject.keyIterator(); keyIter.hasNext(); ) {
            String nextName = keyIter.next();
            if ("resourceType".equals(nextName)) {
                if (theState.isToplevelResourceElement()) {
                    continue;
                }
            } else if ("extension".equals(nextName)) {
                BaseJsonLikeArray array = grabJsonArray(theObject, nextName, "extension");
                parseExtension(theState, array, false);
                continue;
            } else if ("modifierExtension".equals(nextName)) {
                BaseJsonLikeArray array = grabJsonArray(theObject, nextName, "modifierExtension");
                parseExtension(theState, array, true);
                continue;
            } else if (nextName.equals("fhir_comments")) {
                //parseFhirComments(theObject.get(nextName), theState);
                continue;
            } else if (nextName.charAt(0) == '_') {
                allUnderscoreNames++;
                continue;
            }

            BaseJsonLikeValue nextVal = theObject.get(nextName);
            String alternateName = '_' + nextName;
            BaseJsonLikeValue alternateVal = theObject.get(alternateName);
            if (alternateVal != null) {
                handledUnderscoreNames++;
            }

            parseChildren(theState, nextName, nextVal, alternateVal, alternateName, false);
        }

        // if (elementId != null) {
        // IBase object = (IBase) theState.getObject();
        // if (object instanceof IIdentifiableElement) {
        // ((IIdentifiableElement) object).setElementSpecificId(elementId);
        // } else if (object instanceof IBaseResource) {
        // ((IBaseResource) object).getIdElement().setValue(elementId);
        // }
        // }

        /*
         * This happens if an element has an extension but no actual value. I.e.
         * if a resource has a "_status" element but no corresponding "status"
         * element. This could be used to handle a null value with an extension
         * for example.
         */
        if (allUnderscoreNames > handledUnderscoreNames) {
            for (Iterator<String> keyIter = theObject.keyIterator(); keyIter.hasNext(); ) {
                String alternateName = keyIter.next();
                if (alternateName.startsWith("_") && alternateName.length() > 1) {
                    BaseJsonLikeValue nextValue = theObject.get(alternateName);
                    if (nextValue != null) {
                        if (nextValue.isObject()) {
                            String nextName = alternateName.substring(1);
                            if (theObject.get(nextName) == null) {
                                theState.enteringNewElement(null, nextName);
                                parseAlternates(nextValue, theState, alternateName, alternateName);
                                theState.endingElement();
                            }
                        } else {
                            getErrorHandler()
                                    .incorrectJsonType(
                                            null, alternateName, BaseJsonLikeValue.ValueType.OBJECT, null, nextValue.getJsonType(), null);
                        }
                    }
                }
            }
        }
    }

    private void parseChildren(
            CustomParserState<?> theState,
            String theName,
            BaseJsonLikeValue theJsonVal,
            BaseJsonLikeValue theAlternateVal,
            String theAlternateName,
            boolean theInArray) {
        if (theName.equals("id")) {
            if (!theJsonVal.isString()) {
                getErrorHandler()
                        .incorrectJsonType(
                                null,
                                "id",
                                BaseJsonLikeValue.ValueType.SCALAR,
                                BaseJsonLikeValue.ScalarType.STRING,
                                theJsonVal.getJsonType(),
                                theJsonVal.getDataType());
            }
        }

        if (theJsonVal.isArray()) {
            BaseJsonLikeArray nextArray = theJsonVal.getAsArray();

            BaseJsonLikeValue alternateVal = theAlternateVal;
            if (alternateVal != null && alternateVal.isArray() == false) {
                getErrorHandler()
                        .incorrectJsonType(
                                null, theAlternateName, BaseJsonLikeValue.ValueType.ARRAY, null, alternateVal.getJsonType(), null);
                alternateVal = null;
            }

            BaseJsonLikeArray nextAlternateArray = BaseJsonLikeValue.asArray(alternateVal); // could be null
            for (int i = 0; i < nextArray.size(); i++) {
                BaseJsonLikeValue nextObject = nextArray.get(i);
                BaseJsonLikeValue nextAlternate = null;
                if (nextAlternateArray != null && nextAlternateArray.size() >= (i + 1)) {
                    nextAlternate = nextAlternateArray.get(i);
                }
                parseChildren(theState, theName, nextObject, nextAlternate, theAlternateName, true);
            }
        } else if (theJsonVal.isObject()) {
            if (!theInArray && theState.elementIsRepeating(theName)) {
                getErrorHandler().incorrectJsonType(null, theName, BaseJsonLikeValue.ValueType.ARRAY, null, BaseJsonLikeValue.ValueType.OBJECT, null);
            }

            theState.enteringNewElement(null, theName);
            parseAlternates(theAlternateVal, theState, theAlternateName, theAlternateName);
            BaseJsonLikeObject nextObject = theJsonVal.getAsObject();
            boolean preResource = false;
            if (theState.isPreResource()) {
                BaseJsonLikeValue resType = nextObject.get("resourceType");
                if (resType == null || !resType.isString()) {
                    throw new DataFormatException(Msg.code(1843)
                            + "Missing required element 'resourceType' from JSON resource object, unable to parse");
                }
                theState.enteringNewElement(null, resType.getAsString());
                preResource = true;
            }
            parseChildren(nextObject, theState);
            if (preResource) {
                theState.endingElement();
            }
            theState.endingElement();
        } else if (theJsonVal.isNull()) {
            theState.enteringNewElement(null, theName);
            parseAlternates(theAlternateVal, theState, theAlternateName, theAlternateName);
            theState.endingElement();
        } else {
            // must be a SCALAR
            theState.enteringNewElement(null, theName);
            String asString = theJsonVal.getAsString();
            theState.attributeValue("value", asString);
            parseAlternates(theAlternateVal, theState, theAlternateName, theAlternateName);
            theState.endingElement();
        }
    }


    private void parseExtension(CustomParserState<?> theState, BaseJsonLikeArray theValues, boolean theIsModifier) {
        int allUnderscoreNames = 0;
        int handledUnderscoreNames = 0;

        for (int i = 0; i < theValues.size(); i++) {
            BaseJsonLikeObject nextExtObj = BaseJsonLikeValue.asObject(theValues.get(i));
            BaseJsonLikeValue jsonElement = nextExtObj.get("url");
            String url;
            if (null == jsonElement || !(jsonElement.isScalar())) {
                String parentElementName;
                if (theIsModifier) {
                    parentElementName = "modifierExtension";
                } else {
                    parentElementName = "extension";
                }
                getErrorHandler()
                        .missingRequiredElement(new CustomParseLocation().setParentElementName(parentElementName), "url");
                url = null;
            } else {
                url = getExtensionUrl(jsonElement.getAsString());
            }
            theState.enteringNewElementExtension(null, url, theIsModifier, getServerBaseUrl());
            for (Iterator<String> keyIter = nextExtObj.keyIterator(); keyIter.hasNext(); ) {
                String next = keyIter.next();
                if ("url".equals(next)) {
                    continue;
                } else if ("extension".equals(next)) {
                    BaseJsonLikeArray jsonVal = BaseJsonLikeValue.asArray(nextExtObj.get(next));
                    parseExtension(theState, jsonVal, false);
                } else if ("modifierExtension".equals(next)) {
                    BaseJsonLikeArray jsonVal = BaseJsonLikeValue.asArray(nextExtObj.get(next));
                    parseExtension(theState, jsonVal, true);
                } else if (next.charAt(0) == '_') {
                    allUnderscoreNames++;
                    continue;
                } else {
                    BaseJsonLikeValue jsonVal = nextExtObj.get(next);
                    String alternateName = '_' + next;
                    BaseJsonLikeValue alternateVal = nextExtObj.get(alternateName);
                    if (alternateVal != null) {
                        handledUnderscoreNames++;
                    }
                    parseChildren(theState, next, jsonVal, alternateVal, alternateName, false);
                }
            }

            /*
             * This happens if an element has an extension but no actual value. I.e.
             * if a resource has a "_status" element but no corresponding "status"
             * element. This could be used to handle a null value with an extension
             * for example.
             */
            if (allUnderscoreNames > handledUnderscoreNames) {
                for (Iterator<String> keyIter = nextExtObj.keyIterator(); keyIter.hasNext(); ) {
                    String alternateName = keyIter.next();
                    if (alternateName.startsWith("_") && alternateName.length() > 1) {
                        BaseJsonLikeValue nextValue = nextExtObj.get(alternateName);
                        if (nextValue != null) {
                            if (nextValue.isObject()) {
                                String nextName = alternateName.substring(1);
                                if (nextExtObj.get(nextName) == null) {
                                    theState.enteringNewElement(null, nextName);
                                    parseAlternates(nextValue, theState, alternateName, alternateName);
                                    theState.endingElement();
                                }
                            } else {
                                getErrorHandler()
                                        .incorrectJsonType(
                                                null,
                                                alternateName,
                                                BaseJsonLikeValue.ValueType.OBJECT,
                                                null,
                                                nextValue.getJsonType(),
                                                null);
                            }
                        }
                    }
                }
            }
            theState.endingElement();
        }
    }

    private BaseJsonLikeArray grabJsonArray(BaseJsonLikeObject theObject, String nextName, String thePosition) {
        BaseJsonLikeValue object = theObject.get(nextName);
        if (object == null || object.isNull()) {
            return null;
        }
        if (!object.isArray()) {
            throw new DataFormatException(
                    Msg.code(1841) + "Syntax error parsing JSON FHIR structure: Expected ARRAY at element '"
                            + thePosition + "', found '" + object.getJsonType() + "'");
        }
        return object.getAsArray();
    }

    private void parseAlternates(
            BaseJsonLikeValue theAlternateVal,
            CustomParserState<?> theState,
            String theElementName,
            String theAlternateName) {
        if (theAlternateVal == null || theAlternateVal.isNull()) {
            return;
        }

        if (theAlternateVal.isArray()) {
            BaseJsonLikeArray array = theAlternateVal.getAsArray();
            if (array.size() > 1) {
                throw new DataFormatException(Msg.code(1842) + "Unexpected array of length " + array.size()
                        + " (expected 0 or 1) for element: " + theElementName);
            }
            if (array.size() == 0) {
                return;
            }
            parseAlternates(array.get(0), theState, theElementName, theAlternateName);
            return;
        }

        BaseJsonLikeValue alternateVal = theAlternateVal;
        if (alternateVal.isObject() == false) {
            getErrorHandler()
                    .incorrectJsonType(
                            null, theAlternateName, BaseJsonLikeValue.ValueType.OBJECT, null, alternateVal.getJsonType(), null);
            return;
        }

        BaseJsonLikeObject alternate = alternateVal.getAsObject();

        for (Iterator<String> keyIter = alternate.keyIterator(); keyIter.hasNext(); ) {
            String nextKey = keyIter.next();
            BaseJsonLikeValue nextVal = alternate.get(nextKey);
            if ("extension".equals(nextKey)) {
                boolean isModifier = false;
                BaseJsonLikeArray array = nextVal.getAsArray();
                parseExtension(theState, array, isModifier);
            } else if ("modifierExtension".equals(nextKey)) {
                boolean isModifier = true;
                BaseJsonLikeArray array = nextVal.getAsArray();
                parseExtension(theState, array, isModifier);
            } else if ("id".equals(nextKey)) {
                if (nextVal.isString()) {
                    theState.attributeValue("id", nextVal.getAsString());
                } else {
                    getErrorHandler()
                            .incorrectJsonType(
                                    null,
                                    "id",
                                    BaseJsonLikeValue.ValueType.SCALAR,
                                    BaseJsonLikeValue.ScalarType.STRING,
                                    nextVal.getJsonType(),
                                    nextVal.getDataType());
                }
            } else if ("fhir_comments".equals(nextKey)) {
                //parseFhirComments(nextVal, theState);
            }
        }
    }
}
