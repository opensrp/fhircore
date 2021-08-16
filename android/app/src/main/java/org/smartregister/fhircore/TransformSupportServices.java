package org.smartregister.fhircore;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 13-08-2021.
 */

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceFactory;
import org.hl7.fhir.r4.model.RiskAssessment;
import org.hl7.fhir.r4.terminologies.ConceptMapEngine;
import org.hl7.fhir.r4.utils.StructureMapUtilities;

import java.io.PrintWriter;
import java.util.List;

/**
 * Copied from https://github.com/hapifhir/org.hl7.fhir.core/blob/master/org.hl7.fhir.validation/src/main/java/org/hl7/fhir/validation/TransformSupportServices.java
 * and adapted for R4. This class enables us to implement generation of Types and Resources not in
 * the original Hapi Fhir source code here https://github.com/hapifhir/org.hl7.fhir.core/blob/master/org.hl7.fhir.r4/src/main/java/org/hl7/fhir/r4/model/ResourceFactory.java.
 * The missing Types and Resources are internal model types eg RiskAssessment.Prediction, Immunization.Reaction
 */
public class TransformSupportServices implements StructureMapUtilities.ITransformerServices {

    private final PrintWriter mapLog;
    private final SimpleWorkerContext context;
    private List<Base> outputs;

    public TransformSupportServices(List<Base> outputs,
                                    PrintWriter mapLog,
                                    SimpleWorkerContext context) {
        this.outputs = outputs;
        this.mapLog = mapLog;
        this.context = context;
    }

    @Override
    public void log(String message) {
        if (mapLog != null)
            mapLog.println(message);
        System.out.println(message);
    }

    @Override
    public Base createType(Object appInfo, String name) throws FHIRException {
        if (name.equals("RiskAssessment_Prediction")) {
            return new RiskAssessment.RiskAssessmentPredictionComponent();
        }

        return ResourceFactory.createResourceOrType(name);
    }

    @Override
    public Base createResource(Object appInfo, Base res, boolean atRootofTransform) {
        if (atRootofTransform)
            outputs.add(res);
        return res;
    }

    @Override
    public Coding translate(Object appInfo, Coding source, String conceptMapUrl) throws FHIRException {
        ConceptMapEngine cme = new ConceptMapEngine(context);
        return cme.translate(source, conceptMapUrl);
    }

    @Override
    public Base resolveReference(Object appContext, String url) throws FHIRException {
        throw new FHIRException("resolveReference is not supported yet");
    }

    @Override
    public List<Base> performSearch(Object appContext, String url) throws FHIRException {
        throw new FHIRException("performSearch is not supported yet");
    }
}