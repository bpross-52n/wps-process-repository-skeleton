package org.n52.wps.python.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.python.repository.PythonAlgorithmRepository;
import org.n52.wps.python.repository.modules.PythonAlgorithmRepositoryCM;
import org.n52.wps.python.util.JavaProcessStreamReader;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Algorithm(
        version = "1.0.0")
public class QuakeMLProcess extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(QuakeMLProcess.class);

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

    public QuakeMLProcess() {
        // TODO Get from script
        outputFileName = "test.xml";

        PythonAlgorithmRepositoryCM repositoryCM = (PythonAlgorithmRepositoryCM) WPSConfig.getInstance().getConfigurationModuleForClass(PythonAlgorithmRepository.class.getName(), ConfigurationCategory.REPOSITORY);

        workspacePath = repositoryCM.getWorkspacePath() + "quakeledger/";
    }

    @ComplexDataOutput(identifier = "selected-rows", binding = GenericFileDataBinding.class)
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

        // just need to execute the task manager
//        Process proc = rt.exec("cmd.exe /c start");
        return "python3 " + workspacePath + File.separatorChar + pythonScriptName + " " + lonmin + " " + lonmax + " " + latmin + " " + latmax + " " + mmin + " " + mmax
                + " " + zmin + " " + zmax + " " + p + " " + etype + " " + tlon + " " + tlat;
    }

}
