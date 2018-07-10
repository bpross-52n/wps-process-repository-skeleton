package org.n52.wps.project.riesgos.shakemap.io;

import org.n52.wps.io.data.IComplexData;

import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridDocument;

public class ShakemapDataBinding implements IComplexData {

    /**
     *
     */
    private static final long serialVersionUID = 5635195859106892797L;

    private ShakemapGridDocument shakemapGrid;

    public ShakemapDataBinding(ShakemapGridDocument shakemapGrid) {
        this.shakemapGrid = shakemapGrid;
    }

    @Override
    public ShakemapGridDocument getPayload() {
        return shakemapGrid;
    }

    @Override
    public Class<ShakemapGridDocument> getSupportedClass() {
        return ShakemapGridDocument.class;
    }

    @Override
    public void dispose() {}

}
