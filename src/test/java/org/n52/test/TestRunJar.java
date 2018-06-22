package org.n52.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;

import org.n52.geoprocessing.project.testbed14.ml.util.JavaProcessStreamReader;


public class TestRunJar {

    private final String lineSeparator = System.getProperty("line.separator");

    public TestRunJar() {

        File outputFolder = new File(System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID().toString().substring(0, 5));

        outputFolder.mkdir();

        String outputFolderPath = outputFolder.getAbsolutePath() + "/";

        System.out.println(outputFolderPath);

        trainModel("IMG_PHR1B_P_201509271105571_ORT_1974032101-001_R1C1_subset2_downsized3.tif", "labels4.tif", "D:/52n/Projekte/Testbed-14/ML/", outputFolderPath);

    }

    public static void main(String[] args) {
        new TestRunJar();
    }

    private void trainModel(String sourceDataFileName, String trainingDataFileName, String parentFolderPath, String outputFolderPath){

        try {

            Runtime rt = Runtime.getRuntime();

            String command = "java -jar d:/tmp/decisiontree-classification-0.0.1-SNAPSHOT.jar " + sourceDataFileName +
                    " " + trainingDataFileName + " " + parentFolderPath + " " + outputFolderPath;//TODO jar path from properties

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
            String errors = "";
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line = errorReader.readLine();

                while (line != null) {
                    errors = errors.concat(line + lineSeparator);
                    line = errorReader.readLine();
                }
            }

            System.out.println(errors);

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }finally{
                proc.destroy();
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

}
