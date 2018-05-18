package org.n52.geoprocessing.project.testbed14.gait;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.wps.io.IOUtils;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.server.grass.util.JavaProcessStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x100.ProcessDescriptionsDocument;

public class GenericGaitToolAlgorithm extends AbstractObservableAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(GenericGaitToolAlgorithm.class);

    private List<String> errors = new ArrayList<>();

    private String processID;

    private final String inputID = "input-features";

    private String outputIDAttributionErrors = "attribution-errors";

    private String outputIDConditionReports = "condition-reports";

    private final String lineSeparator = System.getProperty("line.separator");

    private String[] envp;

    private String command;

    public static final String OS_Name = System.getProperty("os.name");

    public GenericGaitToolAlgorithm() {

    }

    public GenericGaitToolAlgorithm(String processID) {
        this.processID = processID;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        // ignore check for right id atm
        // if(id.equals("myID")){
        // return MyDataBinding.class;
        // }
        return GenericFileDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        // ignore check for right id atm
        // if(id.equals("myID")){
        // return MyDataBinding.class;
        // }
        return GenericFileDataBinding.class;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputs) throws ExceptionReport {

        LOGGER.info("Starting process with id: " + processID);
        this.update("Starting process with id: " + processID);

        // now you can do what you want, e.g. start an external program based on
        // the processID
        // for this example, we will just return the input as process output

        List<IData> inputList = inputs.get(inputID);

        // we just take the first input, omitting a check for the list size
        IData inputData = inputList.get(0);

        Runtime rt = Runtime.getRuntime();

        //TODO check if data dir exists

        try {

            Process proc = rt.exec(getCommand(), getEnvp(), new File("C:\\Users\\IEUser\\"));//TODO get from config

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
                LOGGER.error("Java process was interrupted.");
            } finally {
                proc.destroy();
            }

        } catch (IOException e) {

        }

        Map<String, IData> outputMap = new HashMap<String, IData>(2);

        try {
            File attributionErrorsZipfile = IOUtils.zipDirectory(new File("D:/Chaos-Folder/Testbed-14/attribution_errors"));

            outputMap.put(outputIDAttributionErrors, new GenericFileDataBinding(new GenericFileData(attributionErrorsZipfile, "application/zip")));

        } catch (IOException e) {
            LOGGER.error("Could not create zipfile for attribution errors");
        }

        try {
            File conditionReportsZipfile = IOUtils.zipDirectory(new File("D:/Chaos-Folder/Testbed-14/condition_reports"));

            outputMap.put(outputIDConditionReports, new GenericFileDataBinding(new GenericFileData(conditionReportsZipfile, "application/zip")));

        } catch (IOException e) {
            LOGGER.error("Could not create zipfile for attribution errors");
        }

        LOGGER.info("Finished process with id: " + processID);
        this.update("Finished process with id: " + processID);

        return outputMap;
    }

    private String getCommand() {

        if (command == null) {
            // command = "\"C:\\Program Files\\GAIT-WINDOWS-26\\gait26.exe\"
            // -nogui \"sample.txt\" GIFDNUNANPO \"SampleBatchSilent\"
            // USE_DFEGMASTER META_ESRI -silent";
            command = "C:\\Users\\IEUser\\testbatchmode2.bat";
        }

        return command;
    }

    private String[] getEnvp() {

        if (envp == null) {
          //TODO get from config
            if (!OS_Name.startsWith("Windows")) {
                envp = new String[] { "DISPLAY=127.0.0.1:0.0", "GAITHOME=C:\\Program Files\\GAIT-WINDOWS-26",
                        "CYGWIN_ROOT=\\cygwin", "PATH=.;%CYGWIN_ROOT%\\bin;%CYGWIN_ROOT%\\usr\\X11R6\\bin;%PATH%",
                        "PATH=.;\"C:\\Program Files\\GAIT-WINDOWS-26\";%PATH%",
                        "GAIT_PROJECTS=C:\\WPS-support-files\\gait-tool", "GAIT_INCLUDE_WGS84_SHAPE_PROJECTION=ON",
                        "GAIT_EXPORT_CONDITION_SHAPEFILE_NAME=myconditionname" };
            } else {
                envp = new String[] { "DISPLAY=127.0.0.1:0.0", "GAITHOME=C:\\Program Files\\GAIT-WINDOWS-26",
                        "CYGWIN_ROOT=\\cygwin", "PATH=.;%CYGWIN_ROOT%\\bin;%CYGWIN_ROOT%\\usr\\X11R6\\bin;%PATH%",
                        "PATH=.;\"C:\\Program Files\\GAIT-WINDOWS-26\";%PATH%",
                        "GAIT_PROJECTS=C:\\WPS-support-files\\gait-tool", "GAIT_INCLUDE_WGS84_SHAPE_PROJECTION=ON",
                        "GAIT_EXPORT_CONDITION_SHAPEFILE_NAME=myconditionname" };
            }
        }

        return envp;
    }

    @Override
    public ProcessDescription getDescription() {

        try {
            InputStream in = getClass().getResourceAsStream("GenericGaitToolAlgorithm.xml");//TODO get from class

            ProcessDescriptionsDocument processDescriptionsDocument = ProcessDescriptionsDocument.Factory.parse(in);

            ProcessDescription processDescription = new ProcessDescription();

            processDescription.addProcessDescriptionForVersion(
                    processDescriptionsDocument.getProcessDescriptions().getProcessDescriptionArray(0), "1.0.0");

            return processDescription;

        } catch (Exception e) {
            LOGGER.error("Could not parse ProcessDescription. Returning empty ProcessDescription.");
        }

        return new ProcessDescription();
    }

}
