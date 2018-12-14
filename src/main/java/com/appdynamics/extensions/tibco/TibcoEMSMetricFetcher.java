/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco;


import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TaskInputArgs;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.tibco.collectors.ConsumerMetricCollector;
import com.appdynamics.extensions.tibco.collectors.DurableMetricCollector;
import com.appdynamics.extensions.tibco.collectors.ProducerMetricCollector;
import com.appdynamics.extensions.tibco.collectors.QueueMetricCollector;
import com.appdynamics.extensions.tibco.collectors.RouteMetricCollector;
import com.appdynamics.extensions.tibco.collectors.ServerMetricCollector;
import com.appdynamics.extensions.tibco.collectors.TopicMetricCollector;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.appdynamics.extensions.tibco.util.Constants;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.tibco.tibjms.TibjmsSSL;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam, Kevin Mcmanus
 */
public class TibcoEMSMetricFetcher implements AMonitorTaskRunnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TibcoEMSMetricFetcher.class);

    private MonitorContextConfiguration configuration;
    private Map<String, ?> emsServer;
    private MetricWriteHelper metricWriteHelper;
    private Metrics.EMSMetrics emsMetrics;
    private String metricPrefix;


    public TibcoEMSMetricFetcher(TasksExecutionServiceProvider serviceProvider, MonitorContextConfiguration configuration, Map<String, ?> emsServer) {
        this.configuration = configuration;
        this.emsServer = emsServer;
        this.metricWriteHelper = serviceProvider.getMetricWriteHelper();
        this.metricPrefix = configuration.getMetricPrefix();

        emsMetrics = (Metrics.EMSMetrics) configuration.getMetricsXml();
    }

    public void onTaskComplete() {
        logger.info("All tasks for server {} finished", this.emsServer.get(Constants.DISPLAY_NAME));

    }

    public enum DestinationType {
        SERVER("Server"), QUEUE("Queue"), TOPIC("Topic"), PRODUCER("Producer"), CONSUMER("Consumer"), ROUTE("Route"), DURABLE("Durable"), CONNECTION("Connection");

        private String type;

        DestinationType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static DestinationType byType(String type) {
            if (SERVER.getType().equalsIgnoreCase(type)) {
                return SERVER;
            } else if (QUEUE.getType().equalsIgnoreCase(type)) {
                return QUEUE;
            } else if (TOPIC.getType().equalsIgnoreCase(type)) {
                return TOPIC;
            } else if (PRODUCER.getType().equalsIgnoreCase(type)) {
                return PRODUCER;
            } else if (CONSUMER.getType().equalsIgnoreCase(type)) {
                return CONSUMER;
            } else if (ROUTE.getType().equalsIgnoreCase(type)) {
                return ROUTE;
            } else if (DURABLE.getType().equalsIgnoreCase(type)) {
                return DURABLE;
            }

            logger.error("Invalid type [ " + type + " ] specified");
            throw new RuntimeException("Invalid type [ " + type + " ] specified");
        }
    }

    public void run() {

        String displayName = (String) emsServer.get("displayName");
        String host = (String) emsServer.get("host");
        String port = (String) emsServer.get("port");
        String protocol = (String) emsServer.get("protocol");
        String user = (String) emsServer.get("user");
        String password = (String) emsServer.get("password");
        String encryptedPassword = (String) emsServer.get("encryptedPassword");
        String encryptionKey = (String) emsServer.get("encryptionKey");
        List<String> faultTolerantServers = (List<String>) emsServer.get("faultTolerantServers");


        String emsURL = String.format("%s://%s:%s", protocol, host, port);
        if (faultTolerantServers != null && faultTolerantServers.size() > 0) {
            emsURL = addFaultTolerantURLs(emsURL, faultTolerantServers);
        }

        logger.debug(String.format("Connecting to %s as %s", emsURL, user));

        String plainPassword = getPassword(password, encryptedPassword, encryptionKey);

        Hashtable sslParams = new Hashtable();

        if (protocol.equals("ssl")) {
            String sslIdentityFile = (String) emsServer.get("sslIdentityFile");
            String sslIdentityPassword = (String) emsServer.get("sslIdentityPassword");
            String sslIdentityEncryptedPassword = (String) emsServer.get("sslIdentityEncryptedPassword");
            String sslIdentityPlainPassword = getPassword(sslIdentityPassword, sslIdentityEncryptedPassword, encryptionKey);

            String sslTrustedCerts = (String) emsServer.get("sslTrustedCerts");


            if (!Strings.isNullOrEmpty(sslIdentityFile)) {
                sslParams.put(TibjmsSSL.IDENTITY, sslIdentityFile);
            }


            if (!Strings.isNullOrEmpty(sslIdentityPlainPassword)) {
                sslParams.put(TibjmsSSL.PASSWORD, sslIdentityPlainPassword);
            }
            if (!Strings.isNullOrEmpty(sslTrustedCerts)) {
                sslParams.put(TibjmsSSL.TRUSTED_CERTIFICATES, sslTrustedCerts);
            }

            String sslDebug = (String) emsServer.get("sslDebug");
            String sslVerifyHost = (String) emsServer.get("sslVerifyHost");
            String sslVerifyHostName = (String) emsServer.get("sslVerifyHostName");
            String sslVendor = (String) emsServer.get("sslVendor");

            if (!Strings.isNullOrEmpty(sslVendor)) {
                sslParams.put(TibjmsSSL.VENDOR, sslVendor);
            }
            if (!Strings.isNullOrEmpty(sslDebug)) {
                sslParams.put(TibjmsSSL.TRACE, sslDebug);
            }
            if (!Strings.isNullOrEmpty(sslVerifyHost)) {
                sslParams.put(TibjmsSSL.ENABLE_VERIFY_HOST, sslVerifyHost);
            }
            if (!Strings.isNullOrEmpty(sslVerifyHostName)) {
                sslParams.put(TibjmsSSL.ENABLE_VERIFY_HOST_NAME, sslVerifyHostName);
            }
        }

        collectMetrics(emsURL, user, plainPassword, sslParams, displayName);

    }

    private String addFaultTolerantURLs(String emsURL, List<String> faultTolerantServers) {
        StringBuilder sb = new StringBuilder(emsURL);
        for (String faultTolerantServer : faultTolerantServers) {
            if (!Strings.isNullOrEmpty(faultTolerantServer)) {
                sb.append(",").append(faultTolerantServer);
            }
        }
        return sb.toString();
    }

    private List<Pattern> buildPattern(List<String> patternStrings) {
        List<Pattern> patternList = new ArrayList<>();

        if (patternStrings != null) {
            for (String pattern : patternStrings) {
                patternList.add(Pattern.compile(pattern));
            }
        }
        return patternList;
    }

    private void collectMetrics(String emsURL, String user, String plainPassword, Hashtable sslParams, String displayName) {

        boolean showTemp = emsMetrics.isShowTemp();
        boolean showSystem = emsMetrics.isShowSystem();
        List<String> includeQueues = (List) emsServer.get("includeQueues");

        List<String> includeTopics = (List) emsServer.get("includeTopics");

        List<String> includeDurables = (List) emsServer.get("includeDurables");

        List<String> includeRoutes = (List) emsServer.get("includeRoutes");

        List<String> includeProducers = (List) emsServer.get("includeProducers");

        List<String> includeConsumers = (List) emsServer.get("includeConsumers");


        List<Pattern> includeQueuesPatterns = buildPattern(includeQueues);

        List<Pattern> includeTopicsPatterns = buildPattern(includeTopics);

        List<Pattern> includeDurablesPatterns = buildPattern(includeDurables);

        List<Pattern> includeRoutesPatterns = buildPattern(includeRoutes);

        List<Pattern> includeProducersPatterns = buildPattern(includeProducers);

        List<Pattern> includeConsumersPatterns = buildPattern(includeConsumers);


        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = new ArrayList<>();

        String fullMetricPrefix;
        if (displayName != null) {
            fullMetricPrefix = refine(metricPrefix) + "|" + displayName;
        } else {
            fullMetricPrefix = refine(metricPrefix) + "|";
        }

        TibjmsAdmin tibjmsAdmin = null;
        try {
            tibjmsAdmin = new TibjmsAdmin(emsURL, user, plainPassword, sslParams);

            Phaser phaser = new Phaser();

            //Register for this task
            phaser.register();
            Metrics[] allMetrics = emsMetrics.getMetrics();

            Map<String, String> queueTopicMetricPrefixes = getQueueTopicMetricPrefixes(allMetrics);

            Boolean displayDynamicIdsInMetricPath = (Boolean) configuration.getConfigYml().get("displayDynamicIdsInMetricPath");

            for (Metrics metrics : allMetrics) {

                String type = metrics.getType();
                Boolean enabled = metrics.isEnabled();

                if (enabled) {
                    switch (DestinationType.byType(type)) {
                        case SERVER:
                            ServerMetricCollector serverMetricCollector = new ServerMetricCollector(tibjmsAdmin, showSystem, showTemp, metrics, fullMetricPrefix, phaser, collectedMetrics);
                            configuration.getContext().getExecutorService().execute(displayName + ": ServerMetricCollector", serverMetricCollector);
                            break;
                        case QUEUE:
                            QueueMetricCollector queueMetricCollector = new QueueMetricCollector(tibjmsAdmin, includeQueuesPatterns, showSystem, showTemp, metrics, fullMetricPrefix, phaser, collectedMetrics);
                            configuration.getContext().getExecutorService().execute(displayName + ": QueueMetricCollector", queueMetricCollector);
                            break;
                        case TOPIC:
                            TopicMetricCollector topicMetricCollector = new TopicMetricCollector(tibjmsAdmin, includeTopicsPatterns, showSystem, showTemp, metrics, fullMetricPrefix, phaser, collectedMetrics);
                            configuration.getContext().getExecutorService().execute(displayName + ": TopicMetricCollector", topicMetricCollector);
                            break;
                        case PRODUCER:
                            ProducerMetricCollector producerMetricCollector = new ProducerMetricCollector(tibjmsAdmin, includeProducersPatterns, showSystem, showTemp, metrics, fullMetricPrefix, phaser, collectedMetrics, queueTopicMetricPrefixes, displayDynamicIdsInMetricPath);
                            configuration.getContext().getExecutorService().execute(displayName + ": ProducerMetricCollector", producerMetricCollector);
                            break;
                        case CONSUMER:
                            ConsumerMetricCollector consumerMetricCollector = new ConsumerMetricCollector(tibjmsAdmin, includeConsumersPatterns, showSystem, showTemp, metrics, fullMetricPrefix, phaser, collectedMetrics, queueTopicMetricPrefixes, displayDynamicIdsInMetricPath);
                            configuration.getContext().getExecutorService().execute(displayName + ": ConsumerMetricCollector", consumerMetricCollector);
                            break;
                        case ROUTE:
                            RouteMetricCollector routeMetricCollector = new RouteMetricCollector(tibjmsAdmin, includeRoutesPatterns, showSystem, showTemp, metrics, fullMetricPrefix, phaser, collectedMetrics);
                            configuration.getContext().getExecutorService().execute(displayName + ": RouteMetricCollector", routeMetricCollector);
                            break;
                        case DURABLE:
                            DurableMetricCollector durableMetricCollector = new DurableMetricCollector(tibjmsAdmin, includeDurablesPatterns, showSystem, showTemp, metrics, fullMetricPrefix, phaser, collectedMetrics, queueTopicMetricPrefixes);
                            configuration.getContext().getExecutorService().execute(displayName + ": DurableMetricCollector", durableMetricCollector);
                            break;
                    }
                }
            }


            //Arrive for this task and Wait for all other tasks
            phaser.arriveAndAwaitAdvance();

        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting metrics from Tibco EMS server [ " + displayName + " ]", e);
        } catch (Exception e) {
            logger.error("Unknown Error while collecting metrics from Tibco EMS server [ " + displayName + " ]", e);
        } finally {
            if (tibjmsAdmin != null) {
                try {
                    tibjmsAdmin.close();
                } catch (TibjmsAdminException e) {
                    logger.error("Error while closing the connection", e);
                }
            }
        }
        if (collectedMetrics.size() > 0) {
            logger.debug("Printing {} metrics", collectedMetrics.size());
            metricWriteHelper.transformAndPrintMetrics(collectedMetrics);
        }
    }

    private Map<String, String> getQueueTopicMetricPrefixes(Metrics[] allMetrics) {
        Map<String, String> queueTopicMetrics = new HashMap<>();
        for (Metrics metrics : allMetrics) {
            if (DestinationType.QUEUE.getType().equals(metrics.getType())) {
                queueTopicMetrics.put(DestinationType.QUEUE.getType(), metrics.getMetricPrefix());
            } else if (DestinationType.TOPIC.getType().equals(metrics.getType())) {
                queueTopicMetrics.put(DestinationType.TOPIC.getType(), metrics.getMetricPrefix());
            }
        }
        return queueTopicMetrics;
    }

    private String refine(String metricPrefix) {
        if (metricPrefix.endsWith("|")) {
            metricPrefix = metricPrefix.substring(0, metricPrefix.length() - 1);
        }
        return metricPrefix;
    }

    private String getPassword(String password, String encryptedPassword, String encryptionKey) {

        if (!Strings.isNullOrEmpty(password)) {
            return password;
        }

        try {
            if (Strings.isNullOrEmpty(encryptedPassword)) {
                return "";
            }
            Map<String, String> args = Maps.newHashMap();
            args.put(TaskInputArgs.ENCRYPTED_PASSWORD, encryptedPassword);
            args.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
            return CryptoUtil.getPassword(args);
        } catch (IllegalArgumentException e) {
            String msg = "Encryption Key not specified. Please set the value in config.yml.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }
}