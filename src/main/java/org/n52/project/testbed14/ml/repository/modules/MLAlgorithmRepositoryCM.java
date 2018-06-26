/*
 * Copyright (C) 2007-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.project.testbed14.ml.repository.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.n52.project.testbed14.ml.repository.MLAlgorithmRepository;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationKey;
import org.n52.wps.webapp.api.FormatEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;

public class MLAlgorithmRepositoryCM extends ClassKnowingModule{

    private boolean isActive = true;

    private List<AlgorithmEntry> algorithmEntries;

//    public static final String outputDirKey = "output_dir";

    public static final String jarPathKey = "jar_path";

//    private ConfigurationEntry<String> outputDirEntry = new StringConfigurationEntry(outputDirKey, "Output directory", "Path to output directory, e.g. /usr/tomcat7/temp/'",
//            true, "/usr/tomcat7/temp/");

    private ConfigurationEntry<String> jarPathEntry = new StringConfigurationEntry(jarPathKey, "Jar path", "Path to executable jar.'",
            true, "/usr/share/decisiontree-classification-0.0.1-SNAPSHOT.jar");

    private List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(jarPathEntry);

//    private String outputDir;
    private String jarPath;

    public MLAlgorithmRepositoryCM() {
        algorithmEntries = new ArrayList<>();
    }

    @Override
    public String getModuleName() {
        return "MLAlgorithmRepository Configuration Module";
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        this.isActive = active;
    }

    @Override
    public ConfigurationCategory getCategory() {
        return ConfigurationCategory.REPOSITORY;
    }

    @Override
    public List<? extends ConfigurationEntry<?>> getConfigurationEntries() {
        return configurationEntries;
    }

    @Override
    public List<AlgorithmEntry> getAlgorithmEntries() {
        return algorithmEntries;
    }

    @Override
    public List<FormatEntry> getFormatEntries() {
        return null;
    }

    @Override
    public String getClassName() {
        return MLAlgorithmRepository.class.getName();
    }

//    public String getOutputDir() {
//        return outputDir;
//    }
//
//    @ConfigurationKey(key = outputDirKey)
//    public void setOutputDir(String outputDir) {
//        this.outputDir = outputDir;
//    }

    public String getJarPath() {
        return jarPath;
    }

    @ConfigurationKey(key = jarPathKey)
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

}
