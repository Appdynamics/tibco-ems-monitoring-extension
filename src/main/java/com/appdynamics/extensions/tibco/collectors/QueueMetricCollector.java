/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.tibco.TibcoEMSMetricFetcher;
import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.base.Strings;
import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public class QueueMetricCollector extends AbstractMetricCollector {

    private static final Logger logger = Logger.getLogger(QueueMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;


    public QueueMetricCollector(TibjmsAdmin conn, List<Pattern> includePatterns, boolean showSystem,
                                boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics) {
        super(conn, includePatterns, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
    }

    public void run() {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting queues info");
        }

        try {
            QueueInfo[] queueInfos = conn.getQueuesStatistics();

            if (queueInfos == null) {
                logger.warn("Unable to get queue statistics");
            } else {
                for (QueueInfo queueInfo : queueInfos) {
                    if (shouldMonitorDestination(queueInfo.getName(), includePatterns, showSystem, showTemp, TibcoEMSMetricFetcher.DestinationType.QUEUE, logger)) {
                        logger.info("Publishing metrics for queue " + queueInfo.getName());
                        List<com.appdynamics.extensions.metrics.Metric> queueInfoMetrics = getQueueInfo(queueInfo, metrics);
                        collectedMetrics.addAll(queueInfoMetrics);
                    }
                }
            }
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting queue metrics", e);
        } finally {
            logger.debug("QueueMetricCollector Phaser arrived");
            phaser.arriveAndDeregister();
        }
    }

    private List<com.appdynamics.extensions.metrics.Metric> getQueueInfo(QueueInfo queueInfo, Metrics metrics) {

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = new ArrayList<>();


        String thisPrefix = metrics.getMetricPrefix();

        String prefix;
        if (Strings.isNullOrEmpty(thisPrefix)) {
            prefix = "Queues|" + queueInfo.getName();
        } else {
            prefix = thisPrefix + "|" + queueInfo.getName();
        }

        Metric[] queueMetrics = metrics.getMetrics();

        for (Metric metric : queueMetrics) {

            String name = metric.getAttr();

            BigDecimal value = null;

            if ("ConsumerCount".equalsIgnoreCase(name)) {
                int consumerCount = queueInfo.getConsumerCount();
                value = BigDecimal.valueOf(consumerCount);
            } else if ("PendingMessageCount".equalsIgnoreCase(name)) {
                long pendingMessageCount = queueInfo.getPendingMessageCount();
                value = BigDecimal.valueOf(pendingMessageCount);
            } else if ("PendingMessageSize".equalsIgnoreCase(name)) {
                long pendingMessageSize = queueInfo.getPendingMessageSize();
                value = BigDecimal.valueOf(pendingMessageSize);
            } else if ("FlowControlMaxBytes".equalsIgnoreCase(name)) {
                long flowControlMaxBytes = queueInfo.getFlowControlMaxBytes();
                value = BigDecimal.valueOf(flowControlMaxBytes);
            } else if ("MaxMsgs".equalsIgnoreCase(name)) {
                long maxMsgs = queueInfo.getMaxMsgs();
                value = BigDecimal.valueOf(maxMsgs);
            } else if ("MaxBytes".equalsIgnoreCase(name)) {
                long maxBytes = queueInfo.getMaxBytes();
                value = BigDecimal.valueOf(maxBytes);
            } else if ("InboundByteRate".equalsIgnoreCase(name)) {
                long byteRate = queueInfo.getInboundStatistics().getByteRate();
                value = BigDecimal.valueOf(byteRate);
            } else if ("InboundMessageRate".equalsIgnoreCase(name)) {
                long messageRate = queueInfo.getInboundStatistics().getMessageRate();
                value = BigDecimal.valueOf(messageRate);
            } else if ("InboundByteCount".equalsIgnoreCase(name)) {
                long totalBytes = queueInfo.getInboundStatistics().getTotalBytes();
                value = BigDecimal.valueOf(totalBytes);
            } else if ("InboundMessageCount".equalsIgnoreCase(name)) {
                long totalMessages = queueInfo.getInboundStatistics().getTotalMessages();
                value = BigDecimal.valueOf(totalMessages);
            } else if ("OutboundByteRate".equalsIgnoreCase(name)) {
                long byteRate = queueInfo.getOutboundStatistics().getByteRate();
                value = BigDecimal.valueOf(byteRate);
            } else if ("OutboundMessageRate".equalsIgnoreCase(name)) {
                long messageRate = queueInfo.getOutboundStatistics().getMessageRate();
                value = BigDecimal.valueOf(messageRate);
            } else if ("OutboundByteCount".equalsIgnoreCase(name)) {
                long totalBytes = queueInfo.getOutboundStatistics().getTotalBytes();
                value = BigDecimal.valueOf(totalBytes);
            } else if ("OutboundMessageCount".equalsIgnoreCase(name)) {
                long totalMessages = queueInfo.getOutboundStatistics().getTotalMessages();
                value = BigDecimal.valueOf(totalMessages);
            } else if ("InTransitCount".equalsIgnoreCase(name)) {
                long inTransitMessageCount = queueInfo.getInTransitMessageCount();
                value = BigDecimal.valueOf(inTransitMessageCount);
            } else if ("ReceiverCount".equalsIgnoreCase(name)) {
                int receiverCount = queueInfo.getReceiverCount();
                value = BigDecimal.valueOf(receiverCount);
            } else if ("MaxRedelivery".equalsIgnoreCase(name)) {
                int maxRedelivery = queueInfo.getMaxRedelivery();
                value = BigDecimal.valueOf(maxRedelivery);
            } else if ("DeliveredMessageCount".equalsIgnoreCase(name)) {
                long deliveredMessageCount = queueInfo.getDeliveredMessageCount();
                value = BigDecimal.valueOf(deliveredMessageCount);
            }

            String alias = metric.getAlias();
            if (alias != null) {
                name = alias;
            }

            Map<String, String> propertiesMap = objectMapper.convertValue(metric, Map.class);

            StringBuilder sb = new StringBuilder(metricPrefix);
            sb.append("|");
            if (!Strings.isNullOrEmpty(prefix)) {
                sb.append(prefix).append("|");
            }
            sb.append(name);

            String fullMetricPath = sb.toString();

            com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric(name, value.toString(), fullMetricPath, propertiesMap);
            collectedMetrics.add(thisMetric);
        }
        return collectedMetrics;
    }
}