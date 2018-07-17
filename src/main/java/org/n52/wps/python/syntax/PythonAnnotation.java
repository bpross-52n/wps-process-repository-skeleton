package org.n52.wps.python.syntax;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.n52.wps.io.data.IData;
import org.n52.wps.python.data.DataTypeRegistry;
import org.n52.wps.python.data.RTypeDefinition;

public class PythonAnnotation {

    private AnnotationType type;

    private DataTypeRegistry dataTypeRegistry;

    private Map<Attribute, Object> attributeHash = new HashMap<Attribute, Object>();

    public PythonAnnotation(AnnotationType annotationType, HashMap<Attribute, Object> attrHash, DataTypeRegistry dataTypeRegistry) {
        this.type = annotationType;
        this.attributeHash.putAll(attrHash);
        this.dataTypeRegistry = dataTypeRegistry;
    }

    public AnnotationType getType() {
        return this.type;
    }

    public String getStringValue(Attribute attr) {
        Object value = getObjectValue(attr);
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    public Object getObjectValue(Attribute attr) {
        Object out = this.attributeHash.get(attr);

        if (out == null && attr.getDefValue() != null) {
            out = attr.getDefValue();
        } else if (attr == Attribute.ENCODING) {
            return "";
        }
        if (attr == Attribute.SCHEMA) {
            return "";
        }
        return out;
    }

    public static List<PythonAnnotation> filterAnnotations(Collection<PythonAnnotation> annotations,
            AnnotationType type,
            Attribute attribute,
            String value) throws PythonAnnotationException {
        LinkedList<PythonAnnotation> out = new LinkedList<PythonAnnotation>();
        for (PythonAnnotation annotation : annotations) {
            // type filter:
            if (type == null || annotation.getType() == type) {
                // attribute - value filter:
                if (attribute == null || value == null
                        || annotation.getStringValue(attribute).equalsIgnoreCase(value)) {
                    out.add(annotation);
                }
            }
        }
        return out;
    }

    public static List<PythonAnnotation> filterAnnotations(List<PythonAnnotation> annotations,
            Attribute attribute,
            String value) throws PythonAnnotationException {
        return filterAnnotations(annotations, null, attribute, value);
    }

    public static List<PythonAnnotation> filterAnnotations(List<PythonAnnotation> annotations,
            AnnotationType type) throws PythonAnnotationException {
        return filterAnnotations(annotations, type, null, null);
    }

    public static PythonAnnotation filterFirstMatchingAnnotation(List<PythonAnnotation> annotations,
            Attribute attribute,
            String value) throws PythonAnnotationException {
        Iterator<PythonAnnotation> iterator = filterAnnotations(annotations, null, attribute, value).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }

    public static PythonAnnotation filterFirstMatchingAnnotation(List<PythonAnnotation> annotations,
            AnnotationType type) throws PythonAnnotationException {
        Iterator<PythonAnnotation> iterator = filterAnnotations(annotations, type, null, null).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }

    public boolean containsKey(Attribute key) {
        return this.attributeHash.containsKey(key);
    }

    public void setAttribute(Attribute key,
            int i) {
        this.attributeHash.put(key, i);

    }

    /**
     *
     * @param rClass
     *        - value referring to RAttribute.TYPE
     * @return null or supported IData class for rClass - string
     * @throws PythonAnnotationException  if an exception occurred while trying to get the data class
     */
    public Class< ? extends IData> getDataClass(String rClass) throws PythonAnnotationException {
        RTypeDefinition rType = dataTypeRegistry.getType(rClass);
        return rType.getIDataClass();
    }

    public Class< ? extends IData> getDataClass() throws PythonAnnotationException {
        String rClass = getStringValue(Attribute.TYPE);
        return getDataClass(rClass);
    }

    /**
     * Checks if the type - argument of an annotation refers to complex data
     * @param rClass the R type to check
     * @return it given R type is complex
     * @throws PythonAnnotationException if an invalid data type key was detected
     */
    public boolean isComplex(String rClass) throws PythonAnnotationException {
        return dataTypeRegistry.getType(rClass).isComplex();
    }

    /**
     * @return true, if the type attribute of an Annotation refers to a complex data type
     * @throws PythonAnnotationException if an invalid data type key was detected
     */
    public boolean isComplex() throws PythonAnnotationException {
        return isComplex(this.getStringValue(Attribute.TYPE));
    }

    /**
     *
     * @return null or supported ProcessdescriptionType
     * @throws PythonAnnotationException if an invalid data type key was detected
     */
    public String getProcessDescriptionType() throws PythonAnnotationException {
        String type = getStringValue(Attribute.TYPE);
        RTypeDefinition rdt = dataTypeRegistry.getType(type);
        if (rdt != null) {
            return rdt.getMimeType();
        }

        return null;
    }

}
