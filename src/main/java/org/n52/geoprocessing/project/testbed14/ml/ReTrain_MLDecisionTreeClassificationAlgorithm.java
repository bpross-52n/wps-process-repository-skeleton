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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.media.jai.JAI;
import javax.xml.namespace.QName;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.identity.GmlObjectIdImpl;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.process.raster.PolygonExtractionProcess;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jaitools.numeric.Range;
import org.n52.geoprocessing.project.testbed14.ml.util.JavaProcessStreamReader;
import org.n52.geoprocessing.project.testbed14.ml.util.JsonUtil;
import org.n52.project.testbed14.ml.repository.MLAlgorithmRepository;
import org.n52.project.testbed14.ml.repository.modules.MLAlgorithmRepositoryCM;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.GTHelper;
import org.n52.wps.io.IOUtils;
import org.n52.wps.io.SchemaRepository;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralAnyURIBinding;
import org.n52.wps.io.datahandler.generator.GeoServerUploader;
import org.n52.wps.io.datahandler.generator.GeoserverWCSGenerator;
import org.n52.wps.io.datahandler.parser.GML2BasicParser;
import org.n52.wps.io.datahandler.parser.GML32BasicParser;
import org.n52.wps.io.datahandler.parser.GML3BasicParser;
import org.n52.wps.io.datahandler.parser.GenericFileParser;
import org.n52.wps.io.datahandler.parser.GeotiffParser;
import org.n52.wps.io.modules.generator.GeoserverWCSGeneratorCM;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.server.database.DatabaseFactory;
import org.n52.wps.server.handler.DataInputInterceptors.DataInputInterceptorImplementations;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.identity.Identifier;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import net.opengis.wps.x100.ProcessDescriptionsDocument;

@DataInputInterceptorImplementations(value="org.n52.wps.server.handler.PassToAlgorithmInputInterceptors")
public class ReTrain_MLDecisionTreeClassificationAlgorithm extends AbstractObservableAlgorithm {

    private static Logger LOGGER = LoggerFactory
            .getLogger(ReTrain_MLDecisionTreeClassificationAlgorithm.class);

    private final String fileSeparator = System.getProperty("file.separator");
    private final String lineSeparator = System.getProperty("line.separator");

    private List<String> errors = new ArrayList<>();

    private String processID;
    private final String inputIDSourceData = "source-data";
    private final String inputIDTrainingData = "training-data";
    private final String inputIDInitialModelParameters = "initial-model-parameters";
    private final String inputIDModel = "input-model";
    private String outputIDModel = "re-trained-model";
    private String outputIDClassifiedImage = "classified-image";
    private String outputIDClassifiedFeatures = "classified-features";
    private String outputIDModelQuality = "model-quality";
    private String jarPath;

    private String modelPath;

    public ReTrain_MLDecisionTreeClassificationAlgorithm(){
    }

