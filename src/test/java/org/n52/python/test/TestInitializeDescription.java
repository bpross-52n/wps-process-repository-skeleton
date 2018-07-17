package org.n52.python.test;

import org.junit.Test;
import org.n52.wps.python.algorithm.GenericPythonProcess;
import org.n52.wps.python.metadata.PythonAnnotationParser;
import org.n52.wps.webapp.common.AbstractITClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TestInitializeDescription extends AbstractITClass{

    @Autowired
    private PythonAnnotationParser parser;

    private static Logger LOGGER = LoggerFactory.getLogger(TestInitializeDescription.class);

    @Test
    public void testInitializeDescription(){
        LOGGER.info(new GenericPythonProcess("test.echo", parser).getDescription().getProcessDescriptionType("1.0.0").xmlText());
    }

}
