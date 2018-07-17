package org.n52.wps.python.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.python.repository.PythonAlgorithmRepository;
import org.n52.wps.python.repository.modules.PythonAlgorithmRepositoryCM;
import org.n52.wps.python.util.JavaProcessStreamReader;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Algorithm(
        version = "1.0.0")
public class GenericPythonAlgorithm extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(GenericPythonAlgorithm.class);

    private List<String> errors = new ArrayList<>();

    private final String lineSeparator = System.getProperty("line.separator");

    @LiteralDataInput(
            identifier = "lonmin", defaultValue = "288")
    public double lonmin;

    @LiteralDataInput(
            identifier = "lonmax", defaultValue = "292")
    public double lonmax;

    @LiteralDataInput(
            identifier = "latmin", defaultValue = "-70")
    public double latmin;

    @LiteralDataInput(
            identifier = "latmax", defaultValue = "-10")
    public double latmax;

    @LiteralDataInput(
            identifier = "mmin", defaultValue = "6.6")
    public double mmin;

    @LiteralDataInput(
            identifier = "mmax", defaultValue = "8.5")
    public double mmax;

    @LiteralDataInput(
            identifier = "zmin", defaultValue = "5")
    public double zmin;

    @LiteralDataInput(
            identifier = "zmax", defaultValue = "140")
    public double zmax;

    @LiteralDataInput(
            identifier = "p", defaultValue = "0")
    public double p;

    @LiteralDataInput(
            identifier = "etype", allowedValues = { "historic", "deaggregation", "stochastic", "expert" }, defaultValue="historic")
    public String etype;

    private GenericFileData selectedRows;

    private String outputFileName;

    private String outputDir;

    public GenericPythonAlgorithm() {
        // TODO Get from script
        outputFileName = "selected.csv";

        PythonAlgorithmRepositoryCM repositoryCM = (PythonAlgorithmRepositoryCM) WPSConfig.getInstance().getConfigurationModuleForClass(PythonAlgorithmRepository.class.getName(), ConfigurationCategory.REPOSITORY);

        outputDir = repositoryCM.getOutputDir();

    }

    @ComplexDataOutput(identifier = "selected-rows", binding = GenericFileDataBinding.class)
    public GenericFileData getResult() {
        return selectedRows;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Execute
    public void runScript() throws ExceptionReport {
        LOGGER.info("Executing python script.");

        try {

            Runtime rt = Runtime.getRuntime();

            String command = getCommand();

//            LOGGER.info(command);

//            Map<String, String> sysEnv = System.getenv();
//
//            Iterator<Entry<String, String>> iterator = sysEnv.entrySet().iterator();
//
//            while(iterator
//                            .hasNext()){
//                Entry<String, String> entry = iterator.next();
//
//                LOGGER.info(entry.getKey() + " " + entry.getValue());
//            }

            String[] env = {"PATH=/home/bpr/.local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin",
                    "XAUTHORITY=/run/user/1000/gdm/Xauthority",
                    "XMODIFIERS=@im=ibus",
                    "GDMSESSION=ubuntu",
                    "XDG_DATA_DIRS=/usr/share/ubuntu:/usr/local/share:/usr/share:/var/lib/snapd/desktop",
                    "TEXTDOMAINDIR=/usr/share/locale/",
                    "GTK_IM_MODULE=ibus",
                    "DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/1000/bus",
                    "XDG_CURRENT_DESKTOP=ubuntu:GNOME",
                    "SSH_AGENT_PID=1776",
                    "COLORTERM=truecolor",
                    "QT4_IM_MODULE=xim",
                    "SESSION_MANAGER=local/ubuntu:@/tmp/.ICE-unix/1683,unix/ubuntu:/tmp/.ICE-unix/1683",
                    "USERNAME=bpr",
                    "LOGNAME=bpr",
                    "PWD=/home/bpr",
                    "IM_CONFIG_PHASE=2",
                    "GJS_DEBUG_TOPICS=JSERROR;JSLOG",
                    "LESSOPEN=|/usr/bin/lesspipe%s",
                    "SHELL=/bin/bash",
                    "GNOME_DESKTOP_SESSION_ID=this-is-deprecated",
                    "GTK_MODULES=gail:atk-bridge",
                    "CLUTTER_IM_MODULE=xim",
                    "TEXTDOMAIN=im-config",
                    "XDG_SESSION_DESKTOP=ubuntu",
                    "SHLVL=1",
                    "LESSCLOSE=/usr/bin/lesspipe%s%s",
                    "QT_IM_MODULE=xim",
                    "TERM=xterm-256color",
                    "XDG_CONFIG_DIRS=/etc/xdg/xdg-ubuntu:/etc/xdg",
                    "GNOME_TERMINAL_SERVICE=:1.59",
                    "LANG=en_US.UTF-8",
                    "XDG_SESSION_TYPE=x11",
                    "XDG_SESSION_ID=2",
                    "DISPLAY=:0",
                    "_=/usr/bin/java",
                    "GPG_AGENT_INFO=/run/user/1000/gnupg/S.gpg-agent:0:1",
                    "DESKTOP_SESSION=ubuntu",
                    "USER=bpr",
                    "XDG_MENU_PREFIX=gnome-",
                    "VTE_VERSION=5201",
                    "WINDOWPATH=2",
                    "QT_ACCESSIBILITY=1",
                    "GJS_DEBUG_OUTPUT=stderr",
                    "XDG_SEAT=seat0",
                    "SSH_AUTH_SOCK=/run/user/1000/keyring/ssh",
                    "GNOME_SHELL_SESSION_MODE=ubuntu",
                    "XDG_RUNTIME_DIR=/run/user/1000",
                    "XDG_VTNR=2",
                    "HOME=/home/bpr"};

            Process proc = rt.exec(command, env);

            PipedOutputStream pipedOut = new PipedOutputStream();

            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            // attach error stream reader
            JavaProcessStreamReader errorStreamReader =
                    new JavaProcessStreamReader(proc.getErrorStream(), "ERROR", pipedOut);

            // attach output stream reader
            JavaProcessStreamReader outputStreamReader = new JavaProcessStreamReader(proc.getInputStream(), "OUTPUT");

            // start them
            errorStreamReader.start();
            outputStreamReader.start();

            // fetch errors if there are any
            String errors = "";
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line = errorReader.readLine();

                while (line != null) {
                    errors = errors.concat(line + lineSeparator);
                    line = errorReader.readLine();
                }
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java process was interrupted.", e1);
            } finally {
                proc.destroy();
            }

            LOGGER.info(errors);

        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to execute python script.", e);
        }

        File selectedRowCVSFile = new File(outputDir + "/" + outputFileName);

        try {
            selectedRows = new GenericFileData(selectedRowCVSFile, "text/csv");
        } catch (IOException e) {
            LOGGER.error("Could not create GenericFileData.", e);
        }
    }

    private String getCommand() {
        return "python3 /home/bpr/eventquery.py " + lonmin + " " + lonmax + " " + latmin + " " + latmax + " " + mmin + " " + mmax
                + " " + zmin + " " + zmax + " " + p + " " + etype;
    }

}
