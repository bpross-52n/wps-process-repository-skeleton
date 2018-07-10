package org.n52.wps.project.riesgos.shakemap.io;

import java.io.IOException;
import java.io.InputStream;

import org.n52.wps.io.data.IData;
import org.n52.wps.io.datahandler.generator.AbstractGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShakemapGenerator extends AbstractGenerator {

    private static Logger LOGGER = LoggerFactory
            .getLogger(ShakemapGenerator.class);

    public ShakemapGenerator() {
        super();
        supportedIDataTypes.add(ShakemapDataBinding.class);
    }

    @Override
    public InputStream generateStream(IData data,
            String mimeType,
            String schema) throws IOException {

        if(data instanceof ShakemapDataBinding){
            return ((ShakemapDataBinding)data).getPayload().newInputStream();
        }

        LOGGER.error("Data not of type ShakemapDataBinding. Returning null.");

        return null;
    }

}
