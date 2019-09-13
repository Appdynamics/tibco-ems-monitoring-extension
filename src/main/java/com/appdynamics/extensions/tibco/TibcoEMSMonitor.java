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
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.appdynamics.extensions.tibco.util.Constants;
import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Satish Muddam
 */
public class TibcoEMSMonitor extends ABaseMonitor {

    private static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|Tibco EMS|";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TibcoEMSMonitor.class);

    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_ARG = "metric-file";

    private Cache<String, TibjmsAdmin> connectionCache;

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

            TibcoEMSMetricFetcher task = new TibcoEMSMetricFetcher(tasksExecutionServiceProvider, this.getContextConfiguration(), emsServer, connectionCache);

            if (emsServers.size() > 1) {
                AssertUtils.assertNotNull(emsServer.get(Constants.DISPLAY_NAME),
                        "The displayName can not be null");
            }
            tasksExecutionServiceProvider.submit((String) emsServer.get(Constants.DISPLAY_NAME), task);
        }
    }

    protected int getTaskCount() {
        List<Map<String, ?>> servers = (List<Map<String, ?>>) this.getContextConfiguration().getConfigYml().get(Constants.SERVERS);
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers.size();
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        getContextConfiguration().setMetricXml(args.get("metric-file"), Metrics.EMSMetrics.class);

        int connectionCacheTimeoutInMinutes = (Integer) getContextConfiguration().getConfigYml().get("connectionCacheTimeoutInMinutes");
        if (connectionCache == null) {
            connectionCache = CacheBuilder.newBuilder().expireAfterAccess(connectionCacheTimeoutInMinutes, TimeUnit.MINUTES)
                    .removalListener(new RemovalListener<String, TibjmsAdmin>() {
                        @Override
                        public void onRemoval(RemovalNotification<String, TibjmsAdmin> removalNotification) {

                            TibjmsAdmin connection = removalNotification.getValue();
                            if (connection != null) {
                                try {
                                    logger.info("Closing the evicted connection for the server {}", removalNotification.getKey());
                                    connection.close();
                                } catch (TibjmsAdminException e) {
                                    logger.error("Error while closing the connection", e);
                                }
                            }

                        }
                    }).build();
        }

        //Need to call cleanUp explicitly to invoke removeListener on the expired entry
        //Call the cleanup every connectionCacheTimeoutInMinutes + 1 minutes to include all the expired entries at connectionCacheTimeoutInMinutes minutes.
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                connectionCache.cleanUp();
            }
        }, connectionCacheTimeoutInMinutes, connectionCacheTimeoutInMinutes + 1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        Logger.getRootLogger().addAppender(ca);

        final TibcoEMSMonitor monitor = new TibcoEMSMonitor();

        final Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put(CONFIG_ARG, "src/main/resources/conf/config.yml");
        taskArgs.put(METRIC_ARG, "src/main/resources/conf/metrics.xml");

        //monitor.execute(taskArgs, null);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitor.execute(taskArgs, null);
            } catch (Exception e) {
                logger.error("Error while running the task", e);
            }
        }, 2, 30, TimeUnit.SECONDS);

    }
}