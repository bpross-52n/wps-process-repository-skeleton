package org.n52.wps.project.riesgos.shakemap.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.project.riesgos.shakemap.io.ShakemapDataBinding;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridDocument;
import net.opengis.wps.x100.ProcessDescriptionsDocument;

public class ShakemapAlgorithm extends AbstractObservableAlgorithm {

    private static Logger LOGGER = LoggerFactory
            .getLogger(ShakemapAlgorithm.class);

    private List<String> errors = new ArrayList<>();

    private String processID;
    private String outputID = "shakemap";

    public ShakemapAlgorithm(){

    }

    public ShakemapAlgorithm(String processID) {
        this.processID = processID;
    }

    @Override
    public List<String> getErrors() {
        return errors ;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        return GenericFileDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return ShakemapDataBinding.class;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputs) throws ExceptionReport {
        LOGGER.info("Starting process with id: " + processID);

        InputStream in = getClass().getResourceAsStream("us2000fzwt_us_1531091459400_download_grid.xml");

        ShakemapGridDocument shakemapGridDocument;
        try {
            shakemapGridDocument = ShakemapGridDocument.Factory.parse(in);
        } catch (XmlException | IOException e) {
            LOGGER.error("Could not parse Shakemap.", e);
            throw new ExceptionReport("Could not parse Shakemap.", ExceptionReport.NO_APPLICABLE_CODE, e);
        }

        ShakemapDataBinding shakemapDataBinding = new ShakemapDataBinding(shakemapGridDocument);

        Map<String, IData> outputMap = new HashMap<String, IData>(1);

        outputMap.put(outputID, shakemapDataBinding);

        LOGGER.info("Finished process with id: " + processID);

        return outputMap;
    }

    @Override
    public ProcessDescription getDescription() {

        try {
            InputStream in = getClass().getResourceAsStream("ShakemapAlgorithm.xml");

            ProcessDescriptionsDocument processDescriptionsDocument = ProcessDescriptionsDocument.Factory.parse(in);

            ProcessDescription processDescription = new ProcessDescription();

            processDescription.addProcessDescriptionForVersion(processDescriptionsDocument.getProcessDescriptions().getProcessDescriptionArray(0), "1.0.0");

            return processDescription;

        } catch (Exception e) {
            LOGGER.error("Could not parse ProcessDescription. Returning empty ProcessDescription.");
        }

        return new ProcessDescription();
    }

}
