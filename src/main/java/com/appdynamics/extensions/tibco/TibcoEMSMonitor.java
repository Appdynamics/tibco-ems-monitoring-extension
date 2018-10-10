/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.crypto.Decryptor;
import com.appdynamics.extensions.crypto.Encryptor;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Satish Muddam
 */
public class TibcoEMSMonitor extends AManagedMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|Tibco EMS|";

    private static final Logger logger = Logger.getLogger(TibcoEMSMonitor.class);

    private static final String CONFIG_ARG = "config-file";

    private boolean initialized;
    private MonitorConfiguration configuration;
    private Map<String, Map<String, String>> cachedStats;

    public TibcoEMSMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
        cachedStats = new HashMap<String, Map<String, String>>();
    }

    private static String getImplementationVersion() {
        return TibcoEMSMonitor.class.getPackage().getImplementationTitle();
    }

    public TaskOutput execute(Map<String, String> args, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        logger.info("Starting the Tibco EMS Monitoring task.");

        Thread thread = Thread.currentThread();
        ClassLoader originalCl = thread.getContextClassLoader();
        thread.setContextClassLoader(AManagedMonitor.class.getClassLoader());

        try {
            if (!initialized) {
                initialize(args);
            }
            configuration.executeTask();

            logger.info("Finished Tibco EMS monitor execution");
            return new TaskOutput("Finished Tibco EMS monitor execution");
        } catch (Exception e) {
            logger.error("Failed to execute the Tibco EMS monitoring task", e);
            throw new TaskExecutionException("Failed to execute the Tibco EMS monitoring task" + e);
        } finally {
            thread.setContextClassLoader(originalCl);
        }
    }

    private void initialize(Map<String, String> argsMap) {
        if (!initialized) {
            final String configFilePath = argsMap.get(CONFIG_ARG);

            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            conf.setConfigYml(configFilePath);

            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.METRIC_PREFIX,
                    MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            if (!initialized) {
                logger.info("Tibco EMS Monitor is still initializing");
                return;
            }

            Map<String, ?> config = configuration.getConfigYml();

            List<Map> emsServers = (List<Map>) config.get("emsServers");

            if (emsServers == null || emsServers.isEmpty()) {
                logger.error("No EMS servers configured in config.yml");
                return;
            }

            for (Map emsServer : emsServers) {
                TibcoEMSMetricFetcher task = new TibcoEMSMetricFetcher(configuration, emsServer, cachedStats);
                configuration.getExecutorService().execute(task);
            }
        }
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        logger.getRootLogger().addAppender(ca);

        TibcoEMSMonitor monitor = new TibcoEMSMonitor();

        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "F:\\AppDynamics\\extensions\\tibcoems-monitoring-extension\\src\\main\\resources\\conf\\config.yml");

        monitor.execute(taskArgs, null);

    }

}
