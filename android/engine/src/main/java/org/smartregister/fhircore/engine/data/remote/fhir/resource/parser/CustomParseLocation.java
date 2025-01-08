package org.smartregister.fhircore.engine.data.remote.fhir.resource.parser;

import ca.uhn.fhir.parser.IParserErrorHandler.IParseLocation;

import static org.apache.commons.lang3.StringUtils.defaultString;

class CustomParseLocation implements IParseLocation {

    private String myParentElementName;

    /**
     * Constructor
     */
    CustomParseLocation() {
        super();
    }

    /**
     * Constructor
     */
    CustomParseLocation(String theParentElementName) {
        setParentElementName(theParentElementName);
    }

    @Override
    public String getParentElementName() {
        return myParentElementName;
    }

    CustomParseLocation setParentElementName(String theParentElementName) {
        myParentElementName = theParentElementName;
        return this;
    }

    @Override
    public String toString() {
        return "[element=\"" + defaultString(myParentElementName) + "\"]";
    }

    /**
     * Factory method
     */
    static CustomParseLocation fromElementName(String theChildName) {
        return new CustomParseLocation(theChildName);
    }
}
