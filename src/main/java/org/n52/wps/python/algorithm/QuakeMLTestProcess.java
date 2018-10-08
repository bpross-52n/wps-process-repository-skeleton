package org.n52.wps.python.algorithm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.commons.context.ExecutionContextFactory;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.io.datahandler.generator.GeoJSONGenerator;
import org.n52.wps.python.data.quakeml.QuakeMLDataBinding;
import org.n52.wps.python.data.quakeml.QuakeMLParser;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x100.OutputDefinitionType;

public class QuakeMLTestProcess extends AbstractAlgorithm {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuakeMLTestProcess.class);

    private String mimeTypeGeoJSON = "application/vnd.geo+json";

    private String mimeTypeQuakeML = "text/xml";

    private String inputID = "input";
    private String outputID = "output";

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {

        IData input = inputData.get(inputID).get(0);

        Map<String, IData> result = new HashMap<>();

        result.put(outputID, input);

        return result;
    }

    @Override
    public List<String> getErrors() {
        return null;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        return QuakeMLDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return QuakeMLDataBinding.class;
    }

}
