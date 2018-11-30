package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.tibco.TibcoEMSMetricFetcher;
import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.base.Strings;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import com.tibco.tibjms.admin.TopicInfo;
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
public class TopicMetricCollector extends AbstractMetricCollector {
    private static final Logger logger = Logger.getLogger(TopicMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;


    public TopicMetricCollector(TibjmsAdmin conn, List<Pattern> includePatterns, List<Pattern> excludePatterns, boolean showSystem, boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics) {
        super(conn, includePatterns, excludePatterns, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
    }

    public void run() {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting topics info");
        }

        try {
            TopicInfo[] topicInfos = conn.getTopicsStatistics();

            if (topicInfos == null) {
                logger.warn("Unable to get topic statistics");
            } else {
                for (TopicInfo topicInfo : topicInfos) {
                    if (shouldMonitorDestination(topicInfo.getName(), includePatterns, excludePatterns, showSystem, showTemp, TibcoEMSMetricFetcher.DestinationType.TOPIC, logger)) {
                        logger.info("Publishing metrics for topic " + topicInfo.getName());
                        List<com.appdynamics.extensions.metrics.Metric> topicInfoMetrics = getTopicInfo(topicInfo, metrics);
                        collectedMetrics.addAll(topicInfoMetrics);
                    }
                }
            }
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting topic metrics", e);
        } finally {
            logger.debug("TopicMetricCollector Phaser arrived");
            phaser.arriveAndDeregister();
        }
    }

    private List<com.appdynamics.extensions.metrics.Metric> getTopicInfo(TopicInfo topicInfo, Metrics metrics) {

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = new ArrayList<>();


        String thisPrefix = metrics.getMetricPrefix();

        String prefix;
        if (Strings.isNullOrEmpty(thisPrefix)) {
            prefix = "Topics|" + topicInfo.getName();
        } else {
            prefix = thisPrefix + "|" + topicInfo.getName();
        }

        Metric[] queueMetrics = metrics.getMetrics();

        for (Metric metric : queueMetrics) {

            String name = metric.getAttr();
            BigDecimal value = null;

            if ("ConsumerCount".equalsIgnoreCase(name)) {
                int consumerCount = topicInfo.getConsumerCount();
                value = BigDecimal.valueOf(consumerCount);
            } else if ("PendingMessageCount".equalsIgnoreCase(name)) {
                long pendingMessageCount = topicInfo.getPendingMessageCount();
                value = BigDecimal.valueOf(pendingMessageCount);
            } else if ("PendingMessageSize".equalsIgnoreCase(name)) {
                long pendingMessageSize = topicInfo.getPendingMessageSize();
                value = BigDecimal.valueOf(pendingMessageSize);
            } else if ("FlowControlMaxBytes".equalsIgnoreCase(name)) {
                long flowControlMaxBytes = topicInfo.getFlowControlMaxBytes();
                value = BigDecimal.valueOf(flowControlMaxBytes);
            } else if ("MaxMsgs".equalsIgnoreCase(name)) {
                long maxMsgs = topicInfo.getMaxMsgs();
                value = BigDecimal.valueOf(maxMsgs);
            } else if ("MaxBytes".equalsIgnoreCase(name)) {
                long maxBytes = topicInfo.getMaxBytes();
                value = BigDecimal.valueOf(maxBytes);
            } else if ("InboundByteRate".equalsIgnoreCase(name)) {
                long byteRate = topicInfo.getInboundStatistics().getByteRate();
                value = BigDecimal.valueOf(byteRate);
            } else if ("InboundMessageRate".equalsIgnoreCase(name)) {
                long messageRate = topicInfo.getInboundStatistics().getMessageRate();
                value = BigDecimal.valueOf(messageRate);
            } else if ("InboundByteCount".equalsIgnoreCase(name)) {
                long totalBytes = topicInfo.getInboundStatistics().getTotalBytes();
                value = BigDecimal.valueOf(totalBytes);
            } else if ("InboundMessageCount".equalsIgnoreCase(name)) {
                long totalMessages = topicInfo.getInboundStatistics().getTotalMessages();
                value = BigDecimal.valueOf(totalMessages);
            } else if ("OutboundByteRate".equalsIgnoreCase(name)) {
                long byteRate = topicInfo.getOutboundStatistics().getByteRate();
                value = BigDecimal.valueOf(byteRate);
            } else if ("OutboundMessageRate".equalsIgnoreCase(name)) {
                long messageRate = topicInfo.getOutboundStatistics().getMessageRate();
                value = BigDecimal.valueOf(messageRate);
            } else if ("OutboundByteCount".equalsIgnoreCase(name)) {
                long totalBytes = topicInfo.getOutboundStatistics().getTotalBytes();
                value = BigDecimal.valueOf(totalBytes);
            } else if ("OutboundMessageCount".equalsIgnoreCase(name)) {
                long totalMessages = topicInfo.getOutboundStatistics().getTotalMessages();
                value = BigDecimal.valueOf(totalMessages);
            } else if ("SubscriberCount".equalsIgnoreCase(name)) {
                long inTransitMessageCount = topicInfo.getSubscriberCount();
                value = BigDecimal.valueOf(inTransitMessageCount);
            } else if ("ActiveDurableCount".equalsIgnoreCase(name)) {
                int receiverCount = topicInfo.getActiveDurableCount();
                value = BigDecimal.valueOf(receiverCount);
            } else if ("DurableCount".equalsIgnoreCase(name)) {
                int maxRedelivery = topicInfo.getDurableCount();
                value = BigDecimal.valueOf(maxRedelivery);
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