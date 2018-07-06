package org.n52.geoprocessing.project.testbed14.ml;

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
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.n52.geoprocessing.project.testbed14.ml.util.JavaProcessStreamReader;
import org.n52.project.testbed14.ml.repository.MLAlgorithmRepository;
import org.n52.project.testbed14.ml.repository.modules.MLAlgorithmRepositoryCM;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.IOUtils;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x100.ProcessDescriptionsDocument;

public class Train_MLDecisionTreeClassificationAlgorithm extends AbstractObservableAlgorithm {

    private static Logger LOGGER = LoggerFactory
            .getLogger(Train_MLDecisionTreeClassificationAlgorithm.class);

    private final String fileSeparator = System.getProperty("file.separator");
    private final String lineSeparator = System.getProperty("line.separator");

    private List<String> errors = new ArrayList<>();

    private String processID;
    private final String inputIDSourceData = "source-data";
    private final String inputIDTrainingData = "training-data";
    private final String inputIDInitialModelParameters = "initial-model-parameters";
    private String outputIDModelParameters = "model";
    private String outputIDClassifiedImage = "classified-image";
    private String outputIDModelQuality = "model-quality";
    private String outputDir;
    private String jarPath;

    public Train_MLDecisionTreeClassificationAlgorithm(){
    }

    public Train_MLDecisionTreeClassificationAlgorithm(String processID) {
        super();
        this.processID = processID;
    }

