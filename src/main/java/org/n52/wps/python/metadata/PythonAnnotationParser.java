/*
 * Copyright (C) 2010-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.python.metadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.xmlbeans.XmlOptions;
import org.n52.wps.python.data.DataTypeRegistry;
import org.n52.wps.python.syntax.AnnotationSeperator;
import org.n52.wps.python.syntax.AnnotationType;
import org.n52.wps.python.syntax.Attribute;
import org.n52.wps.python.syntax.PythonAnnotation;
import org.n52.wps.python.syntax.PythonAnnotationException;
import org.n52.wps.server.ExceptionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.opengis.wps.x100.ProcessDescriptionType;


public class PythonAnnotationParser {

    private static final String ANNOTATION_CHARACTER = "#";

    private static final String COMMENTED_ANNOTATION_CHARACTER = "##";

    @Autowired
    private DataTypeRegistry dataTypeRegistry;

    private static Logger LOGGER = LoggerFactory.getLogger(PythonAnnotationParser.class);

    public PythonAnnotationParser() {
        LOGGER.debug("New {}", this);
    }

    /**
     *
     * @param script
     *            the script to validate
     * @param identifier
     *            the script id to use for generating a test-process description
     * @return false if the process description is invalid
     * @throws PythonAnnotationException
     *             if an exception occurred while validating
     */
    public boolean validateScript(InputStream script,
            String identifier) throws PythonAnnotationException {
        return validateScriptWithErrors(script, identifier).isEmpty();
    }

    /**
     *
     * @param script
     *            the script to validate
     * @param identifier
     *            the script id to use for generating a test-process description
     * @return a list of the XML validation errors or the exceptions during
     *         validation
     * @throws PythonAnnotationException
     *             if an exception occurred while validatin
     */
    public Collection<Exception> validateScriptWithErrors(InputStream script,
            String identifier) throws PythonAnnotationException {
        ArrayList<Exception> validationErrors = new ArrayList<>();
        XmlOptions validationOptions = new XmlOptions();
        validationOptions.setErrorListener(validationErrors);

        // try to parse annotations:
        List<PythonAnnotation> annotations = null;
        try {
            annotations = parse(script, true);
        } catch (PythonAnnotationException e) {
            LOGGER.error("Error parsing annotations during validation", e);
            validationErrors.add(e);
        }

        if (annotations != null) {
            if (annotations.isEmpty()) {
                validationErrors.add(new PythonAnnotationException("No annotations found"));
            } else {
                // check for exactly one description
                hasOneDescription(identifier, validationErrors, validationOptions, annotations);

                validateMetadataAnnotations(validationErrors, annotations, identifier);
            }
        }

        return validationErrors;
    }

    private void validateMetadataAnnotations(ArrayList<Exception> validationErrors,
            List<PythonAnnotation> annotations,
            String scriptId) throws PythonAnnotationException {
        List<PythonAnnotation> metadata = PythonAnnotation.filterAnnotations(annotations, AnnotationType.METADATA);
        for (PythonAnnotation annotation : metadata) {
            if (annotation.getType().equals(AnnotationType.METADATA)) {
                LOGGER.trace("Validating metadata annotation: {}", annotation);

                // title is set
                String title = annotation.getStringValue(Attribute.TITLE);
                if (title == null || title.isEmpty()) {
                    StringBuilder sb = new StringBuilder().append("Annotation of type '")
                            .append(AnnotationType.METADATA.getStartKey()).append("' in the script '").append(scriptId)
                            .append("' is not valid: ").append(Attribute.TITLE).append(" must be set in ")
                            .append(annotation);
                    validationErrors.add(new Exception(sb.toString()));
                }

                // href is set and valid URL
                String href = annotation.getStringValue(Attribute.HREF);
                if (href == null || href.isEmpty()) {
                    StringBuilder sb = new StringBuilder().append("Annotation of type '")
                            .append(AnnotationType.METADATA.getStartKey()).append("' in the script '").append(scriptId)
                            .append("' is not valid: ").append(Attribute.HREF)
                            .append(" must be set, but annotation is ").append(annotation);
                    validationErrors.add(new Exception(sb.toString()));
                }
                try {
                    URL url = new URL(href);
                    url.toURI();
                } catch (MalformedURLException | URISyntaxException e) {
                    StringBuilder sb = new StringBuilder().append("Annotation of type '")
                            .append(AnnotationType.METADATA.getStartKey()).append("' in the script '").append(scriptId)
                            .append("' is not valid: ").append(Attribute.HREF)
                            .append(" must be a well-formed URL, but annotation is ").append(annotation);
                    validationErrors.add(new Exception(sb.toString()));
                }
            }
        }
    }

    private void hasOneDescription(String identifier,
            ArrayList<Exception> validationErrors,
            XmlOptions validationOptions,
            List<PythonAnnotation> annotations) throws PythonAnnotationException {
        List<PythonAnnotation> descriptions =
                PythonAnnotation.filterAnnotations(annotations, AnnotationType.DESCRIPTION);
        if (descriptions.size() != 1) {
            validationErrors.add(new PythonAnnotationException(
                    "Exactly one description annotation required, but found " + descriptions.size()));
        }

        try {
            // try to create process description from annotations
            ProcessDescriptionCreator descriptionCreator = new ProcessDescriptionCreator(identifier);
            ProcessDescriptionType processType = descriptionCreator.createDescribeProcessType(annotations, identifier);

            boolean valid = processType.validate(validationOptions);
            if (!valid) {
                LOGGER.warn(
                        "Invalid R algorithm '{}'. The process description created from the script is not valid. Validation errors: \n{}",
                        identifier, Arrays.toString(validationErrors.toArray()));

                // save process description in output errors
                StringBuilder sb = new StringBuilder().append("Errorenous XML Process Description, so the script '")
                        .append(identifier).append("' is not valid: ").append(processType.xmlText());
                validationErrors.add(new Exception(sb.toString())); // TODO name
                                                                    // exception
            }

        } catch (ExceptionReport | PythonAnnotationException e) {
            LOGGER.error(
                    "Invalid R algorithm '{}'. Script validation failed when executing process description creator.",
                    identifier, e);
            validationErrors.add(e);
        }
    }

    public List<PythonAnnotation> parseAnnotationsfromScript(InputStream inputScript)
            throws PythonAnnotationException, IOException {
        LOGGER.debug("Starting to parse annotations from script " + inputScript);

        return parse(inputScript, false);
    }

    private ArrayList<PythonAnnotation> parse(InputStream inputScript,
            boolean validationOnly) throws PythonAnnotationException {
        try {
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(inputScript));
            int lineCounter = 0;
            boolean isCurrentlyParsingAnnotation = false;
            StringBuilder annotationString = null;
            AnnotationType annotationType = null;
            ArrayList<PythonAnnotation> annotations = new ArrayList<>();
            String scriptId = null;

            while (lineReader.ready()) {
                String line = lineReader.readLine();
                lineCounter++;

                if (line.trim().startsWith(ANNOTATION_CHARACTER)
                        && !line.trim().startsWith(COMMENTED_ANNOTATION_CHARACTER)) {
                    line = line.split("#", 2)[1];
                    line = line.trim();

                    if (line.isEmpty()) {
                        continue;
                    }

                    LOGGER.trace("Parsing annotation line '{}'", line);
                    if (!isCurrentlyParsingAnnotation) // searches for startKey
                                                       // - expressions in a
                                                       // line
                    {
                        for (AnnotationType anot : AnnotationType.values()) {
                            String startKey = anot.getStartKey().getKey();
                            if (line.contains(startKey)) {
                                LOGGER.trace("Parsing annotation of type {}", startKey);

                                // start parsing an annotation, which might
                                // spread several lines
                                line = line.split(AnnotationSeperator.STARTKEY_SEPARATOR.getKey(), 2)[1];
                                annotationString = new StringBuilder();
                                annotationType = anot;
                                isCurrentlyParsingAnnotation = true;

                                break;
                            }
                        }
                    }
                    try {
                        if (isCurrentlyParsingAnnotation) {
                            String endKey = AnnotationSeperator.ANNOTATION_END.getKey();
                            if (line.contains(endKey)) {
                                line = line.split(endKey, 2)[0];
                                isCurrentlyParsingAnnotation = false;
                                // last line for multiline annotation
                            }

                            annotationString.append(line);
                            if (!isCurrentlyParsingAnnotation) {

                                HashMap<Attribute, Object> attrHash =
                                        hashAttributes(annotationType, annotationString.toString());
                                PythonAnnotation newAnnotation =
                                        new PythonAnnotation(annotationType, attrHash, dataTypeRegistry);
                                if (scriptId == null && annotationType.equals(AnnotationType.DESCRIPTION)) {
                                    scriptId = (String) newAnnotation.getObjectValue(Attribute.IDENTIFIER);
                                }

                                LOGGER.trace("Done parsing annotation {} for script {}", newAnnotation, scriptId);
                                annotations.add(newAnnotation);
                            }
                        }
                    } catch (PythonAnnotationException e) {
                        LOGGER.error("Invalid R script with wrong annotation in Line {}: {}", lineCounter,
                                e.getMessage());
                        throw e;
                    }
                }
            }

            LOGGER.debug("Finished parsing {} annotations from script (set debug to TRACE to see details):\n\t\t{}",
                    annotations.size(), Arrays.deepToString(annotations.toArray()));
            return annotations;

        } catch (RuntimeException | IOException e) {
            LOGGER.error("Error parsing annotations.", e);
            throw new PythonAnnotationException("Error parsing annotations.", e);
        }
    }

    private HashMap<Attribute, Object> hashAttributes(AnnotationType anotType,
            String attributeString) throws PythonAnnotationException {

        HashMap<Attribute, Object> attrHash = new HashMap<>();
        StringTokenizer attrValueTokenizer =
                new StringTokenizer(attributeString, AnnotationSeperator.ATTRIBUTE_SEPARATOR.getKey());
        boolean iterableOrder = true;
        // iterates over the attribute sequence of an Annotation
        Iterator<Attribute> attrKeyIterator = anotType.getAttributeSequence().iterator();

        // Important for sequential order: start attribute contains no value,
        // iteration starts from the second key
        attrKeyIterator.next();

        while (attrValueTokenizer.hasMoreElements()) {
            String attrValue = attrValueTokenizer.nextToken();
            if (attrValue.trim().startsWith("\"")) {

                for (; attrValueTokenizer.hasMoreElements() && !attrValue.trim().endsWith("\"");) {
                    attrValue += AnnotationSeperator.ATTRIBUTE_SEPARATOR + attrValueTokenizer.nextToken();
                }

                attrValue = attrValue.substring(attrValue.indexOf("\"") + 1, attrValue.lastIndexOf("\""));
            }

            if (attrValue.contains(AnnotationSeperator.ATTRIBUTE_VALUE_SEPARATOR.getKey())) {
                iterableOrder = false;

                // in the following case, the annotation contains no sequential
                // order and
                // lacks an explicit attribute declaration --> Annotation cannot
                // be interpreted
                // e.g. value1, value2, attribute9 = value9, value4 --> parser
                // error for "value4"
            } else if (!iterableOrder) {
                throw new PythonAnnotationException("Annotation contains no valid order: " + "\""
                        + anotType.getStartKey().getKey() + " " + attributeString + "\"");
            }

            // Valid annotations:
            // 1) Annotation with a sequential attribute order:
            // wps.in: name,description,0,1;
            // 2) Annotation with a partially sequential attribute order:
            // wps.in: name,description, maxOccurs = 1;
            // 3) Annotations without sequential order:
            // wps.des: abstract = example process, title = Example1;
            if (iterableOrder) {
                attrHash.put(attrKeyIterator.next(), attrValue.trim());

            } else {
                String[] keyValue = attrValue.split(AnnotationSeperator.ATTRIBUTE_VALUE_SEPARATOR.getKey());
                Attribute attribute = anotType.getAttribute(keyValue[0].trim());
                String value = keyValue[1].trim();
                if (value.startsWith("\"")) {

                    for (; attrValueTokenizer.hasMoreElements() && !value.trim().endsWith("\"");) {
                        value += AnnotationSeperator.ATTRIBUTE_SEPARATOR + attrValueTokenizer.nextToken();
                    }

                    value = value.substring(value.indexOf("\"") + 1, value.lastIndexOf("\""));
                }

                attrHash.put(attribute, value);
            }
        }
        return attrHash;
    }

}
