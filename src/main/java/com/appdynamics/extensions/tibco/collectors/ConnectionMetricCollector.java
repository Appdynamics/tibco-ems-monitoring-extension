package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.base.Strings;
import com.tibco.tibjms.admin.ConnectionInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * @author Satish Muddam
 */
public class ConnectionMetricCollector extends AbstractMetricCollector {

    private static final Logger logger = Logger.getLogger(ConnectionMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;


    public ConnectionMetricCollector(TibjmsAdmin conn, boolean showSystem,
                                     boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics) {
        super(conn, null, null, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
    }

    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("Collecting connections info");
        }

        try {
            ConnectionInfo[] connections = conn.getConnections();

            if (connections == null) {
                logger.warn("Unable to get connection metrics");
            } else {
                for (ConnectionInfo connectionInfo : connections) {
                    logger.info("Publishing metrics for connection with id " + connectionInfo.getID());
                    List<com.appdynamics.extensions.metrics.Metric> connectionInfoMetrics = getConnectionInfo(connectionInfo, metrics);
                    collectedMetrics.addAll(connectionInfoMetrics);
                }
            }
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting connection metrics", e);
        } finally {
            logger.debug("ConnectionMetricCollector Phaser arrived");
            phaser.arriveAndDeregister();
        }
    }

    private List<com.appdynamics.extensions.metrics.Metric> getConnectionInfo(ConnectionInfo connectionInfo, Metrics metrics) {

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = new ArrayList<>();

        String thisPrefix = metrics.getMetricPrefix();
        String prefix;
        if (Strings.isNullOrEmpty(thisPrefix)) {
            prefix = "Connections|";
        } else {
            prefix = thisPrefix + "|";
        }

        prefix += connectionInfo.getID() + "|" + connectionInfo.getHost() + "|" + connectionInfo.getType();

        Metric[] connectionMetrics = metrics.getMetrics();

        for (Metric metric : connectionMetrics) {
            String name = metric.getAttr();


            BigDecimal value = null;

            if ("SessionCount".equalsIgnoreCase(name)) {
                int sessionCount = connectionInfo.getSessionCount();
                value = BigDecimal.valueOf(sessionCount);
            } else if ("ConsumerCount".equalsIgnoreCase(name)) {
                int consumerCount = connectionInfo.getConsumerCount();
                value = BigDecimal.valueOf(consumerCount);
            } else if ("ProducerCount".equalsIgnoreCase(name)) {
                int producerCount = connectionInfo.getProducerCount();
                value = BigDecimal.valueOf(producerCount);
            } else if ("StartTime".equalsIgnoreCase(name)) {
                long startTime = connectionInfo.getStartTime();
                value = BigDecimal.valueOf(startTime);
            } else if ("UpTime".equalsIgnoreCase(name)) {
                long upTime = connectionInfo.getUpTime();
                value = BigDecimal.valueOf(upTime);
            }

            String alias = metric.getAlias();
            if (alias != null) {
                name = alias;
            }

            StringBuilder sb = new StringBuilder(metricPrefix);
            sb.append("|");
            if (!Strings.isNullOrEmpty(prefix)) {
                sb.append(prefix).append("|");
            }
            sb.append(name);

            String fullMetricPath = sb.toString();

            Map<String, String> propertiesMap = objectMapper.convertValue(metric, Map.class);


            com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric(name, value.toString(), fullMetricPath, propertiesMap);
            collectedMetrics.add(thisMetric);
        }
        return collectedMetrics;
    }
}