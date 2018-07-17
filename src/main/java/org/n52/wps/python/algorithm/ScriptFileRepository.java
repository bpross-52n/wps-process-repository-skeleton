package org.n52.wps.python.algorithm;

import java.io.File;

public class ScriptFileRepository {

    public File getValidatedScriptFile(String wkn) throws InvalidPythonScriptException {

        return new File("D:/dev/GitHub4w/wps-process-repository-skeleton/src/test/resources/org/n52/python/test/test.py");
    }

}
