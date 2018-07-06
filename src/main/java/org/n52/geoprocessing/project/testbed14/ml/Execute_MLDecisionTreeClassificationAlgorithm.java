package org.n52.geoprocessing.project.testbed14.ml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.n52.geoprocessing.project.testbed14.ml.util.JavaProcessStreamReader;
import org.n52.project.testbed14.ml.repository.MLAlgorithmRepository;
import org.n52.project.testbed14.ml.repository.modules.MLAlgorithmRepositoryCM;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.IOUtils;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.datahandler.parser.GeotiffParser;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x100.ProcessDescriptionsDocument;

public class Execute_MLDecisionTreeClassificationAlgorithm extends AbstractObservableAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(Execute_MLDecisionTreeClassificationAlgorithm.class);

    private final String fileSeparator = System.getProperty("file.separator");

    private final String lineSeparator = System.getProperty("line.separator");

    private List<String> errors = new ArrayList<>();

    private String processID;

    private final String inputIDSourceData = "source-data";

    private final String inputIDModelParameters = "model";

    private String outputIDClassifiedImage = "classified-image";

    private String outputIDModelQuality = "model-quality";

    private String jarPath;

    public Execute_MLDecisionTreeClassificationAlgorithm() {
    }

    public Execute_MLDecisionTreeClassificationAlgorithm(String processID) {
        super();
        this.processID = processID;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        if (id.equals(inputIDSourceData)) {
            return GenericFileDataBinding.class;
        }
        if (id.equals(inputIDModelParameters)) {
            return GenericFileDataBinding.class;
        }
        return GenericFileDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        if (id.equals(outputIDModelQuality)) {
            return GenericFileDataBinding.class;
        }
        if (id.equals(outputIDClassifiedImage)) {
            return GTRasterDataBinding.class;
        }
        return GenericFileDataBinding.class;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputs) throws ExceptionReport {

        MLAlgorithmRepositoryCM algorithmCM =
                (MLAlgorithmRepositoryCM) WPSConfig.getInstance().getConfigurationModuleForClass(
                        MLAlgorithmRepository.class.getName(), ConfigurationCategory.REPOSITORY);

        // outputDir = algorithmCM.getOutputDir();
        jarPath = algorithmCM.getExecutionJarPath();

        this.update("Starting process with id: " + processID);

        Map<String, IData> outputMap = new HashMap<String, IData>(1);

        File sourceDataFile = null;

        File modelParametersFile = null;

        try {
            IData sourceDataData = getSingleInputData(inputs, inputIDSourceData);

            sourceDataFile = ((GenericFileDataBinding) sourceDataData).getPayload().getBaseFile(false);
        } catch (Exception e1) {
            LOGGER.error("Could not get mandatory input: " + inputIDSourceData);
            throw new ExceptionReport("Could not get mandatory input.", ExceptionReport.INVALID_PARAMETER_VALUE,
                    inputIDSourceData);
        }

        try {
            IData modelParametersData = getSingleInputData(inputs, inputIDModelParameters);

            modelParametersFile = ((GenericFileDataBinding) modelParametersData).getPayload().getBaseFile(false);
        } catch (Exception e1) {
            LOGGER.info("Did not get optional input: " + inputIDModelParameters);
        }

        try {
            IOUtils.unzipAll(modelParametersFile);
        } catch (Exception e) {
            // TODO: handle exception
        }

        // get parent folder of input files
        String parentFolderPath = sourceDataFile.getParent() + fileSeparator;

        LOGGER.debug("Folder of input files: " + parentFolderPath);

        String sourceDataFileName = sourceDataFile.getName();

        String unzippedModelPath = System.getProperty("java.io.tmpdir") + fileSeparator + UUID.randomUUID().toString().substring(0, 5) + fileSeparator;

        unzipFolder(unzippedModelPath, modelParametersFile.getAbsolutePath());

        File outputFolder = new File(System.getProperty("java.io.tmpdir"));

        String outputFolderPath = outputFolder.getAbsolutePath() + fileSeparator;

        File metricsOutputFolder = new File(outputFolderPath + fileSeparator + "metrics");

        if (metricsOutputFolder.exists()) {
            try {
                FileUtils.deleteDirectory(metricsOutputFolder);
                LOGGER.info("Deleted metrics output folder.");
            } catch (Exception e) {
                LOGGER.error("Could not delete metrics output folder.");
            }
        }

        this.update("Starting classification.");

        runModel(sourceDataFileName, parentFolderPath, outputFolderPath, unzippedModelPath);

        this.update("Finished classification.");

        // zip model and metrics folder

        File zippedMetricsOutputFolder = null;

        if (metricsOutputFolder.exists()) {

            try {
                zippedMetricsOutputFolder = IOUtils.zipDirectory(metricsOutputFolder);
                LOGGER.info("Zipped metrics folder path: " + zippedMetricsOutputFolder.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Could not zip metrics output folder.", e);
            }

        } else {
            LOGGER.error("Could not zip metrics output folder.");
        }

        File classifiedImage = new File(outputFolderPath + "classification.tiff");

        if (!classifiedImage.exists()) {
            LOGGER.error("Classified image doesn't exist.");
        }

        try {
            outputMap.put(outputIDModelQuality,
                    new GenericFileDataBinding(new GenericFileData(zippedMetricsOutputFolder, "application/zip")));

            outputMap.put(outputIDClassifiedImage,
                    new GeotiffParser().parse(new FileInputStream(classifiedImage), "application/x-geotiff", null));

        } catch (IOException e) {
            LOGGER.error("Could not create process outputs", e);
        }

        this.update("Finished process with id: " + processID);

        return outputMap;
    }

    private void runModel(String sourceDataFileName,
            String parentFolderPath,
            String outputFolderPath,
            String modelParametersFileName) {

        try {

            LOGGER.info("Executing model.");

            Runtime rt = Runtime.getRuntime();

            String command = "java -jar " + jarPath + " " + sourceDataFileName + " " + parentFolderPath + " "
                    + outputFolderPath + " " + modelParametersFileName;

            LOGGER.info(command);

            Process proc = rt.exec(command);

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
            String errorString = "";
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line = errorReader.readLine();

                while (line != null) {
                    errorString = errorString.concat(line + lineSeparator);
                    line = errorReader.readLine();
                }
            }

            LOGGER.info(errorString);

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java proces was interrupted.", e1);
            } finally {
                proc.destroy();
            }

        } catch (IOException e) {
            LOGGER.error("An error occured while executing the model.", e);
            throw new RuntimeException(e);
        }

    }

    private IData getSingleInputData(Map<String, List<IData>> inputs,
            String id) throws Exception {

        List<IData> inputList = inputs.get(id);

        return inputList.get(0);
    }

    private void unzipFolder(String outputFolderPath,
            String fileName) {

        try (ZipFile file = new ZipFile(fileName)) {
            FileSystem fileSystem = FileSystems.getDefault();

            Enumeration<? extends ZipEntry> entries = file.entries();

            String uncompressedDirectory = outputFolderPath;
            Files.createDirectory(fileSystem.getPath(uncompressedDirectory));

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    LOGGER.trace("Creating Directory:" + uncompressedDirectory + entry.getName());
                    Files.createDirectories(fileSystem.getPath(uncompressedDirectory + entry.getName()));
                } else {
                    InputStream is = file.getInputStream(entry);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    String uncompressedFileName = uncompressedDirectory + entry.getName();
                    Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
                    Files.createFile(uncompressedFilePath);
                    FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
                    while (bis.available() > 0) {
                        fileOutput.write(bis.read());
                    }
                    fileOutput.close();
                    LOGGER.trace("Written :" + entry.getName());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not unzip folder.", e);
        }
    }

    @Override
    public ProcessDescription getDescription() {

        try {
            InputStream in = getClass().getResourceAsStream("Execute_MLDecisionTreeClassificationAlgorithm.xml");

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
