package org.n52.wps.python.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.bbox.BoundingBoxData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.python.util.JavaProcessStreamReader;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShakemapProcess2 extends AbstractAlgorithm {

	private String shakemapInputId = "shakemap";
	private String boundingboxInputId = "bbox";
	private String exposureInputId = "exposure";
	private String exposureOutputId = "exposure-result";
	private File shakemapFile;
	private double[] lowerCorner;
	private File exposureFile;
	private String workspacePath;

    private final String lineSeparator = System.getProperty("line.separator");
	private String outputFileName;
	private GenericFileData selectedRows;
	
	private static Logger LOGGER = LoggerFactory.getLogger(ShakemapProcess2.class);
	
	
	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {
		
		List<IData> shakemapInput = inputData.get(shakemapInputId);
		
		shakemapFile = ((GenericFileDataBinding) shakemapInput.get(0)).getPayload().getBaseFile(false);
		
		List<IData> bboxInput = inputData.get(boundingboxInputId);
		
		BoundingBoxData boundingBox = ((BoundingBoxData) bboxInput.get(0)).getPayload();
		
		lowerCorner = boundingBox.getLowerCorner();
		
		List<IData> exposureInput = inputData.get(exposureInputId);
		
		exposureFile = ((GenericFileDataBinding) exposureInput.get(0)).getPayload().getBaseFile(false);
		
		runScript();
		
		File exposureResultFile = new File("/tmp/exposure.xml");
		
		Map<String, IData> result = new HashMap<>();
		
		try {
			result.put(exposureOutputId, new GenericFileDataBinding(new GenericFileData(exposureResultFile, "text/xml")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	private String getCommand() {
		// TODO Auto-generated method stub
		return "python3 scrupt.py " + shakemapFile.getAbsolutePath() + " " + lowerCorner[0];
	}

    public void runScript() throws ExceptionReport {
        LOGGER.info("Executing python script.");

        try {

            Runtime rt = Runtime.getRuntime();

            String command = getCommand();

            Process proc = rt.exec(command, new String[]{}, new File(workspacePath));

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
            String errors = "";
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line = errorReader.readLine();

                while (line != null) {
                    errors = errors.concat(line + lineSeparator);
                    line = errorReader.readLine();
                }
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java process was interrupted.", e1);
            } finally {
                proc.destroy();
            }

            LOGGER.info(errors);

        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to execute python script.", e);
        }

        File selectedRowCVSFile = new File(workspacePath + "/" + outputFileName);

        try {
            selectedRows = new GenericFileData(selectedRowCVSFile, "text/xml");
        } catch (IOException e) {
            LOGGER.error("Could not create GenericFileData.", e);
        }
    }

	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getInputDataType(String id) {
		if(id.equals(shakemapInputId)){
			return GenericFileDataBinding.class;
		} else if(id.equals(boundingboxInputId)){
			return BoundingBoxData.class;
		} else if(id.equals(exposureInputId)){
			return GenericFileDataBinding.class;
		}
		return null;
	}

	@Override
	public Class<?> getOutputDataType(String id) {
		if(id.equals(exposureOutputId)){
			return GenericFileDataBinding.class;
		}
		return null;
	}

}
