package org.n52.wps.python.data.quakeml;

import org.geotools.feature.FeatureCollection;
import org.n52.wps.io.data.IComplexData;

public class QuakeMLDataBinding implements IComplexData {

    /**
     *
     */
    private static final long serialVersionUID = 5635195859106892797L;

    private FeatureCollection featureCollection;

    public QuakeMLDataBinding(FeatureCollection featureCollection) {
        this.featureCollection = featureCollection;
    }

    @Override
    public FeatureCollection getPayload() {
        return featureCollection;
    }

    @Override
    public Class<FeatureCollection> getSupportedClass() {
        return FeatureCollection.class;
    }

    @Override
    public void dispose() {}

}