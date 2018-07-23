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
package org.n52.geoprocessing.project.testbed14.gait.configmodules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.n52.geoprocessing.project.testbed14.gait.repository.GaitToolAlgorithmRepository;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationKey;
import org.n52.wps.webapp.api.FormatEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;

public class GaitToolAlgorithmRepositoryCM extends ClassKnowingModule{

    private boolean isActive = true;

    private List<AlgorithmEntry> algorithmEntries;

    public static final String gaitHomeKey = "gait_home";
    public static final String pythonHomeKey = "python_home";
    public static final String pythonPathKey = "python_path";
    public static final String moduleStarterHomeKey = "moduleStarter_home";
    public static final String dataDirKey = "tmp_dir";
    public static final String gisrcDirKey = "gisrc_dir";
    public static final String addonDirKey = "addon_dir";

    private ConfigurationEntry<String> gaitHomeEntry = new StringConfigurationEntry(gaitHomeKey, "Gait Tool Home", "Path to Gait Tool installation, e.g. 'C:\\Program Files\\GAIT-WINDOWS-26' or '/usr/lib/grass70'",
            true, "C:\\Program Files\\GAIT-WINDOWS-26");
    private ConfigurationEntry<String> pythonHomeEntry = new StringConfigurationEntry(pythonHomeKey, "Python Home", "Path to python executable, e.g. 'C:\\python27' or '/usr/bin'",
            true, "C:\\python27");
    private ConfigurationEntry<String> pythonPathEntry = new StringConfigurationEntry(pythonPathKey, "Python Path", "Path to python installation, e.g. 'C:\\python27' or '/usr/lib/python2.7'",
            true, "C:\\python27");
    private ConfigurationEntry<String> moduleStarterHomeEntry = new StringConfigurationEntry(moduleStarterHomeKey, "ModuleStarter Home", "Path to GRASSModuleStarter (wps-grass-bridge), e.g. 'D:\\dev\\grass\\wps-grass-bridge-patched\\gms' or '/home/user/wps-grass-bridge-patched/gms'",
            true, "D:\\dev\\grass\\wps-grass-bridge-patched\\gms");
    private ConfigurationEntry<String> dataDirEntry = new StringConfigurationEntry(dataDirKey, "Data Directory", "Path to the directory in which the data should be stored",
            true, "D:\\tmp\\grass_tmp");
    private ConfigurationEntry<String> gisrcDirEntry = new StringConfigurationEntry(gisrcDirKey, "GISRC File", "Path to GISRC file, e.g. 'C:\\Program Files (x86)\\GRASS GIS 7.0.0\\demolocation\\.grassrc70' or '/home/user/grassdata/.grassrc70'",
            true, "C:\\Program Files (x86)\\GRASS GIS 7.0.0\\demolocation\\.grassrc70");
    private ConfigurationEntry<String> addonDirEntry = new StringConfigurationEntry(addonDirKey, "Addon Directory", "Path to addon Directory, optional.",
            false, "N/A");

    private List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(gaitHomeEntry, pythonHomeEntry, pythonPathEntry, moduleStarterHomeEntry, dataDirEntry, gisrcDirEntry, addonDirEntry);

    private String gaitToolHome;
    private String pythonHome;
    private String pythonPath;
    private String moduleStarterHome;
    private String tmpDir;
    private String gisrcDir;
    private String addonDir;

    public GaitToolAlgorithmRepositoryCM() {
        algorithmEntries = new ArrayList<>();
    }

    @Override
    public String getModuleName() {
        return "GaitToolAlgorithmRepository Configuration Module";
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
        return GaitToolAlgorithmRepository.class.getName();
    }

    public String getGaitToolHome() {
        return gaitToolHome;
    }
    
    @ConfigurationKey(key = gaitHomeKey)
    public void setGaitToolHome(String gaitToolHome) {
        this.gaitToolHome = gaitToolHome;
    }

}
