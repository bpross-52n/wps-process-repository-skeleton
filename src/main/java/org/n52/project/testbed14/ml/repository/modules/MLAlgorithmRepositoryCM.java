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

    public static final String trainingJarPathKey = "training_jar_path";

    public static final String executionJarPathKey = "execution_jar_path";

    private ConfigurationEntry<String> trainingJarPathEntry = new StringConfigurationEntry(trainingJarPathKey, "Jar path for training", "Path to executable jar for training.'",
            true, "/usr/share/decisiontree-classification-0.0.1-SNAPSHOT.jar");

    private ConfigurationEntry<String> executionJarPathEntry = new StringConfigurationEntry(executionJarPathKey, "Jar path for execution", "Path to executable jar for classification execution.'",
            true, "/usr/share/decisiontree-classification-0.0.1-SNAPSHOT-execution.jar");

    private List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(trainingJarPathEntry, executionJarPathEntry);

    private String trainingJarPath;

    private String executionJarPath;

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

    public String getTrainingJarPath() {
        return trainingJarPath;
    }

    @ConfigurationKey(key = trainingJarPathKey)
    public void setTrainingJarPath(String trainingJarPath) {
        this.trainingJarPath = trainingJarPath;
    }

    public String getExecutionJarPath() {
        return executionJarPath;
    }

    @ConfigurationKey(key = executionJarPathKey)
    public void setExecutionJarPath(String executionJarPath) {
        this.executionJarPath = executionJarPath;
    }

}
