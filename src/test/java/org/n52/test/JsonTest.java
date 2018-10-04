package org.n52.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonTest {

    private static Logger LOGGER = LoggerFactory
            .getLogger(JsonTest.class);

    public JsonTest() {

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode modelIDNode = objectMapper.createObjectNode();

        modelIDNode.put("model_id", "MD23652352");

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("numFolds", 4);
        parameters.put("seed", "-1191137437");

        Map<String, String> systemInfo = new HashMap<>();

        systemInfo.put("system", "Apache Spark");
        systemInfo.put("version", "2.2.1");

//        System.out.println(createJSON("MD153683552099675754", "FT2018091306452814", "IM2018091306452687", parameters, systemInfo, "2018-09-13T:09:30"));


        File modelOutputsFolder = new File("D:\\tmp\\k345695a\\model");

        File metadataFile = new File(modelOutputsFolder.getAbsolutePath() + File.separator + "metadata/part-00000");

        if(metadataFile.exists()){
            try {
                JsonNode metadataNode = objectMapper.readTree(metadataFile);

                System.out.println(metadataNode.toString());

//                JsonNode parametersNode = metadataNode.path("avgMetrics");


                ObjectNode parametersNode = objectMapper.createObjectNode();

                parametersNode.set("paramMap", metadataNode.path("paramMap"));

                parametersNode.set("avgMetrics", metadataNode.path("avgMetrics"));

                ObjectNode systemInfoNode = objectMapper.createObjectNode();

                systemInfoNode.set("class", metadataNode.path("class"));
                systemInfoNode.set("timestamp", metadataNode.path("timestamp"));
                systemInfoNode.set("sparkVersion", metadataNode.path("sparkVersion"));

                String json = createJSON("MD153683552099675754", "FT2018091306452814", "IM2018091306452687", parametersNode, systemInfoNode);

                //store metadata in Knowledge Base
                String storeMetadataURL = "http://140.134.48.19/ML/StoreMetadata.ashx" + "?json=" + URLEncoder.encode(json, "UTF-8");

                URI uri = new URI(storeMetadataURL);

                System.out.println(uri.toString());

            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                LOGGER.error(e.getMessage());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOGGER.error(e.getMessage());
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                LOGGER.error(e.getMessage());
            }
        }

    }

    public static void main(String[] args) {
        new JsonTest();
    }

    public String createJSON(String modelID, String featureID, String imageID, JsonNode parameters, Map<String, String> systemInfo, String executionTime){

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("model_id", modelID);
        rootNode.put("feature_id", featureID);
        rootNode.put("image_id", imageID);

        rootNode.set("quality_parameters", parameters);

        ObjectNode processinfoNode = objectMapper.createObjectNode();

        for (String key : systemInfo.keySet()) {
            processinfoNode.put(key, systemInfo.get(key));
        }

        processinfoNode.put("execution_time", executionTime);

        rootNode.set("process_information", processinfoNode);

        return rootNode.toString();

    }

    public String createJSON(String modelID, String featureID, String imageID, JsonNode parameters, JsonNode systemInfo){

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("model_id", modelID);
        rootNode.put("feature_id", featureID);
        rootNode.put("image_id", imageID);

        rootNode.set("quality_parameters", parameters);

        rootNode.set("process_information", systemInfo);

        return rootNode.toString();

    }

    public String createJSON(String modelID, String featureID, String imageID, Map<String, Object> parameters, Map<String, String> systemInfo, String executionTime){

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("model_id", modelID);
        rootNode.put("feature_id", featureID);
        rootNode.put("image_id", imageID);

        ObjectNode parametersNode = objectMapper.createObjectNode();

        for (String key : parameters.keySet()) {

            Object o = parameters.get(key);

            if(o instanceof Integer){
                parametersNode.put(key, (int)o);
            }else if(o instanceof String){
                parametersNode.put(key, (String)o);
            }

        }

        rootNode.set("quality_parameters", parametersNode);

        ObjectNode processinfoNode = objectMapper.createObjectNode();

        for (String key : systemInfo.keySet()) {
            processinfoNode.put(key, systemInfo.get(key));
        }

        processinfoNode.put("execution_time", executionTime);

        rootNode.set("process_information", processinfoNode);

        return rootNode.toString();

    }

}
