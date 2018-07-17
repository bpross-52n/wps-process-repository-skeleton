package org.n52.wps.python.metadata;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.n52.wps.io.GeneratorFactory;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.python.syntax.Attribute;
import org.n52.wps.python.syntax.PythonAnnotation;
import org.n52.wps.python.syntax.PythonAnnotationException;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.webapp.api.FormatEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.wps.x100.ComplexDataCombinationsType;
import net.opengis.wps.x100.ComplexDataDescriptionType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralInputType;
import net.opengis.wps.x100.LiteralOutputType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType.DataInputs;
import net.opengis.wps.x100.ProcessDescriptionType.ProcessOutputs;
import net.opengis.wps.x100.SupportedComplexDataType;

public class ProcessDescriptionCreator {

    private static final String DEFAULT_VERSION = "1.0.0";

    private static final Logger log = LoggerFactory.getLogger(ProcessDescriptionCreator.class);

    private String id;

    public ProcessDescriptionCreator(String identifier) {
        this.id = identifier;
    }

    public ProcessDescriptionType createDescribeProcessType(List<PythonAnnotation> annotations,
            String identifier) throws ExceptionReport, PythonAnnotationException {
        this.id = identifier;
        ProcessDescriptionType pdt = ProcessDescriptionType.Factory.newInstance();
        pdt.setStatusSupported(true);
        pdt.setStoreSupported(true);
        ProcessOutputs outputs = pdt.addNewProcessOutputs();
        DataInputs inputs = pdt.addNewDataInputs();

        // iterates over annotations,
        // The annotation type (PythonAnnotationType - enumeration) determines
        // next method call
        for (PythonAnnotation annotation : annotations) {
            log.trace("Adding information to process description based on annotation {}", annotation);

            switch (annotation.getType()) {
            case INPUT:
                addInput(inputs, annotation);
                break;
            case OUTPUT:
                addOutput(outputs, annotation);
                break;
            case DESCRIPTION:
                addProcessDescription(pdt, annotation);
                break;
            default:
                log.trace("Unhandled annotation: {}", annotation);
                break;
            }
        }
        return pdt;
    }

    private void addProcessDescription(ProcessDescriptionType pdt,
            PythonAnnotation annotation) throws PythonAnnotationException {
        pdt.addNewIdentifier().setStringValue(id);

        String abstr = annotation.getStringValue(Attribute.ABSTRACT);
        if (abstr != null && !abstr.isEmpty()) {
            pdt.addNewAbstract().setStringValue(abstr);
        }

        String title = annotation.getStringValue(Attribute.TITLE);
        if (title != null && !title.isEmpty()) {
            pdt.addNewTitle().setStringValue(title);
        }

        String version = annotation.getStringValue(Attribute.VERSION);
        if (version != null && !version.isEmpty()) {
            pdt.setProcessVersion(version);
        } else {
            pdt.setProcessVersion(DEFAULT_VERSION);
        }
    }

    private static void addInput(DataInputs inputs,
            PythonAnnotation annotation) throws PythonAnnotationException {
        InputDescriptionType input = inputs.addNewInput();

        String identifier = annotation.getStringValue(Attribute.IDENTIFIER);
        input.addNewIdentifier().setStringValue(identifier);

        // title is optional in the annotation, therefore it could be null, but
        // it is required in the
        // description - then set to ID
        String title = annotation.getStringValue(Attribute.TITLE);
        if (title != null) {
            input.addNewTitle().setStringValue(title);
        } else {
            input.addNewTitle().setStringValue(identifier);
        }

        String abstr = annotation.getStringValue(Attribute.ABSTRACT);
        // abstract is optional, therefore it can be missing
        if (abstr != null) {
            input.addNewAbstract().setStringValue(abstr);
        }

        String min = annotation.getStringValue(Attribute.MIN_OCCURS);
        BigInteger minOccurs = BigInteger.valueOf(Long.parseLong(min));
        input.setMinOccurs(minOccurs);

        String max = annotation.getStringValue(Attribute.MAX_OCCURS);
        BigInteger maxOccurs = BigInteger.valueOf(Long.parseLong(max));
        input.setMaxOccurs(maxOccurs);

        if (annotation.isComplex()) {
            addComplexInput(annotation, input);
        } else {
            addLiteralInput(annotation, input);

        }
    }

