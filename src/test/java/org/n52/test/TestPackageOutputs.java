package org.n52.test;

import java.io.File;
import java.io.IOException;

import org.n52.wps.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPackageOutputs {

    private static final String fileSeparator = System.getProperty("file.separator");

    private static Logger LOGGER = LoggerFactory
            .getLogger(TestPackageOutputs.class);

    public static void main(String[] args) {

        File outputFolder = new File("C:\\Users\\bpr\\AppData\\Local\\Temp\\def46");

        File modelOutputFolder = new File(outputFolder + fileSeparator + "model");

        File metricsOutputFolder = new File(outputFolder + fileSeparator + "metrics");

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

    }

}
