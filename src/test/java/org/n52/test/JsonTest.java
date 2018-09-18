package org.n52.test;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonTest {

    public static void main(String[] args) {

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode modelIDNode = objectMapper.createObjectNode();

        modelIDNode.put("model_id", "MD23652352");

        System.out.println(modelIDNode);
    }

    public String createJSON(String modelID, String featureID, String imageID, Map<String, String> parameters, String systemInfo, String executionTime){

        String json = "";

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode modelIDNode = objectMapper.createObjectNode();

        modelIDNode.put("model_id", modelID);
        modelIDNode.put("feature_id", featureID);
        modelIDNode.put("image_id", imageID);

        ObjectNode parametersNode = objectMapper.createObjectNode();

        for (String key : parameters.keySet()) {
            parametersNode.put(key, parameters.get(key));
        }

        modelIDNode.set("quality_parameters", parametersNode);

        ObjectNode processinfoNode = objectMapper.createObjectNode();

        return json;

    }

}