    private static void addLiteralInput(PythonAnnotation annotation,
            InputDescriptionType input) throws PythonAnnotationException {
        LiteralInputType literalInput = input.addNewLiteralData();
        DomainMetadataType dataType = literalInput.addNewDataType();
        dataType.setReference(annotation.getProcessDescriptionType());
        literalInput.setDataType(dataType);
        literalInput.addNewAnyValue();
        String def = annotation.getStringValue(Attribute.DEFAULT_VALUE);
        if (def != null) {
            literalInput.setDefaultValue(def);
        }
    }

    private static void addComplexInput(PythonAnnotation annotation,
            InputDescriptionType input) throws PythonAnnotationException {
        SupportedComplexDataType complexInput = input.addNewComplexData();
        ComplexDataDescriptionType cpldata = complexInput.addNewDefault().addNewFormat();
        cpldata.setMimeType(annotation.getProcessDescriptionType());
        String encod = annotation.getStringValue(Attribute.ENCODING);
        if (encod != null && encod != "base64") {
            cpldata.setEncoding(encod);
        }

        Class<? extends IData> iClass = annotation.getDataClass();
        if (iClass.equals(GenericFileDataBinding.class)) {
            ComplexDataCombinationsType supported = complexInput.addNewSupported();
            ComplexDataDescriptionType format = supported.addNewFormat();
            format.setMimeType(annotation.getProcessDescriptionType());
            encod = annotation.getStringValue(Attribute.ENCODING);
            if (encod != null) {
                format.setEncoding(encod);
            }
            if (encod == "base64") {
                // set a format entry such that not encoded data is supported as
                // well
                ComplexDataDescriptionType format2 = supported.addNewFormat();
                format2.setMimeType(annotation.getProcessDescriptionType());
            }
        } else {
            addSupportedInputFormats(complexInput, iClass);
        }
    }

    private static void addOutput(ProcessOutputs outputs,
            PythonAnnotation out) throws PythonAnnotationException {
        OutputDescriptionType output = outputs.addNewOutput();

        String identifier = out.getStringValue(Attribute.IDENTIFIER);
        output.addNewIdentifier().setStringValue(identifier);

        // title is optional, therefore it could be null; but required in
        // description, so the to id
        String title = out.getStringValue(Attribute.TITLE);
        if (title != null) {
            output.addNewTitle().setStringValue(title);
        } else {
            output.addNewTitle().setStringValue(identifier);
        }

        // is optional, therefore it could be null
        String abstr = out.getStringValue(Attribute.ABSTRACT);
        if (abstr != null) {
            output.addNewAbstract().setStringValue(abstr);
        }

        if (out.isComplex()) {
            addComplexOutput(out, output);
        } else {
            addLiteralOutput(out, output);
        }
    }

    private static void addLiteralOutput(PythonAnnotation out,
            OutputDescriptionType output) throws PythonAnnotationException {
        LiteralOutputType literalOutput = output.addNewLiteralOutput();
        DomainMetadataType dataType = literalOutput.addNewDataType();
        dataType.setReference(out.getProcessDescriptionType());
        literalOutput.setDataType(dataType);
    }

    private static void addComplexOutput(PythonAnnotation out,
            OutputDescriptionType output) throws PythonAnnotationException {
        SupportedComplexDataType complexOutput = output.addNewComplexOutput();
        ComplexDataDescriptionType complexData = complexOutput.addNewDefault().addNewFormat();
        complexData.setMimeType(out.getProcessDescriptionType());

        String encod = out.getStringValue(Attribute.ENCODING);
        if (encod != null && encod != "base64") {
            // base64 shall not be default, but occur in the supported formats
            complexData.setEncoding(encod);
        }
        Class<? extends IData> iClass = out.getDataClass();

        if (iClass.equals(GenericFileDataBinding.class)) {

            ComplexDataCombinationsType supported = complexOutput.addNewSupported();
            ComplexDataDescriptionType format = supported.addNewFormat();
            format.setMimeType(out.getProcessDescriptionType());
            encod = out.getStringValue(Attribute.ENCODING);

            if (encod != null) {
                format.setEncoding(encod);
                if (encod == "base64") {
                    // set a format entry such that not encoded data is
                    // supported as well
                    ComplexDataDescriptionType format2 = supported.addNewFormat();
                    format2.setMimeType(out.getProcessDescriptionType());
                }
            }
        } else {
            addSupportedOutputFormats(complexOutput, iClass);
        }

    }