    public ReTrain_MLDecisionTreeClassificationAlgorithm(String processID) {
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
        if (id.equals(inputIDModel)) {
            return GenericFileDataBinding.class;
        }
        return GenericFileDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
      if(id.equals(outputIDModel)){
          return GenericFileDataBinding.class;
      }
      if(id.equals(outputIDClassifiedFeatures)){
          return GTVectorDataBinding.class;
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

        File modelParametersFile = null;

        URL trainingDataURL = null;

        boolean needToConvert= false;

        try {
            IData trainingDataData = getSingleInputData(inputs, inputIDTrainingData);

            if(trainingDataData instanceof LiteralAnyURIBinding){

                trainingDataURL = ((LiteralAnyURIBinding)trainingDataData).getPayload().toURL();

                LOGGER.debug("Training data URL: " + trainingDataURL);

                if(trainingDataURL.toString().contains("wfs") || trainingDataURL.toString().contains("WFS")){
                    needToConvert = true;
                    if(trainingDataURL.toString().contains("shp") || trainingDataURL.toString().contains("SHP")){
                        trainingDataData = new GenericFileParser().parse(trainingDataURL.openStream(), "application/x-zipped-shp", null);
                    }else {
                        trainingDataData = parseGML(trainingDataURL.openStream());
                    }
                }else {

                    trainingDataData = new GenericFileParser().parse(trainingDataURL.openStream(), "image/tiff", null);
                }
            }

            try {
                IData modelParametersData = getSingleInputData(inputs, inputIDModel);

                modelParametersFile = ((GenericFileDataBinding) modelParametersData).getPayload().getBaseFile(false);
            } catch (Exception e1) {
                LOGGER.info("Did not get optional input: " + inputIDModel);
            }

            if(modelParametersFile != null){

                String unzippedModelPath = System.getProperty("java.io.tmpdir") + fileSeparator + UUID.randomUUID().toString().substring(0, 5) + fileSeparator;

                unzipFolder(unzippedModelPath, modelParametersFile.getAbsolutePath());
            } else {
                //use latest model

            }

            if(needToConvert){

              SimpleFeatureCollection features = null;

              if(trainingDataData instanceof GenericFileDataBinding){

                  trainingDataFile = ((GenericFileDataBinding)trainingDataData).getPayload().getBaseFile(true);

                  DataStore store = new ShapefileDataStore(trainingDataFile.toURI().toURL());
                  features = store.getFeatureSource(
                          store.getTypeNames()[0]).getFeatures();
              }else{
                  features = createCorrectFeatureCollection(((GTVectorDataBinding)trainingDataData).getPayload());
              }

              GridCoverage2D raster = vectorToRaster(features);

              try {
                  newTrainingDataFile = new File("/tmp/training" + UUID.randomUUID().toString().substring(0, 5) + ".tif");

                  GeoTiffWriter geoTiffWriter = new GeoTiffWriter(newTrainingDataFile);
                  writeGeotiff(geoTiffWriter, raster);
                  geoTiffWriter.dispose();

                  LOGGER.info(newTrainingDataFile.getAbsolutePath());
              } catch (IOException e) {
                  LOGGER.error(e.getMessage());
              }

            }else {

                trainingDataFile = ((GenericFileDataBinding)trainingDataData).getPayload().getBaseFile(true);

                newTrainingDataFile = new File("/tmp/training" + UUID.randomUUID().toString().substring(0, 5) + ".tif");

                try {
                    FileUtils.copyFile(trainingDataFile, newTrainingDataFile);
                } catch (Exception e) {
                    LOGGER.info("Could not copy training data file.", e);
                }
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

        String modelID = "MD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 5);

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

        String featureID = "";
        try {
            featureID = extractAndUploadFeatures(classifiedTiffImage, geoserverWCSGeneratorCM, outputMap);
        } catch (IllegalAttributeException | IOException e2) {
            LOGGER.error("Could not upload features.", e2);
        }

        try {

            String modelURL = DatabaseFactory.getDatabase().storeComplexValue(UUID.randomUUID() + "model", new GenericFileData(zippedModelOutputFolder, "application/zip").getDataStream(), null, "application/zip");

            String storeModelURL = String.format("http://140.134.48.19/ML/StoreModels.ashx?ModelID=%s&Link=%s&Format=Application/zip", modelID, modelURL);

            postData(storeModelURL);

        } catch (IOException e1) {
            LOGGER.error("Could not store images.", e1);
        }

        //create metadata
        File metadataFile = new File(modelOutputFolder.getAbsolutePath() + File.separator + "metadata/part-00000");

        JsonUtil jsonUtil = new JsonUtil(metadataFile);

        String json = jsonUtil.createJSON(modelID, featureID, imageID);

        try {
            //store metadata in Knowledge Base
            String storeMetadataURL = "http://140.134.48.19/ML/StoreMetadata.ashx" + "?json=" + URLEncoder.encode(json, "UTF-8");
            postData(storeMetadataURL);
        } catch (IOException e1) {
            LOGGER.error("Could not store metadata.", e1);
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

    private IData parseGML(InputStream openStream) {

        IData gtvectorDataBinding = null;

        try {
            gtvectorDataBinding = new GML2BasicParser().parse(openStream, "", "");
        } catch (Exception e) {
            LOGGER.warn("Could not parse GML2.");
        }

        if(gtvectorDataBinding != null){
            return gtvectorDataBinding;
        }


        try {
            gtvectorDataBinding = new GML3BasicParser().parse(openStream, "", "");
        } catch (Exception e) {
            LOGGER.warn("Could not parse GML3.");
        }

        if(gtvectorDataBinding != null){
            return gtvectorDataBinding;
        }


        try {
            gtvectorDataBinding = new GML32BasicParser().parse(openStream, "", "");
        } catch (Exception e) {
            LOGGER.warn("Could not parse GML32.");
        }

        if(gtvectorDataBinding != null){
            return gtvectorDataBinding;
        }


        return null;
    }

    private GridCoverage2D vectorToRaster(FeatureCollection<?, ?> featureCollection) {
        Integer rasterWidth = 250;
        Integer rasterHeight = 331;
        String title = "trainingdata";
        String attribute = "value";
        Envelope bounds;
        GridCoverage2D raster = new VectorToRasterProcess().execute((SimpleFeatureCollection) featureCollection, rasterWidth, rasterHeight, title, attribute, null, null);

        return raster;
    }

    private void writeGeotiff(GeoTiffWriter geoTiffWriter, GridCoverage coverage){
        GeoTiffFormat format = new GeoTiffFormat();

        GeoTiffWriteParams wp = new GeoTiffWriteParams();

        wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        wp.setCompressionType("LZW");
        wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        int width = ((GridCoverage2D) coverage).getRenderedImage().getWidth();
        int tileWidth = 1024;
        if(width<2048){
            tileWidth = new Double(Math.sqrt(width)).intValue();
        }
        wp.setTiling(tileWidth, tileWidth);
        ParameterValueGroup paramWrite = format.getWriteParameters();
        paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(256*1024*1024);

        try {
            geoTiffWriter.write(coverage, (GeneralParameterValue[])paramWrite.values().toArray(new
                    GeneralParameterValue[1]));
        } catch (IllegalArgumentException e1) {
            LOGGER.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
        } catch (IndexOutOfBoundsException e1) {
            LOGGER.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
        } catch (IOException e1) {LOGGER.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
        }
    }

    private String extractAndUploadFeatures(File inputTiff, GeoserverWCSGeneratorCM geoserverWCSGeneratorCM, Map<String, IData> outputMap) throws IllegalAttributeException, IOException{

        String host = geoserverWCSGeneratorCM.getGeoserverHost();
        String port = geoserverWCSGeneratorCM.getGeoserverPort();

        InputStream input = new FileInputStream(inputTiff);

        GeotiffParser theParser = new GeotiffParser();

        String[] mimetypes = theParser.getSupportedFormats();

        GTRasterDataBinding theBinding = theParser.parse(input, mimetypes[0],
                null);

        Collection<Number> noDataValues = new ArrayList<>();
        List<Range> classificationRanges = new ArrayList<>();

        noDataValues.add(new Integer(1));

        Range range = new Range<Integer>(new Integer(2) , true, new Integer(3), true);

        classificationRanges.add(range);

        SimpleFeatureCollection vectorFeatures = new PolygonExtractionProcess().execute(theBinding.getPayload(), new Integer(0), true, null, noDataValues, null, null);

        String storeName = "tb14-ml" + UUID.randomUUID().toString().substring(0, 5);

        File shp = getShpFile(vectorFeatures, storeName);

        //zip shp file
        String path = shp.getAbsolutePath();
        String baseName = path.substring(0, path.length() - ".shp".length());
        File shx = new File(baseName + ".shx");
        File dbf = new File(baseName + ".dbf");
        File prj = new File(baseName + ".prj");
        File zipped =org.n52.wps.io.IOUtils.zip(shp, shx, dbf, prj);

        outputMap.put(outputIDClassifiedFeatures, new GTVectorDataBinding(vectorFeatures));

        try {
            new GeoServerUploader(geoserverWCSGeneratorCM.getGeoserverUsername(), geoserverWCSGeneratorCM.getGeoserverPassword(), host, port).uploadShp(zipped, storeName);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        String getFeatureLink = "http://"+host+":"+port+"/geoserver/wfs?SERVICE=WFS&REQUEST=GetFeature&VERSION=2.0.0&typeName="+ storeName;

        String storeFeaturesURL = "http://140.134.48.19/ML/StoreFeatures.ashx" + "?WFS_URL=" + getFeatureLink;

        String featureID = "";

        try {
            String storageResult = postData(storeFeaturesURL);

            featureID = extractID(storageResult, "FeatureID");

        } catch (IOException e1) {
            LOGGER.error("Could not store features.", e1);
        }

        return featureID;

    }

    public static File getShpFile(FeatureCollection<?, ?> collection, String fileName)
            throws IOException, IllegalAttributeException {
        SimpleFeatureType type = null;
        SimpleFeatureBuilder build = null;
        FeatureIterator<?> iterator = collection.features();
        List<SimpleFeature> featureList = new ArrayList<>();
        Transaction transaction = new DefaultTransaction("create");
        FeatureStore<SimpleFeatureType, SimpleFeature> store = null;
        File shp = File.createTempFile(fileName, ".shp");
        shp.deleteOnExit();
        while (iterator.hasNext()) {
            SimpleFeature sf = (SimpleFeature) iterator.next();
            // create SimpleFeatureType
            if (type == null) {
                SimpleFeatureType inType = (SimpleFeatureType) collection
                        .getSchema();
                SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.setName(inType.getName());
                builder.setNamespaceURI(inType.getName().getNamespaceURI());

                if (collection.getSchema().getCoordinateReferenceSystem() == null) {
                    builder.setCRS(DefaultGeographicCRS.WGS84);
                } else {
                    builder.setCRS(collection.getSchema()
                            .getCoordinateReferenceSystem());
                }

                builder.setDefaultGeometry(sf.getDefaultGeometryProperty()
                        .getName().getLocalPart());

                /*
                 * seems like the geometries must always be the first property..
                 * @see also ShapeFileDataStore.java getSchema() method
                 */
                Property geomProperty = sf.getDefaultGeometryProperty();

                //TODO: check if that makes any sense at all
                if(geomProperty.getType().getBinding().getSimpleName().equals("Geometry")){
                Geometry g = (Geometry)geomProperty.getValue();
                if(g!=null){
                    GeometryAttribute geo = null;
                    if(g instanceof MultiPolygon){

                    GeometryAttribute oldGeometryDescriptor = sf.getDefaultGeometryProperty();
                    GeometryType type1 = new GeometryTypeImpl(geomProperty.getName(),MultiPolygon.class, oldGeometryDescriptor.getType().getCoordinateReferenceSystem(),oldGeometryDescriptor.getType().isIdentified(),oldGeometryDescriptor.getType().isAbstract(),oldGeometryDescriptor.getType().getRestrictions(),oldGeometryDescriptor.getType().getSuper(),oldGeometryDescriptor.getType().getDescription());

                    GeometryDescriptor newGeometryDescriptor = new GeometryDescriptorImpl(type1,geomProperty.getName(),0,1,true,null);
                    Identifier identifier = new GmlObjectIdImpl(sf.getID());
                    geo = new GeometryAttributeImpl((Object)g,newGeometryDescriptor, identifier);
                    sf.setDefaultGeometryProperty(geo);
                    sf.setDefaultGeometry(g);
                    }else{
                        //TODO: implement other cases
                    }
                    if(geo != null){
                    builder.add(geo.getName().getLocalPart(), geo
                            .getType().getBinding());
                    }
                }
                }else {
                    builder.add(geomProperty.getName().getLocalPart(), geomProperty
                            .getType().getBinding());
                }

                for (Property prop : sf.getProperties()) {

                    if (prop.getType() instanceof GeometryType) {
                        /*
                         * skip, was handled before
                         */
                    }else {
                        builder.add(prop.getName().getLocalPart(), prop
                                .getType().getBinding());
                    }
                }

                type = builder.buildFeatureType();

                ShapefileDataStore dataStore = new ShapefileDataStore(shp
                        .toURI().toURL());
                dataStore.createSchema(type);
                dataStore.forceSchemaCRS(type.getCoordinateReferenceSystem());

                String typeName = dataStore.getTypeNames()[0];
                store = (FeatureStore<SimpleFeatureType, SimpleFeature>) dataStore
                        .getFeatureSource(typeName);

                store.setTransaction(transaction);

                build = new SimpleFeatureBuilder(type);
            }
            for (AttributeType attributeType : type.getTypes()) {
                build.add(sf.getProperty(attributeType.getName()).getValue());
            }

            SimpleFeature newSf = build.buildFeature(sf.getIdentifier()
                    .getID());

            featureList.add(newSf);
        }

        try {
            store.addFeatures(GTHelper.createSimpleFeatureCollectionFromSimpleFeatureList(featureList));
            transaction.commit();
            return shp;
        } catch (Exception e1) {
            e1.printStackTrace();
            transaction.rollback();
            throw new IOException(e1.getMessage());
        } finally {
            transaction.close();
        }
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
            LOGGER.error("Method failed: "
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

    private SimpleFeatureCollection createCorrectFeatureCollection(FeatureCollection<?,?> fc) throws NoSuchAuthorityCodeException, FactoryException {

        //need mapping between textual categories and values

        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");

        List<SimpleFeature> simpleFeatureList = new ArrayList<SimpleFeature>();
        SimpleFeatureType featureType = null;
        FeatureIterator<?> iterator = fc.features();
        String uuid = UUID.randomUUID().toString();
        int i = 0;
        while(iterator.hasNext()){
            SimpleFeature feature = (SimpleFeature) iterator.next();

            if(i==0){
                featureType = GTHelper.createFeatureType(feature.getProperties(), (Geometry)feature.getDefaultGeometry(), uuid, crs);
                QName qname = GTHelper.createGML3SchemaForFeatureType(featureType);
                SchemaRepository.registerSchemaLocation(qname.getNamespaceURI(), qname.getLocalPart());
            }
            SimpleFeature resultFeature = GTHelper.createFeature("ID"+i, (Geometry)feature.getDefaultGeometry(), featureType, feature.getProperties());

            simpleFeatureList.add(resultFeature);
            i++;
        }
        iterator.close();

        ListFeatureCollection resultFeatureCollection = new ListFeatureCollection(featureType, simpleFeatureList);
        return resultFeatureCollection;

    }

    @Override
    public ProcessDescription getDescription() {

        try {
            InputStream in = getClass().getResourceAsStream("ReTrain_MLDecisionTreeClassificationAlgorithm.xml");

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
