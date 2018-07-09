package org.n52.wps.project.riesgos.shakemap.io;

import org.n52.wps.io.data.IComplexData;

import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGrid;
import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridType;

public class ShakemapDataBinding implements IComplexData {

    /**
     *
     */
    private static final long serialVersionUID = 5635195859106892797L;

    private ShakemapGridType shakemapGrid;

    public ShakemapDataBinding(ShakemapGridType shakemapGrid) {
        this.shakemapGrid = shakemapGrid;
    }

    @Override
    public ShakemapGridType getPayload() {
        return shakemapGrid;
    }

    @Override
    public Class<ShakemapGrid> getSupportedClass() {
        return ShakemapGrid.class;
    }

    @Override
    public void dispose() {}

}
