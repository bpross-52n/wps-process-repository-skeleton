package org.n52.geoprocessing.project.testbed14.gait;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.n52.geoprocessing.project.testbed14.gait.configmodules.GaitToolAlgorithmRepositoryCM;
import org.n52.geoprocessing.project.testbed14.gait.repository.GaitToolAlgorithmRepository;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.IOUtils;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.server.grass.util.JavaProcessStreamReader;
import org.n52.wps.webapp.api.ConfigurationCategory;
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

    private File workspaceFolder;

    private String gaitHome;

    public static final String OS_Name = System.getProperty("os.name");

    public GenericGaitToolAlgorithm() {

        GaitToolAlgorithmRepositoryCM gaitToolAlgorithmRepoConfigModule = (GaitToolAlgorithmRepositoryCM) WPSConfig.getInstance()
                .getConfigurationModuleForClass(GaitToolAlgorithmRepository.class.getName(),
                        ConfigurationCategory.REPOSITORY);
        
        gaitHome = gaitToolAlgorithmRepoConfigModule.getGaitToolHome();
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

        this.update("Starting process with id: " + processID);

        // now you can do what you want, e.g. start an external program based on
        // the processID
        // for this example, we will just return the input as process output

        List<IData> inputList = inputs.get(inputID);

        // we just take the first input, omitting a check for the list size
        IData inputData = inputList.get(0);

        File inputZip = ((GenericFileDataBinding)inputData).getPayload().getBaseFile(true).getParentFile();
        
        workspaceFolder = new File(System.getProperty("java.io.tmpdir") + File.separatorChar + UUID.randomUUID().toString().substring(0, 5));
        try {
            workspaceFolder.mkdirs();
        } catch (Exception e) {
            LOGGER.error("Could not create workspace folder.");
        }

        String projectUUID = UUID.randomUUID().toString().substring(0, 5);

        String outputFolderName = "output" + projectUUID;

        // create .bat file
        String gaitToolCommand = "gait26.exe -nogui \"" + projectUUID + ".txt\" GIFDNUNANPO \"" + outputFolderName
                + "\" USE_DFEGMASTER META_ESRI";

        String batFileName = workspaceFolder.getAbsolutePath() + File.separatorChar + projectUUID + ".bat";

        try {
            writeContentToFile(gaitToolCommand, batFileName);
        } catch (IOException e2) {
            LOGGER.error("Could not create .bat file.", e2);
        }

        try {
            writeContentToFile(inputZip.getAbsolutePath(),
                    workspaceFolder.getAbsolutePath() + File.separatorChar + projectUUID + ".txt");
        } catch (IOException e2) {
            LOGGER.error("Could not create .txt file.", e2);
        }

        // copy or create .bat file

        // execute .bat file

        // create sample.txt
        // write output location

        // run GAIT tool
        try {

            Runtime rt = Runtime.getRuntime();

            Process proc = rt.exec(batFileName, getEnvp(), workspaceFolder);

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

            LOGGER.error(errors);

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java process was interrupted.");
            } finally {
                proc.destroy();
            }

        } catch (IOException e) {
            LOGGER.error("Java process was interrupted.");
        }

        Map<String, IData> outputMap = new HashMap<String, IData>(2);

        try {
            File attributionErrorsZipfile = IOUtils.zipDirectory(new File(workspaceFolder.getAbsolutePath() + File.separatorChar + outputFolderName + File.separatorChar + "attribution_errors"));

            outputMap.put(outputIDAttributionErrors,
                    new GenericFileDataBinding(new GenericFileData(attributionErrorsZipfile, "application/zip")));

        } catch (IOException e) {
            LOGGER.error("Could not create zipfile for attribution errors");
        }

        try {
            File conditionReportsZipfile = IOUtils.zipDirectory(new File(workspaceFolder.getAbsolutePath() + File.separatorChar + outputFolderName + File.separatorChar + "condition_reports"));

            outputMap.put(outputIDConditionReports,
                    new GenericFileDataBinding(new GenericFileData(conditionReportsZipfile, "application/zip")));

        } catch (IOException e) {
            LOGGER.error("Could not create zipfile for attribution errors");
        }

        this.update("Finished process with id: " + processID);

        return outputMap;
    }

    private void writeContentToFile(String content,
            String filePath) throws IOException {

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath)));

        bufferedWriter.write(content);

        bufferedWriter.close();

    }

    private String[] getEnvp() {

        String[] env = new String[] { "GAITHOME=" + gaitHome, "DISPLAY=127.0.0.1:0.0", "CYGWIN_ROOT=\\cygwin",
                "PATH=.;%CYGWIN_ROOT%\\bin;%CYGWIN_ROOT%\\usr\\X11R6\\bin;%PATH%", "PATH=.;\"" + gaitHome + "\";%PATH%",
                "GAIT_PROJECTS=" + workspaceFolder.getAbsolutePath() };// TODO
                                                                       // use
                                                                       // different
                                                                       // variables
                                                                       // for
                                                                       // linux

        return env;
    }

    @Override
    public ProcessDescription getDescription() {

        try {
            InputStream in = getClass().getResourceAsStream("GenericGaitToolAlgorithm.xml");// TODO
                                                                                            // get
                                                                                            // from
                                                                                            // class

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
