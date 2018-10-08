package org.n52.wps.python.data.quakeml;

import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IComplexData;

public class QuakeMLDataBinding implements IComplexData {

    /**
     *
     */
    private static final long serialVersionUID = 5635195859106892797L;

    protected GenericFileData payload;

    public QuakeMLDataBinding(GenericFileData fileData){
        this.payload = fileData;
    }

    public GenericFileData getPayload() {
        return payload;
    }

    public Class<GenericFileData> getSupportedClass() {
        return GenericFileData.class;
    }

    @Override
    public void dispose(){}
}