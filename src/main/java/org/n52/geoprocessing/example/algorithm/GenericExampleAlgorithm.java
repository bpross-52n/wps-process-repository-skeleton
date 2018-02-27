package org.n52.geoprocessing.example.algorithm;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x100.ProcessDescriptionsDocument;

public class GenericExampleAlgorithm extends AbstractObservableAlgorithm {

    private static Logger LOGGER = LoggerFactory
            .getLogger(GenericExampleAlgorithm.class);

    private List<String> errors = new ArrayList<>();

    private String processID;
    private final String inputID = "input";
    private String outputID = "output";

    public GenericExampleAlgorithm(){

    }

    public GenericExampleAlgorithm(String processID) {
        this.processID = processID;
    }

    @Override
    public List<String> getErrors() {
        return errors ;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        //ignore check for right id atm
//        if(id.equals("myID")){
//            return MyDataBinding.class;
//        }
        return GenericFileDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        //ignore check for right id atm
//      if(id.equals("myID")){
//          return MyDataBinding.class;
//      }
        return GenericFileDataBinding.class;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputs) throws ExceptionReport {
        LOGGER.info("Starting process with id: " + processID);
        this.update("Starting process with id: " + processID);

        //now you can do what you want, e.g. start an external program based on the processID
        //for this example, we will just return the input as process output

        List<IData> inputList = inputs.get(inputID);

        //we just take the first input, omitting a check for the list size
        IData inputData = inputList.get(0);

        Map<String, IData> outputMap = new HashMap<String, IData>(1);

        outputMap.put(outputID, inputData);

        LOGGER.info("Finished process with id: " + processID);
        this.update("Finished process with id: " + processID);

        return outputMap;
    }

    @Override
    public ProcessDescription getDescription() {

        try {
            InputStream in = getClass().getResourceAsStream("GenericExampleAlgorithm.xml");

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
