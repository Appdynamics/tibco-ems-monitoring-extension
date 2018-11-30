package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.tibco.TibcoEMSMetricFetcher;
import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.base.Strings;
import com.tibco.tibjms.admin.RouteInfo;
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
public class RouteMetricCollector extends AbstractMetricCollector {
    private static final Logger logger = Logger.getLogger(RouteMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;


    public RouteMetricCollector(TibjmsAdmin conn, List<Pattern> includePatterns, List<Pattern> excludePatterns, boolean showSystem,
                                boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics) {
        super(conn, includePatterns, excludePatterns, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
    }

    public void run() {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting routes info");
        }

        try {
            RouteInfo[] routes = conn.getRoutes();

            if (routes == null) {
                logger.warn("Unable to get route metrics");
            } else {
                for (RouteInfo routeInfo : routes) {
                    if (shouldMonitorDestination(routeInfo.getName(), includePatterns, excludePatterns, showSystem, showTemp, TibcoEMSMetricFetcher.DestinationType.ROUTE, logger)) {
                        logger.info("Publishing metrics for route " + routeInfo.getName());
                        List<com.appdynamics.extensions.metrics.Metric> routeInfoMetrics = getRouteInfo(routeInfo, metrics);
                        collectedMetrics.addAll(routeInfoMetrics);
                    }
                }
            }
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting queue metrics", e);
        } finally {
            logger.debug("RouteMetricCollector Phaser arrived");
            phaser.arriveAndDeregister();
        }
    }

    private List<com.appdynamics.extensions.metrics.Metric> getRouteInfo(RouteInfo routeInfo, Metrics metrics) {


        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = new ArrayList<>();


        String thisPrefix = metrics.getMetricPrefix();

        String prefix;
        if (Strings.isNullOrEmpty(thisPrefix)) {
            prefix = "Routes|" + routeInfo.getName();
        } else {
            prefix = thisPrefix + "|" + routeInfo.getName();
        }

        Metric[] routeMetrics = metrics.getMetrics();

        StatData inboundStatistics = routeInfo.getInboundStatistics();
        StatData outboundStatistics = routeInfo.getOutboundStatistics();

        for (Metric metric : routeMetrics) {

            String name = metric.getAttr();

            BigDecimal value = null;

            if ("InboundMessageRate".equalsIgnoreCase(name) && inboundStatistics != null) {
                long messageRate = inboundStatistics.getMessageRate();
                value = BigDecimal.valueOf(messageRate);
            } else if ("InboundTotalMessages".equalsIgnoreCase(name) && inboundStatistics != null) {
                long totalMessages = inboundStatistics.getTotalMessages();
                value = BigDecimal.valueOf(totalMessages);
            } else if ("InboundByteRate".equalsIgnoreCase(name) && inboundStatistics != null) {
                long byteRate = inboundStatistics.getByteRate();
                value = BigDecimal.valueOf(byteRate);
            } else if ("OutboundMessageRate".equalsIgnoreCase(name) && outboundStatistics != null) {
                long messageRate = outboundStatistics.getMessageRate();
                value = BigDecimal.valueOf(messageRate);
            } else if ("OutboundTotalMessages".equalsIgnoreCase(name) && outboundStatistics != null) {
                long totalMessages = outboundStatistics.getTotalMessages();
                value = BigDecimal.valueOf(totalMessages);
            } else if ("OutboundByteRate".equalsIgnoreCase(name) && outboundStatistics != null) {
                long byteRate = outboundStatistics.getByteRate();
                value = BigDecimal.valueOf(byteRate);
            } else if ("BacklogCount".equalsIgnoreCase(name)) {
                long backlogCount = routeInfo.getBacklogCount();
                value = BigDecimal.valueOf(backlogCount);
            } else if ("BacklogSize".equalsIgnoreCase(name)) {
                long backlogSize = routeInfo.getBacklogSize();
                value = BigDecimal.valueOf(backlogSize);
            } else if ("IsConnected".equalsIgnoreCase(name)) {
                int connected = routeInfo.isConnected() ? 1 : 0;
                value = BigDecimal.valueOf(connected);
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