    /**
     * Searches all available datahandlers for supported encodings / schemas /
     * mime-types and adds them to the supported list of an output
     *
     * @param complex
     *            IData class for which data handlers are searched
     * @param supportedClass
     *            the supported class implementing <code>IData</code>
     */
    private static void addSupportedOutputFormats(SupportedComplexDataType complex,
            Class<? extends IData> supportedClass) {
        // retrieve a list of generators which support the supportedClass-input
        List<IGenerator> generators = GeneratorFactory.getInstance().getAllGenerators();
        List<IGenerator> foundGenerators = new ArrayList<IGenerator>();
        for (IGenerator generator : generators) {
            Class<?>[] supportedClasses = generator.getSupportedDataBindings();
            for (Class<?> clazz : supportedClasses) {
                if (clazz.equals(supportedClass)) {
                    foundGenerators.add(generator);
                }
            }
        }

        ComplexDataCombinationsType supported = complex.addNewSupported();
        for (int i = 0; i < foundGenerators.size(); i++) {
            IGenerator generator = foundGenerators.get(i);
            List<FormatEntry> fullFormats = generator.getSupportedFullFormats();// getSupportedFullFormats();

            for (FormatEntry format : fullFormats) {
                ComplexDataDescriptionType newSupportedFormat = supported.addNewFormat();
                String encoding = format.getEncoding();
                if (encoding != null) {
                    newSupportedFormat.setEncoding(encoding);
                } else {
                    newSupportedFormat.setEncoding(IOHandler.DEFAULT_ENCODING);
                }

                newSupportedFormat.setMimeType(format.getMimeType());
                String schema = format.getSchema();
                if (schema != null) {
                    newSupportedFormat.setSchema(schema);
                }
            }

        }

    }

    /**
     * Searches all available datahandlers for supported encodings / schemas /
     * mime-types and adds them to the supported list of an output
     *
     * @param complex
     *            IData class for which data handlers are searched
     * @param supportedClass
     *            the supported class implementing <code>IData</code>
     */
    private static void addSupportedInputFormats(SupportedComplexDataType complex,
            Class<? extends IData> supportedClass) {
        // retrieve a list of parsers which support the supportedClass-input
        List<IParser> parsers = ParserFactory.getInstance().getAllParsers();
        List<IParser> foundParsers = new ArrayList<IParser>();
        for (IParser parser : parsers) {
            Class<?>[] supportedClasses = parser.getSupportedDataBindings();
            for (Class<?> clazz : supportedClasses) {
                if (clazz.equals(supportedClass)) {
                    foundParsers.add(parser);
                }
            }
        }

        // add properties for each parser which is found
        ComplexDataCombinationsType supported = complex.addNewSupported();
        for (int i = 0; i < foundParsers.size(); i++) {
            IParser parser = foundParsers.get(i);
            List<FormatEntry> fullFormats = parser.getSupportedFullFormats();
            for (FormatEntry format : fullFormats) {
                ComplexDataDescriptionType newSupportedFormat = supported.addNewFormat();
                String encoding = format.getEncoding();
                if (encoding != null) {
                    newSupportedFormat.setEncoding(encoding);
                } else {
                    newSupportedFormat.setEncoding(IOHandler.DEFAULT_ENCODING);
                }
                newSupportedFormat.setMimeType(format.getMimeType());
                String schema = format.getSchema();
                if (schema != null) {
                    newSupportedFormat.setSchema(schema);
                }
            }

        }

    }

}
