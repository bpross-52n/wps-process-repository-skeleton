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
package org.n52.wps.python.repository.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.n52.wps.python.repository.PythonAlgorithmRepository;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationKey;
import org.n52.wps.webapp.api.FormatEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;

public class PythonAlgorithmRepositoryCM extends ClassKnowingModule{

    private boolean isActive = true;

    private List<AlgorithmEntry> algorithmEntries;

    public static final String outputDirKey = "output_dir";

    public static final String workspacePathKey = "workspace_path";

    private ConfigurationEntry<String> outputDirEntry = new StringConfigurationEntry(outputDirKey, "Output Directory", "Path to output Directory.",
            true, "/tmp/");

    private ConfigurationEntry<String> workspacePathEntry = new StringConfigurationEntry(workspacePathKey, "Workspace path", "Path to workspace (python scripts, etc.).",
            true, "/usr/share/git/");

    private List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(outputDirEntry, workspacePathEntry);

    private String outputDir;

    private String workspacePath;

    public PythonAlgorithmRepositoryCM() {
        algorithmEntries = new ArrayList<>();
    }

    @Override
    public String getModuleName() {
        return "PythonAlgorithmRepository Configuration Module";
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
        return PythonAlgorithmRepository.class.getName();
    }

    public String getOutputDir() {
        return outputDir;
    }

    @ConfigurationKey(key=outputDirKey)
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    @ConfigurationKey(key=workspacePathKey)
    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

}
