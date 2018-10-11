package org.n52.wps.python.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.bbox.BoundingBoxData;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.python.data.quakeml.QuakeMLDataBinding;
import org.n52.wps.python.repository.PythonAlgorithmRepository;
import org.n52.wps.python.repository.modules.PythonAlgorithmRepositoryCM;
import org.n52.wps.python.util.JavaProcessStreamReader;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Algorithm(
//        version = "1.0.0")
public class QuakeMLProcessBBox extends AbstractAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(QuakeMLProcessBBox.class);

    private List<String> errors = new ArrayList<>();

    private final String lineSeparator = System.getProperty("line.separator");

    @LiteralDataInput(
            identifier = "lonmin", defaultValue = "288")
    public double lonmin;

    @LiteralDataInput(
            identifier = "lonmax", defaultValue = "292")
    public double lonmax;

    @LiteralDataInput(
            identifier = "latmin", defaultValue = "-70")
    public double latmin;

    @LiteralDataInput(
            identifier = "latmax", defaultValue = "-10")
    public double latmax;

    @LiteralDataInput(
            identifier = "mmin", defaultValue = "6.6")
    public double mmin;

    @LiteralDataInput(
            identifier = "mmax", defaultValue = "8.5")
    public double mmax;

    @LiteralDataInput(
            identifier = "zmin", defaultValue = "5")
    public double zmin;

    @LiteralDataInput(
            identifier = "zmax", defaultValue = "140")
    public double zmax;

    @LiteralDataInput(
            identifier = "p", defaultValue = "0.1")
    public double p;

    @LiteralDataInput(
            identifier = "etype", allowedValues = { "observed", "deaggregation", "stochastic", "expert" }, defaultValue="deaggregation")
    public String etype;

    @LiteralDataInput(
            identifier = "tlon", defaultValue = "-71.5730623712764")
    public double tlon;

    @LiteralDataInput(
            identifier = "tlat", defaultValue = "-33.1299174879672")
    public double tlat;

    private GenericFileData selectedRows;

    private String outputFileName;

    private String workspacePath;

    public QuakeMLProcessBBox() {
        // TODO Get from script
        outputFileName = "test.xml";

        PythonAlgorithmRepositoryCM repositoryCM = (PythonAlgorithmRepositoryCM) WPSConfig.getInstance().getConfigurationModuleForClass(PythonAlgorithmRepository.class.getName(), ConfigurationCategory.REPOSITORY);

        workspacePath = repositoryCM.getWorkspacePath() + "quakeledger/";
    }

    @ComplexDataOutput(identifier = "selected-rows", binding = QuakeMLDataBinding.class)
    public GenericFileData getResult() {
        return selectedRows;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Execute
    public void runScript() throws ExceptionReport {
        LOGGER.info("Executing python script.");

        try {

            Runtime rt = Runtime.getRuntime();

            String command = getCommand();

            Process proc = rt.exec(command, new String[]{}, new File(workspacePath));

            PipedOutputStream pipedOut = new PipedOutputStream();

            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            // attach error stream reader
            JavaProcessStreamReader errorStreamReader =
                    new JavaProcessStreamReader(proc.getErrorStream(), "ERROR", pipedOut);

            // attach output stream reader
            JavaProcessStreamReader outputStreamReader = new JavaProcessStreamReader(proc.getInputStream(), "OUTPUT");

            // start them
            errorStreamReader.start();
            outputStreamReader.start();

            // fetch errors if there are any
            String errors = "";
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line = errorReader.readLine();

                while (line != null) {
                    errors = errors.concat(line + lineSeparator);
                    line = errorReader.readLine();
                }
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java process was interrupted.", e1);
            } finally {
                proc.destroy();
            }

            LOGGER.info(errors);

        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to execute python script.", e);
        }

        File selectedRowCVSFile = new File(workspacePath + "/" + outputFileName);

        try {
            selectedRows = new GenericFileData(selectedRowCVSFile, "text/xml");
        } catch (IOException e) {
            LOGGER.error("Could not create GenericFileData.", e);
        }
    }

    private String getCommand() {

        String pythonScriptName = "eventquery.py";

        return "python3 " + workspacePath + File.separatorChar + pythonScriptName + " " + lonmin + " " + lonmax + " " + latmin + " " + latmax + " " + mmin + " " + mmax
                + " " + zmin + " " + zmax + " " + p + " " + etype + " " + tlon + " " + tlat;
    }

    private void setInputs(Map<String, List<IData>> inputData) {
        getBoundingBox(inputData);
        mmin = getInput(inputData, "mmin");
        mmax = getInput(inputData, "mmax");
        zmin = getInput(inputData, "zmin");
        zmax = getInput(inputData, "zmax");
        p = getInput(inputData, "p");
        etype = getInputString(inputData, "etype");
        tlon = getInput(inputData, "tlon");
        tlat = getInput(inputData, "tlat");
    }

    private void getBoundingBox(Map<String, List<IData>> inputData) {
        IData data = inputData.get("input-boundingbox").get(0);

        if(data instanceof BoundingBoxData){
            BoundingBoxData boundingBox = (BoundingBoxData)data;
            double[] lowerCorner = boundingBox.getLowerCorner();
            double[] upperCorner = boundingBox.getUpperCorner();
            latmin = lowerCorner[0];
            lonmin = lowerCorner[1];
            latmax = upperCorner[0];
            lonmax = upperCorner[1];
        }

    }

    private String getInputString(Map<String, List<IData>> inputData,
            String inputID) {
        List<IData> dataList = inputData.get(inputID);

        IData data = dataList.get(0);

        if(data instanceof LiteralStringBinding){
            return ((LiteralStringBinding)data).getPayload();
        }

        throw new IllegalArgumentException("Could not get input: " + inputID);
    }

    private Double getInput(Map<String, List<IData>> inputData, String inputID){

        List<IData> dataList = inputData.get(inputID);

        IData data = dataList.get(0);

        if(data instanceof LiteralDoubleBinding){
            return ((LiteralDoubleBinding)data).getPayload();
        }

        throw new IllegalArgumentException("Could not get input: " + inputID);
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {

        setInputs(inputData);

        runScript();

        Map<String, IData> result = new HashMap<>();

        result.put("selected-rows", new QuakeMLDataBinding(selectedRows));

        return result;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        return LiteralDoubleBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return QuakeMLDataBinding.class;
    }

}
