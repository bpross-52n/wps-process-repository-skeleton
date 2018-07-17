package org.n52.wps.python.syntax;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Describes each annotation type considering attributes, their order and behavior
 * Derived from RAnnotationType
 *
 */
public enum AnnotationType {

    INPUT(Arrays.asList(Attribute.INPUT_START, Attribute.IDENTIFIER, Attribute.TYPE, Attribute.TITLE,
            Attribute.ABSTRACT, Attribute.DEFAULT_VALUE, Attribute.MIN_OCCURS, Attribute.MAX_OCCURS)),

    OUTPUT(Arrays.asList(Attribute.OUTPUT_START, Attribute.IDENTIFIER, Attribute.TYPE, Attribute.TITLE,
            Attribute.ABSTRACT)),

    DESCRIPTION(Arrays.asList(Attribute.DESCRIPTION_START, Attribute.IDENTIFIER, Attribute.TITLE, Attribute.VERSION,
            Attribute.ABSTRACT, Attribute.AUTHOR)),

    RESOURCE(Arrays.asList(Attribute.RESOURCE_START, Attribute.NAMED_LIST)),

    IMPORT(Arrays.asList(Attribute.IMPORT_START, Attribute.NAMED_LIST)), METADATA(
            Arrays.asList(Attribute.METADATA_START, Attribute.TITLE, Attribute.HREF));

    private HashMap<String, Attribute> attributeLut = new HashMap<String, Attribute>();

    private HashSet<Attribute> mandatory = new HashSet<Attribute>();

    private Attribute startKey;

    private List<Attribute> attributeSequence;

    private AnnotationType(List<Attribute> attributeSequence) {
        this.startKey = attributeSequence.get(0);
        this.attributeSequence = attributeSequence;

        for (Attribute attribute : this.attributeSequence) {
            this.attributeLut.put(attribute.getKey(), attribute);
            if (attribute.isMandatory()) {
                this.mandatory.add(attribute);
            }
        }
    }

    public Attribute getStartKey() {
        return this.startKey;
    }

    public Attribute getAttribute(String key) throws PythonAnnotationException {
        String k = key.toLowerCase();
        if (this.attributeLut.containsKey(k)) {
            return this.attributeLut.get(k);
        }

        throw new PythonAnnotationException(
                "Annotation " + this + " (" + this.startKey + " ...) cannot contain a parameter named '" + key + "'.");
    }

    public Iterable<Attribute> getAttributeSequence() {
        return this.attributeSequence;

    }

    /**
     * Checks if Annotation content is valid for process description and adds
     * attributes / standard values if necessary
     *
     * @param rAnnotation
     *            the annotation from the RSkript
     * @throws PythonAnnotationException
     *             if the annotation contains syntax errors
     */
    public void validateDescription(PythonAnnotation rAnnotation) throws PythonAnnotationException {
        // check minOccurs Attribute and default value:
        try {
            if (rAnnotation.containsKey(Attribute.MIN_OCCURS)) {
                Integer min = Integer.parseInt(rAnnotation.getStringValue(Attribute.MIN_OCCURS));
                if (rAnnotation.containsKey(Attribute.DEFAULT_VALUE) && !min.equals(0)) {
                    throw new PythonAnnotationException(
                            "Default value found but minimum required number of occurrences is not '0' in annotation "
                                    + this);
                }
            }
        } catch (NumberFormatException e) {
            throw new PythonAnnotationException("Syntax Error in Annotation " + this + " (" + this.startKey + " ...), "
                    + "unable to parse Integer value from attribute " + Attribute.MIN_OCCURS.getKey()
                    + e.getMessage());
        }

        if (rAnnotation.containsKey(Attribute.DEFAULT_VALUE) && !rAnnotation.containsKey(Attribute.MIN_OCCURS)) {
            rAnnotation.setAttribute(Attribute.MIN_OCCURS, 0);
        }

        // check maxOccurs Attribute:
        try {
            if (rAnnotation.containsKey(Attribute.MAX_OCCURS)) {
                Integer.parseInt(rAnnotation.getStringValue(Attribute.MAX_OCCURS));
            }
        } catch (NumberFormatException e) {
            throw new PythonAnnotationException("Syntax Error in Annotation " + this + " (" + this.startKey + " ...), "
                    + "unable to parse Integer value from attribute " + Attribute.MAX_OCCURS.getKey());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RAnnotationType [startKey = ");
        sb.append(this.startKey);
        // sb.append(", attributes = ");
        // sb.append(Arrays.toString(this.attributeSequence.toArray()));
        sb.append("]");
        return sb.toString();
    }

}
