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
import com.tibco.tibjms.admin.DurableInfo;
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
public class DurableMetricCollector extends AbstractMetricCollector {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(DurableMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;
    private Map<String, String> queueTopicMetricPrefixes;


    public DurableMetricCollector(TibjmsAdmin conn, List<Pattern> includePatterns, boolean showSystem,
                                  boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics, Map<String, String> queueTopicMetricPrefixes) {
        super(conn, includePatterns, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
        this.queueTopicMetricPrefixes = queueTopicMetricPrefixes;
    }

    public void run() {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting durables info");
        }

        try {
            DurableInfo[] durables = conn.getDurables();

            if (durables == null) {
                logger.warn("Unable to get durable metrics");
            } else {
                for (DurableInfo durableInfo : durables) {
                    if (shouldMonitorDestination(durableInfo.getDurableName(), includePatterns, showSystem, showTemp, TibcoEMSMetricFetcher.DestinationType.DURABLE, logger)) {
                        logger.info("Publishing metrics for durable " + durableInfo.getDurableName());
                        List<com.appdynamics.extensions.metrics.Metric> durableInfoMetrics = getDurableInfo(durableInfo, metrics);
                        collectedMetrics.addAll(durableInfoMetrics);
                    }
                }
            }
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting durable metrics", e);
        } finally {
            logger.debug("DurableMetricCollector Phaser arrived");
            phaser.arriveAndDeregister();
        }
    }

    private List<com.appdynamics.extensions.metrics.Metric> getDurableInfo(DurableInfo durableInfo, Metrics metrics) {

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = new ArrayList<>();


        String thisPrefix = metrics.getMetricPrefix();
        String prefix;
        if (Strings.isNullOrEmpty(thisPrefix)) {
            prefix = "Durables|" + durableInfo.getDurableName();
        } else {
            prefix = thisPrefix + "|" + durableInfo.getDurableName();
        }

        String topicPrefix = queueTopicMetricPrefixes.get(TibcoEMSMetricFetcher.DestinationType.TOPIC.getType());

        String topicName = durableInfo.getTopicName();

        String destinationPrefix;
        if (Strings.isNullOrEmpty(topicPrefix)) {
            destinationPrefix = "Topics|"+topicName+"|";
        } else {
            destinationPrefix = topicPrefix + "|"+topicName+"|";
        }

        destinationPrefix += prefix;

        Metric[] durableMetrics = metrics.getMetrics();

        for (Metric metric : durableMetrics) {
            String name = metric.getAttr();

            BigDecimal value = null;

            if ("PendingMessageCount".equalsIgnoreCase(name)) {
                long pendingMessageCount = durableInfo.getPendingMessageCount();
                value = BigDecimal.valueOf(pendingMessageCount);
            } else if ("PendingMessageSize".equalsIgnoreCase(name)) {
                long pendingMessageSize = durableInfo.getPendingMessageSize();
                value = BigDecimal.valueOf(pendingMessageSize);
            }

            Map<String, String> propertiesMap = objectMapper.convertValue(metric, Map.class);

            StringBuilder sb = new StringBuilder(metricPrefix);
            sb.append("|");
            if (!Strings.isNullOrEmpty(destinationPrefix)) {
                sb.append(destinationPrefix).append("|");
            }
            sb.append(name);

            String fullMetricPath = sb.toString();

            com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric(name, value.toString(), fullMetricPath, propertiesMap);
            collectedMetrics.add(thisMetric);
        }
        return collectedMetrics;
    }
}