    @Override
    public List<String> getErrors() {
        return errors ;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        if(id.equals(inputIDSourceData)){
            return GenericFileDataBinding.class;
        }
        if(id.equals(inputIDTrainingData)){
            return GenericFileDataBinding.class;
        }
        if(id.equals(inputIDInitialModelParameters)){
            return GenericFileDataBinding.class;
        }
        return GenericFileDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
      if(id.equals(outputIDModelParameters)){
          return GenericFileDataBinding.class;
      }
      //TODO add ids
      return GenericFileDataBinding.class;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputs) throws ExceptionReport {

        MLAlgorithmRepositoryCM algorithmCM = (MLAlgorithmRepositoryCM) WPSConfig.getInstance()
                .getConfigurationModuleForClass(MLAlgorithmRepository.class.getName(),
                        ConfigurationCategory.REPOSITORY);

//        outputDir = algorithmCM.getOutputDir();
        jarPath = algorithmCM.getTrainingJarPath();

        this.update("Starting process with id: " + processID);

        Map<String, IData> outputMap = new HashMap<String, IData>(1);

        File sourceDataFile = null;

        File trainingDataFile = null;

        File initialModelParametersFile = null;

        try {
            IData trainingDataData = getSingleInputData(inputs, inputIDTrainingData);

            trainingDataFile = ((GenericFileDataBinding)trainingDataData).getPayload().getBaseFile(false);
        } catch (Exception e1) {
            LOGGER.error("Could not get mandatory input: " + inputIDTrainingData);
            throw new ExceptionReport("Could not get mandatory input.", ExceptionReport.INVALID_PARAMETER_VALUE, inputIDTrainingData);
        }

        try {
            IData sourceDataData = getSingleInputData(inputs, inputIDSourceData);

            sourceDataFile = ((GenericFileDataBinding) sourceDataData).getPayload().getBaseFile(false);
        } catch (Exception e1) {
            LOGGER.error("Could not get mandatory input: " + inputIDSourceData);
            throw new ExceptionReport("Could not get mandatory input.", ExceptionReport.INVALID_PARAMETER_VALUE,
                    inputIDSourceData);
        }

        try {
            IData initialModelParametersData = getSingleInputData(inputs, inputIDInitialModelParameters);

            initialModelParametersFile = ((GenericFileDataBinding)initialModelParametersData).getPayload().getBaseFile(false);
        } catch (Exception e1) {
            LOGGER.info("Did not get optional input: " + inputIDInitialModelParameters);
        }

        //get parent folder of input files
        String parentFolderPath = sourceDataFile.getParent() + fileSeparator;

        LOGGER.debug("Folder of input files: " + parentFolderPath);

        String sourceDataFileName = sourceDataFile.getName();

        String trainingDataFileName = trainingDataFile.getName();

        File outputFolder = new File(System.getProperty("java.io.tmpdir"));
//        File outputFolder = new File("/tmp/" + fileSeparator + UUID.randomUUID().toString().substring(0, 5));


        String outputFolderPath = outputFolder.getAbsolutePath() + fileSeparator;

        File modelOutputFolder = new File(outputFolderPath + fileSeparator + "model");

        if(modelOutputFolder.exists()){
            try {
                FileUtils.deleteDirectory(modelOutputFolder);
                LOGGER.info("Deleted model output folder.");
            } catch (Exception e) {
                LOGGER.error("Could not delete model output folder.");
            }
        }

        File metricsOutputFolder = new File(outputFolderPath + fileSeparator + "metrics");

        if(metricsOutputFolder.exists()){
            try {
                FileUtils.deleteDirectory(metricsOutputFolder);
                LOGGER.info("Deleted metrics output folder.");
            } catch (Exception e) {
                LOGGER.error("Could not delete metrics output folder.");
            }
        }

        this.update("Starting training run.");

        trainModel(sourceDataFileName, trainingDataFileName, parentFolderPath, outputFolderPath);

        this.update("Finished training run.");

        //zip model and metrics folder

        File zippedModelOutputFolder = null;

        if(modelOutputFolder.exists()){

            try {
                zippedModelOutputFolder = IOUtils.zipDirectory(modelOutputFolder);

                LOGGER.info("Zipped model folder path: " + zippedModelOutputFolder.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Could not zip model output folder.", e);
            }

        }else{
            LOGGER.error("Could not zip model output folder.");
        }

        File zippedMetricsOutputFolder = null;

        if(metricsOutputFolder.exists()){

            try {
                zippedMetricsOutputFolder = IOUtils.zipDirectory(metricsOutputFolder);
                LOGGER.info("Zipped metrics folder path: " + zippedMetricsOutputFolder.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Could not zip metrics output folder.", e);
            }

        }else{
            LOGGER.error("Could not zip metrics output folder.");
        }

        File classifiedImage = new File(outputFolderPath + "classification.png");

        if(!classifiedImage.exists()){
            LOGGER.error("Classified image doesn't exist.");
        }

        if(!zippedMetricsOutputFolder.exists() && !zippedModelOutputFolder.exists() && !classifiedImage.exists()){
            LOGGER.error("Something did go wrong with executing the model.");
            throw new ExceptionReport("Something did go wrong with executing the model.", ExceptionReport.NO_APPLICABLE_CODE);
        }

        try {
            outputMap.put(outputIDModelParameters, new GenericFileDataBinding(new GenericFileData(zippedModelOutputFolder, "application/zip")));
            outputMap.put(outputIDModelQuality, new GenericFileDataBinding(new GenericFileData(zippedMetricsOutputFolder, "application/zip")));
            outputMap.put(outputIDClassifiedImage, new GenericFileDataBinding(new GenericFileData(classifiedImage, "image/png")));
        } catch (IOException e) {
            LOGGER.error("Could not create process outputs", e);
        }

        this.update("Finished process with id: " + processID);

        return outputMap;
    }

    private void trainModel(String sourceDataFileName, String trainingDataFileName, String parentFolderPath, String outputFolderPath){

        try {

            LOGGER.info("Executing model.");

            Runtime rt = Runtime.getRuntime();

            String command = "java -jar " + jarPath + " " + sourceDataFileName +
                    " " + trainingDataFileName + " " + parentFolderPath + " " + outputFolderPath;//TODO jar path from properties

            LOGGER.info(command);

            Process proc = rt.exec(command);

            PipedOutputStream pipedOut = new PipedOutputStream();

            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            // attach error stream reader
            JavaProcessStreamReader errorStreamReader = new JavaProcessStreamReader(proc
                    .getErrorStream(), "ERROR", pipedOut);

            // attach output stream reader
            JavaProcessStreamReader outputStreamReader = new JavaProcessStreamReader(proc
                    .getInputStream(), "OUTPUT");

            // start them
            errorStreamReader.start();
            outputStreamReader.start();

            //fetch errors if there are any
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
            }finally{
                proc.destroy();
            }

        } catch (IOException e) {
            LOGGER.error("An error occured while executing the model.", e);
            throw new RuntimeException(e);
        }

    }

    private IData getSingleInputData(Map<String, List<IData>> inputs, String id) throws Exception{

        List<IData> inputList = inputs.get(id);

        return inputList.get(0);
    }

    @Override
    public ProcessDescription getDescription() {

        try {
            InputStream in = getClass().getResourceAsStream("Train_MLDecisionTreeClassificationAlgorithm.xml");

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
