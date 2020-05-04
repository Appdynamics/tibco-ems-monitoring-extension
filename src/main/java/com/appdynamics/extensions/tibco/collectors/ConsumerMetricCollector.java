/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.tibco.TibcoEMSMetricFetcher;
import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.base.Strings;
import com.tibco.tibjms.admin.ConsumerInfo;
import com.tibco.tibjms.admin.StatData;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public class ConsumerMetricCollector extends AbstractMetricCollector {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(ConsumerMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;
    private Map<String, String> queueTopicMetricPrefixes;
    private Boolean displayDynamicIdsInMetricPath;


    public ConsumerMetricCollector(TibjmsAdmin conn, List<Pattern> includePatterns, boolean showSystem,
                                   boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics, Map<String, String> queueTopicMetricPrefixes, Boolean displayDynamicIdsInMetricPath) {
        super(conn, includePatterns, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
        this.queueTopicMetricPrefixes = queueTopicMetricPrefixes;
        this.displayDynamicIdsInMetricPath = displayDynamicIdsInMetricPath;
    }

    public void run() {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting consumer info");
        }
        try {
            ConsumerInfo[] consumers = conn.getConsumersStatistics();

            if (consumers == null || consumers.length <= 0) {
                logger.info("No consumers found to get the consumers metrics");
                return;
            }

            for (ConsumerInfo consumerInfo : consumers) {

                String thisPrefix = metrics.getMetricPrefix();


                String destinationName = consumerInfo.getDestinationName();

                if (shouldMonitorDestination(destinationName, includePatterns, showSystem, showTemp, TibcoEMSMetricFetcher.DestinationType.CONSUMER, logger)) {
                    logger.info("Publishing metrics for consumer " + destinationName);
                    List<com.appdynamics.extensions.metrics.Metric> consumerInfoMetrics = getConsumerInfo(consumerInfo, thisPrefix);
                    collectedMetrics.addAll(consumerInfoMetrics);
                }
            }
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting consumer metrics", e);
        } finally {
            logger.debug("ConsumerMetricCollector Phaser arrived");
            phaser.arriveAndDeregister();
        }
    }

    private List<com.appdynamics.extensions.metrics.Metric> getConsumerInfo(ConsumerInfo consumerInfo, String thisPrefix) {

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = new ArrayList<>();


        String prefix;
        if (Strings.isNullOrEmpty(thisPrefix)) {
            prefix = "Consumers|";
        } else {
            prefix = thisPrefix + "|";
        }

        if (displayDynamicIdsInMetricPath) {
            prefix += consumerInfo.getID() + "|";
        }

        int destinationType = consumerInfo.getDestinationType();
        String destinationName = consumerInfo.getDestinationName();

        String destinationPrefix;
        if (destinationType == 2) {
            String topicPrefix = queueTopicMetricPrefixes.get(TibcoEMSMetricFetcher.DestinationType.TOPIC.getType());

            if (Strings.isNullOrEmpty(topicPrefix)) {
                destinationPrefix = "Topics|";
            } else {
                destinationPrefix = topicPrefix + "|";
            }
        } else {

            String queuePrefix = queueTopicMetricPrefixes.get(TibcoEMSMetricFetcher.DestinationType.QUEUE.getType());

            if (Strings.isNullOrEmpty(queuePrefix)) {
                destinationPrefix = "Queues|";
            } else {
                destinationPrefix = queuePrefix + "|";
            }
        }
        destinationPrefix += destinationName + "|" + prefix;

        Metric[] consumerMetrics = metrics.getMetrics();

        StatData statistics = consumerInfo.getStatistics();

        for (Metric metric : consumerMetrics) {

            String name = metric.getAttr();
            BigDecimal value = null;

            if ("TotalMessages".equalsIgnoreCase(name) && statistics != null) {
                long totalMessages = statistics.getTotalMessages();
                value = BigDecimal.valueOf(totalMessages);
            } else if ("TotalBytes".equalsIgnoreCase(name) && statistics != null) {
                long totalBytes = statistics.getTotalBytes();
                value = BigDecimal.valueOf(totalBytes);
            } else if ("MessageRate".equalsIgnoreCase(name) && statistics != null) {
                long messageRate = statistics.getMessageRate();
                value = BigDecimal.valueOf(messageRate);
            }

            Map<String, String> propertiesMap = objectMapper.convertValue(metric, Map.class);


            StringBuilder sb = new StringBuilder(metricPrefix);
            sb.append("|");
            if (!Strings.isNullOrEmpty(destinationPrefix)) {
                sb.append(destinationPrefix);
            }
            if(!sb.toString().endsWith("|")) {
                sb.append("|");
            }
            sb.append(name);

            String fullMetricPath = sb.toString();

            com.appdynamics.extensions.metrics.Metric thisMetrix = new com.appdynamics.extensions.metrics.Metric(name, value.toString(), fullMetricPath, propertiesMap);
            collectedMetrics.add(thisMetrix);
        }
        return collectedMetrics;
    }
}