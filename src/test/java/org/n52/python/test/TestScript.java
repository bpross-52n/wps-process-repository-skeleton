package org.n52.python.test;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.n52.wps.python.metadata.ProcessDescriptionCreator;
import org.n52.wps.python.metadata.PythonAnnotationParser;
import org.n52.wps.python.syntax.PythonAnnotation;
import org.n52.wps.python.syntax.PythonAnnotationException;
import org.n52.wps.server.ExceptionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x100.ProcessDescriptionType;

public class TestScript {

    private static Logger LOGGER = LoggerFactory.getLogger(TestScript.class);

    @Test
    public void testScript(){

        try {
            List<PythonAnnotation> annotations = new PythonAnnotationParser().parseAnnotationsfromScript(getClass().getResourceAsStream("test.py"));

            ProcessDescriptionType processDescription = new ProcessDescriptionCreator("test.echo").createDescribeProcessType(annotations, "test.echo");

            LOGGER.info(processDescription.xmlText());

        } catch (PythonAnnotationException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (ExceptionReport e) {
            LOGGER.error(e.getMessage());
        }
    }

}
