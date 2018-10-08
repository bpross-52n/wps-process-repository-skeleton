package org.n52.wps.python.algorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.python.data.quakeml.QuakeMLDataBinding;
import org.n52.wps.python.repository.PythonAlgorithmRepository;
import org.n52.wps.python.repository.modules.PythonAlgorithmRepositoryCM;
import org.n52.wps.python.util.JavaProcessStreamReader;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShakemapProcess extends AbstractObservableAlgorithm {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShakemapProcess.class);

    private List<String> errors = new ArrayList<>();

    private String workspacePath;

    public ShakemapProcess() {
        super("org.n52.wps.python.algorithm.ShakemapProcess");
    }

    public ShakemapProcess(String wellKnownName) {
        super(wellKnownName, false);
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {

        try {

            PythonAlgorithmRepositoryCM algorithmRepositoryCM = (PythonAlgorithmRepositoryCM) WPSConfig.getInstance().getConfigurationModuleForClass(PythonAlgorithmRepository.class.getName(), ConfigurationCategory.REPOSITORY);

            workspacePath = algorithmRepositoryCM.getWorkspacePath() + "shakyground/";

            Runtime rt = Runtime.getRuntime();

            List<IData> quakeMLInputList = inputData.get("quakeml-input");

            File quakeMLFile = ((QuakeMLDataBinding)quakeMLInputList.get(0)).getPayload().getBaseFile(false);

            File newQuakeMLFile = File.createTempFile("quakeml", ".xml");

            BufferedReader bufferedReader = new BufferedReader(new FileReader(quakeMLFile));

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(newQuakeMLFile));

            String line = "";

            while((line = bufferedReader.readLine()) != null){
                if(line.contains("<?xml version='1.0' encoding='UTF-8'?>")){
                    line = line.replace("<?xml version='1.0' encoding='UTF-8'?>", "");
                }if(line.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")){
                    line = line.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
                }
                bufferedWriter.write(line);
            }

            bufferedReader.close();
            bufferedWriter.close();

            LOGGER.debug("Quakeml file: " + newQuakeMLFile.getAbsolutePath());

            File outputShakemap = File.createTempFile("shakemap", ".xml");

            String pythonScriptName = "service.py";

            String command = "python3 " +  workspacePath + File.separatorChar + pythonScriptName  + " " + newQuakeMLFile.getAbsolutePath();

            LOGGER.debug("Command: " + command);

            Process proc = rt.exec(command, new String[]{"HOME=" + workspacePath}, new File(workspacePath));

            PipedOutputStream pipedOut = new PipedOutputStream();

            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            FileOutputStream fileOutputStream = new FileOutputStream(new File("/tmp/log" + System.currentTimeMillis() + ".log"));

            // attach error stream reader
            JavaProcessStreamReader errorStreamReader =
                    new JavaProcessStreamReader(proc.getErrorStream(), "ERROR", fileOutputStream);

            // attach output stream reader
            JavaProcessStreamReader outputStreamReader = new JavaProcessStreamReader(proc.getInputStream(), "OUTPUT", pipedOut);

            // start them
            errorStreamReader.start();
            outputStreamReader.start();

            // fetch output
            String output = "";
            try (BufferedReader ouptutReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line2 = "";

                while ((line2 = ouptutReader.readLine()) != null) {
                    output = output.concat(line2 + "\n");
                }
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java process was interrupted.", e1);
            } finally {
                proc.destroy();
            }

            BufferedWriter shakemapWriter = new BufferedWriter(new FileWriter(outputShakemap));

            shakemapWriter.write(output);

            shakemapWriter.close();

            Map<String, IData> result = new HashMap<>();

            result.put("shakemap-output", new GenericFileDataBinding(new GenericFileData(outputShakemap, "text/xml")));

            return result;

        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to execute python script.", e);
            throw new ExceptionReport("Exception occurred while trying to execute python script.", ExceptionReport.NO_APPLICABLE_CODE);
        }
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        return QuakeMLDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return GenericFileDataBinding.class;
    }

}
