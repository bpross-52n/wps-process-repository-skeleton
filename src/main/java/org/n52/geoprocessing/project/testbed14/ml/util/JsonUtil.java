package org.n52.geoprocessing.project.testbed14.ml.util;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtil {

    private static Logger LOGGER = LoggerFactory
            .getLogger(JsonUtil.class);

    private ObjectNode parametersNode;
    private ObjectNode systemInfoNode;

    public JsonUtil(File metadataFile) {

        ObjectMapper objectMapper = new ObjectMapper();

        if(metadataFile.exists()){
            try {
                JsonNode metadataNode = objectMapper.readTree(metadataFile);

                parametersNode = objectMapper.createObjectNode();

                parametersNode.set("paramMap", metadataNode.path("paramMap"));

                parametersNode.set("avgMetrics", metadataNode.path("avgMetrics"));

                systemInfoNode = objectMapper.createObjectNode();

                systemInfoNode.set("class", metadataNode.path("class"));
                systemInfoNode.set("timestamp", metadataNode.path("timestamp"));
                systemInfoNode.set("sparkVersion", metadataNode.path("sparkVersion"));

            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage());
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }


    public String createJSON(String modelID, String featureID, String imageID){

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("model_id", modelID);
        rootNode.put("feature_id", featureID);
        rootNode.put("image_id", imageID);

        rootNode.set("quality_parameters", parametersNode);

        rootNode.set("process_information", systemInfoNode);

        return rootNode.toString();

    }

}
