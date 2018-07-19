package org.n52.python.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.python.algorithm.GenericPythonProcess;
import org.n52.wps.python.metadata.PythonAnnotationParser;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.webapp.common.AbstractITClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TestExecute extends AbstractITClass {
    
    @Autowired
    private PythonAnnotationParser parser;

    private static Logger LOGGER = LoggerFactory.getLogger(TestExecute.class);

    @Test
    public void testExecute(){

        Map<String, List<IData>> inputData = new HashMap<>();

        inputData.put("inputVariable", Arrays.asList(new LiteralStringBinding("World!")));

        try {
            GenericPythonProcess pythonProcess = new GenericPythonProcess("test.echo", parser);

            pythonProcess.getDescription();

            pythonProcess.run(inputData);
        } catch (ExceptionReport e) {
            LOGGER.error(e.getMessage());
        }

    }

}
