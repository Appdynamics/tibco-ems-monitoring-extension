/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.appdynamics.extensions.tibco.util.Constants;
import com.appdynamics.extensions.util.AssertUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Satish Muddam
 */
public class TibcoEMSMonitor extends ABaseMonitor {

    private static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|Tibco EMS|";

    private static final org.slf4j.Logger logger = ExtensionsLoggerFactory.getLogger(TibcoEMSMonitor.class);

    public TibcoEMSMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    public String getMonitorName() {
        return "Tibco EMS Monitor";
    }

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {

        List<Map<String, ?>> emsServers = (List<Map<String, ?>>)
                this.getContextConfiguration().getConfigYml().get(Constants.SERVERS);

        for (Map<String, ?> emsServer : emsServers) {

            TibcoEMSMetricFetcher task = new TibcoEMSMetricFetcher(tasksExecutionServiceProvider, this.getContextConfiguration(), emsServer);

            if (emsServers.size() > 1) {
                AssertUtils.assertNotNull(emsServer.get(Constants.DISPLAY_NAME),
                        "The displayName can not be null");
            }
            tasksExecutionServiceProvider.submit((String) emsServer.get(Constants.DISPLAY_NAME), task);
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        List<Map<String, ?>> servers = (List<Map<String, ?>>) this.getContextConfiguration().getConfigYml().get(Constants.SERVERS);
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        getContextConfiguration().setMetricXml(args.get("metric-file"), Metrics.EMSMetrics.class);
    }
}