package org.n52.geoprocessing.project.testbed14.ml;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.n52.geoprocessing.project.testbed14.ml.util.JavaProcessStreamReader;
import org.n52.project.testbed14.ml.repository.MLAlgorithmRepository;
import org.n52.project.testbed14.ml.repository.modules.MLAlgorithmRepositoryCM;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.IOUtils;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralAnyURIBinding;
import org.n52.wps.io.datahandler.generator.GeoServerUploader;
import org.n52.wps.io.datahandler.generator.GeoserverWCSGenerator;
import org.n52.wps.io.datahandler.parser.GenericFileParser;
import org.n52.wps.io.modules.generator.GeoserverWCSGeneratorCM;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.server.database.DatabaseFactory;
import org.n52.wps.server.handler.DataInputInterceptors.DataInputInterceptorImplementations;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x100.ProcessDescriptionsDocument;

@DataInputInterceptorImplementations(value="org.n52.wps.server.handler.PassToAlgorithmInputInterceptors")
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
    private String outputIDModel = "model";
    private String outputIDClassifiedImage = "classified-image";
    private String outputIDModelQuality = "model-quality";
    private String outputDir;
    private String jarPath;

    private String modelPath;

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
      if(id.equals(outputIDModel)){
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

        GeoserverWCSGeneratorCM geoserverWCSGeneratorCM = (GeoserverWCSGeneratorCM) WPSConfig.getInstance().getConfigurationModuleForClass(
                GeoserverWCSGenerator.class.getName(), ConfigurationCategory.GENERATOR);

//        outputDir = algorithmCM.getOutputDir();
        jarPath = algorithmCM.getTrainingJarPath();
        modelPath = algorithmCM.getModelPath();

        try {
            new File(modelPath).mkdirs();
        } catch (Exception e) {
            LOGGER.info("Could not create model folder.");
        }

        this.update("Starting process with id: " + processID);

        Map<String, IData> outputMap = new HashMap<String, IData>(1);

        File sourceDataFile = null;

        File newSourceDataFile = null;

        File trainingDataFile = null;

        File newTrainingDataFile = null;

        File initialModelParametersFile = null;

        URL trainingDataURL = null;

        try {
            IData trainingDataData = getSingleInputData(inputs, inputIDTrainingData);

            if(trainingDataData instanceof LiteralAnyURIBinding){

                trainingDataURL = ((LiteralAnyURIBinding)trainingDataData).getPayload().toURL();

                LOGGER.debug("Training data URL: " + trainingDataURL);

                trainingDataData = new GenericFileParser().parse(trainingDataURL.openStream(), "image/tiff", null);
            }

            trainingDataFile = ((GenericFileDataBinding)trainingDataData).getPayload().getBaseFile(false);

            newTrainingDataFile = new File("/tmp/training" + UUID.randomUUID().toString().substring(0, 5) + ".tif");

            try {
                FileUtils.copyFile(trainingDataFile, newTrainingDataFile);
            } catch (Exception e) {
                LOGGER.info("Could not copy training data file.", e);
            }

            LOGGER.debug("Training data file exists: " + newTrainingDataFile.exists());

        } catch (Exception e1) {
            LOGGER.error("Could not get mandatory input: " + inputIDTrainingData);
            throw new ExceptionReport("Could not get mandatory input.", ExceptionReport.INVALID_PARAMETER_VALUE, inputIDTrainingData);
        }

        try {
            IData sourceDataData = getSingleInputData(inputs, inputIDSourceData);

            if(sourceDataData instanceof LiteralAnyURIBinding){
                URL sourceDataURL = ((LiteralAnyURIBinding)sourceDataData).getPayload().toURL();

                LOGGER.debug("Source data URL: " + sourceDataURL);

                sourceDataData = new GenericFileParser().parse(sourceDataURL.openStream(), "image/tiff", null);
            }

            sourceDataFile = ((GenericFileDataBinding) sourceDataData).getPayload().getBaseFile(false);

            newSourceDataFile = new File("/tmp/source" + UUID.randomUUID().toString().substring(0, 5) + ".tif");

            try {
                FileUtils.copyFile(sourceDataFile, newSourceDataFile);
            } catch (Exception e) {
                LOGGER.info("Could not copy source data file.", e);
            }

            LOGGER.debug("Source data file exists: " + sourceDataFile.exists());

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
        String parentFolderPath = newSourceDataFile.getParent() + fileSeparator;

        LOGGER.debug("Folder of input files: " + parentFolderPath);

        String sourceDataFileName = newSourceDataFile.getName();

        String trainingDataFileName = newTrainingDataFile.getName();

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

        String modelID = System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 5);

        File newDir = new File(modelPath + File.separatorChar + modelID);

        try {
            FileUtils.copyDirectory(modelOutputFolder, newDir);
        } catch (IOException e2) {
            LOGGER.error("Could not copy model.");
        }

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
        File classifiedTiffImage = new File(outputFolderPath + "classification.tiff");

        if(!classifiedImage.exists()){
            LOGGER.error("Classified image doesn't exist.");
        }

        String storeName = "tb14-ml" + UUID.randomUUID().toString().substring(0, 5);

        String host = geoserverWCSGeneratorCM.getGeoserverHost();
        String port = geoserverWCSGeneratorCM.getGeoserverPort();

        try {
            new GeoServerUploader(geoserverWCSGeneratorCM.getGeoserverUsername(), geoserverWCSGeneratorCM.getGeoserverPassword(), host, port).uploadGeotiff(classifiedTiffImage, storeName);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        String getCoverageLink = "http://"+host+":"+port+"/geoserver/wcs?SERVICE=WCS&REQUEST=GetCoverage&VERSION=2.0.1&CoverageId="+ storeName;

        String url = "http://140.134.48.19/ML/StoreImages.ashx" + "?WCS_URL=" + getCoverageLink;

        String imageID = "";

        try {
            String storageResult = postData(url);

            imageID = extractID(storageResult, "ImageID");

        } catch (IOException e1) {
            LOGGER.error("Could not store images.", e1);
        }

        String storeFeaturesURL = "http://140.134.48.19/ML/StoreFeatures.ashx" + "?WFS_URL=" + getCoverageLink;

        String featureID = "";

        try {
            String storageResult = postData(storeFeaturesURL);

            featureID = extractID(storageResult, "FeatureID");

        } catch (IOException e1) {
            LOGGER.error("Could not store images.", e1);
        }

        try {

            String modelURL = DatabaseFactory.getDatabase().storeComplexValue(UUID.randomUUID() + "model", new GenericFileData(zippedMetricsOutputFolder, "application/zip").getDataStream(), null, "application/zip");

            String storeModelURL = String.format("http://140.134.48.19/ML/StoreModels.ashx?ModelID=%s&Link=%s&Format=Application/zip", modelID, modelURL);

            postData(storeModelURL);

        } catch (IOException e1) {
            LOGGER.error("Could not store images.", e1);
        }

        if(!zippedMetricsOutputFolder.exists() && !zippedModelOutputFolder.exists() && !classifiedImage.exists()){
            LOGGER.error("Something did go wrong with executing the model.");
            throw new ExceptionReport("Something did go wrong with executing the model.", ExceptionReport.NO_APPLICABLE_CODE);
        }

        try {
            outputMap.put(outputIDModel, new GenericFileDataBinding(new GenericFileData(zippedModelOutputFolder, "application/zip")));
            outputMap.put(outputIDModelQuality, new GenericFileDataBinding(new GenericFileData(zippedMetricsOutputFolder, "application/zip")));
            outputMap.put(outputIDClassifiedImage, new GenericFileDataBinding(new GenericFileData(classifiedImage, "image/png")));
        } catch (IOException e) {
            LOGGER.error("Could not create process outputs", e);
        }

        this.update("Finished process with id: " + processID);

        return outputMap;
    }

    private String extractID(String s, String id) {

            String idStart = "<" + id + ">";
            String idEnd = "</" + id + ">";

            int i1 = s.indexOf(idStart) + idStart.length();
            int i2 = s.indexOf(idEnd);

            return s.substring(i1, i2);
    }

    private String postData(String url) throws HttpException, IOException{

        HttpClient client = new HttpClient();
        EntityEnclosingMethod requestMethod  = new PostMethod(url);

        int statusCode = client.executeMethod(requestMethod);

        if (!((statusCode == HttpStatus.SC_OK) || (statusCode == HttpStatus.SC_CREATED))) {
            System.err.println("Method failed: "
                    + requestMethod.getStatusLine());
        }

        // Read the response body.
        byte[] responseBody = requestMethod.getResponseBody();
        return new String(responseBody);

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
