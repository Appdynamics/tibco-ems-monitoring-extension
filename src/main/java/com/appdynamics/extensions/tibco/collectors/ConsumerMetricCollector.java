package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.tibco.TibcoEMSMetricFetcher;
import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.base.Strings;
import com.tibco.tibjms.admin.ConsumerInfo;
import com.tibco.tibjms.admin.StatData;
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
public class ConsumerMetricCollector extends AbstractMetricCollector {
    private static final Logger logger = Logger.getLogger(ConsumerMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;


    public ConsumerMetricCollector(TibjmsAdmin conn, List<Pattern> includePatterns, List<Pattern> excludePatterns, boolean showSystem,
                                   boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics) {
        super(conn, includePatterns, excludePatterns, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
    }

    public void run() {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting consumer info");
        }
        try {
            ConsumerInfo[] consumers = conn.getConsumers();

            if (consumers == null || consumers.length <= 0) {
                logger.info("No consumers found to get the consumers metrics");
                return;
            }

            for (ConsumerInfo consumerInfo : consumers) {

                String thisPrefix = metrics.getMetricPrefix();


                String destinationName = consumerInfo.getDestinationName();

                if (shouldMonitorDestination(destinationName, includePatterns, excludePatterns, showSystem, showTemp, TibcoEMSMetricFetcher.DestinationType.CONSUMER, logger)) {
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
            prefix = "Consumers|" + consumerInfo.getID() + "|";
        } else {
            prefix = thisPrefix + "|" + consumerInfo.getID() + "|";
        }

        int destinationType = consumerInfo.getDestinationType();
        String destinationName = consumerInfo.getDestinationName();

        if (destinationType == 2) {
            prefix += "topic|";
        } else {
            prefix += "queue|";
        }
        prefix += destinationName;

        Metric[] consumerMetrics = metrics.getMetrics();

        StatData statistics = consumerInfo.getStatistics();

        for (Metric metric : consumerMetrics) {

            String name = metric.getAttr();
            BigDecimal value = null;

            if ("ConnectionID".equalsIgnoreCase(name)) {
                long connectionID = consumerInfo.getConnectionID();
                value = BigDecimal.valueOf(connectionID);

            } else if ("SessionID".equalsIgnoreCase(name)) {
                long sessionID = consumerInfo.getSessionID();
                value = BigDecimal.valueOf(sessionID);
            } else if ("TotalMessages".equalsIgnoreCase(name) && statistics != null) {
                long totalMessages = statistics.getTotalMessages();
                value = BigDecimal.valueOf(totalMessages);
            } else if ("TotalBytes".equalsIgnoreCase(name) && statistics != null) {
                long totalBytes = statistics.getTotalBytes();
                value = BigDecimal.valueOf(totalBytes);
            } else if ("MessageRate".equalsIgnoreCase(name) && statistics != null) {
                long messageRate = statistics.getMessageRate();
                value = BigDecimal.valueOf(messageRate);
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

            com.appdynamics.extensions.metrics.Metric thisMetrix = new com.appdynamics.extensions.metrics.Metric(name, value.toString(), fullMetricPath, propertiesMap);
            collectedMetrics.add(thisMetrix);
        }
        return collectedMetrics;
    }
}