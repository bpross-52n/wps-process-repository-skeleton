package org.n52.wps.python.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.n52.wps.io.data.IData;
import org.n52.wps.python.metadata.ProcessDescriptionCreator;
import org.n52.wps.python.metadata.PythonAnnotationParser;
import org.n52.wps.python.syntax.AnnotationType;
import org.n52.wps.python.syntax.Attribute;
import org.n52.wps.python.syntax.PythonAnnotation;
import org.n52.wps.python.syntax.PythonAnnotationException;
import org.n52.wps.python.util.JavaProcessStreamReader;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionsDocument;

public class GenericPythonProcess extends AbstractObservableAlgorithm {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericPythonProcess.class);

    private List<PythonAnnotation> annotations;

    private PythonAnnotationParser parser;

    private ScriptFileRepository scriptRepo;

    private List<String> errors = new ArrayList<>();

    public GenericPythonProcess(String wellKnownName, PythonAnnotationParser parser) {
        super(wellKnownName, false);
        this.parser = parser;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {

        try {

            Runtime rt = Runtime.getRuntime();

            //TODO check order
            
//            String command = getCommand();

//            Process proc = rt.exec(command);
            
            List<PythonAnnotation> inAnnotations = PythonAnnotation.filterAnnotations(this.annotations, AnnotationType.INPUT);
            
            String command = "";
            
            for (PythonAnnotation pythonAnnotation : inAnnotations) {
                String id = pythonAnnotation.getStringValue(Attribute.IDENTIFIER);
                
                List<IData> data = inputData.get(id);
                
                if(data == null){
                    continue;
                }
                
                command = data.get(0).getPayload().toString();                
            }
            
            Process proc = rt.exec("python3 /home/bpr/test.py " + command);

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
                    errors = errors.concat(line + "\n");
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
        return null;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessDescription getDescription() {
        return initializeDescription();
    }

    @Override
    protected ProcessDescription initializeDescription() {
        String wkn = getWellKnownName();
        scriptRepo = new ScriptFileRepository();
        // parser = new PythonAnnotationParser();
        LOGGER.debug("Load and validate script for wkn {}", wkn);

        // Reading process information from script annotations:
        File scriptFile = null;
        try {
            scriptFile = scriptRepo.getValidatedScriptFile(wkn);
        } catch (InvalidPythonScriptException e) {
            LOGGER.warn("Could not load script file for {}", wkn, e);
            throw new IllegalStateException("Error creating process description: " + e.getMessage(), e);
        }
        LOGGER.debug("Loaded and valid: {}", scriptFile.getAbsolutePath());

        try (InputStream rScriptStream = new FileInputStream(scriptFile);) {
            LOGGER.info("Initializing description for {}", this.toString());
            this.annotations = this.parser.parseAnnotationsfromScript(rScriptStream);

            // submits annotation with process informations to
            // ProcessdescriptionCreator:
            ProcessDescriptionCreator creator = new ProcessDescriptionCreator(wkn);
            ProcessDescriptionType doc = creator.createDescribeProcessType(this.annotations, wkn);

            if (LOGGER.isTraceEnabled()) {
                ProcessDescriptionsDocument outerDoc = ProcessDescriptionsDocument.Factory.newInstance();
                ProcessDescriptionType type = outerDoc.addNewProcessDescriptions().addNewProcessDescription();
                type.set(doc);
                LOGGER.trace("Created process description for {}:\n{}", wkn, outerDoc.xmlText());
            }

            ProcessDescription processDescription = new ProcessDescription();
            processDescription.addProcessDescriptionForVersion(doc, "1.0.0");
            description = processDescription;
            return processDescription;
        } catch (PythonAnnotationException | IOException | ExceptionReport e) {
            LOGGER.error("Error initializing description for script '{}'", wkn, e);
            throw new RuntimeException("Error while parsing script file or creating process description of script '"
                    + wkn + "': " + e.getMessage(), e);
        }
    }

//    private String getCommand() {
//        return "python3 /home/bpr/eventquery.py " + lonmin + " " + lonmax + " " + latmin + " " + latmax + " " + mmin
//                + " " + mmax + " " + zmin + " " + zmax + " " + p + " " + etype;
//    }

}
