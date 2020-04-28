/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.base.Strings;
import com.tibco.tibjms.admin.ServerInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * @author Satish Muddam
 */
public class ServerMetricCollector extends AbstractMetricCollector {

    private static final org.slf4j.Logger logger = ExtensionsLoggerFactory.getLogger(ServerMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;

    public ServerMetricCollector(TibjmsAdmin conn, boolean showSystem,
                                 boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics) {
        super(conn, null, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
    }

    public void run() {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting server info");
        }

        String thisPrefix = metrics.getMetricPrefix();

        String prefix = "";
        if (!Strings.isNullOrEmpty(thisPrefix)) {
            prefix = thisPrefix + "|";
        }

        try {
            ServerInfo serverInfo = conn.getInfo();

            Metric[] serverMetrics = metrics.getMetrics();


            for (Metric metric : serverMetrics) {

                String name = metric.getAttr();
                BigDecimal value = null;

                if ("DiskReadRate".equalsIgnoreCase(name)) {
                    long diskReadRate = serverInfo.getDiskReadRate();
                    value = BigDecimal.valueOf(diskReadRate);
                } else if ("DiskWriteRate".equalsIgnoreCase(name)) {
                    long diskWriteRate = serverInfo.getDiskWriteRate();
                    value = BigDecimal.valueOf(diskWriteRate);
                } else if ("State".equalsIgnoreCase(name)) {
                    int state = serverInfo.getState();
                    value = BigDecimal.valueOf(state);
                } else if ("MsgMemory".equalsIgnoreCase(name)) {
                    long msgMem = serverInfo.getMsgMem();
                    value = BigDecimal.valueOf(msgMem);
                } else if ("MaxMsgMemory".equalsIgnoreCase(name)) {
                    long maxMsgMemory = serverInfo.getMaxMsgMemory();
                    value = BigDecimal.valueOf(maxMsgMemory);
                } else if ("MemoryPooled".equalsIgnoreCase(name)) {
                    long msgMemPooled = serverInfo.getMsgMemPooled();
                    value = BigDecimal.valueOf(msgMemPooled);
                } else if ("SyncDBSize".equalsIgnoreCase(name)) {
                    long syncDBSize = serverInfo.getSyncDBSize();
                    value = BigDecimal.valueOf(syncDBSize);
                } else if ("AsyncDBSize".equalsIgnoreCase(name)) {
                    long asyncDBSize = serverInfo.getAsyncDBSize();
                    value = BigDecimal.valueOf(asyncDBSize);

                } else if ("QueueCount".equalsIgnoreCase(name)) {
                    int queueCount = serverInfo.getQueueCount();
                    value = BigDecimal.valueOf(queueCount);

                } else if ("TopicCount".equalsIgnoreCase(name)) {
                    int topicCount = serverInfo.getTopicCount();
                    value = BigDecimal.valueOf(topicCount);

                } else if ("DurableCount".equalsIgnoreCase(name)) {
                    int durableCount = serverInfo.getDurableCount();
                    value = BigDecimal.valueOf(durableCount);

                } else if ("InboundBytesRate".equalsIgnoreCase(name)) {
                    long inboundBytesRate = serverInfo.getInboundBytesRate();
                    value = BigDecimal.valueOf(inboundBytesRate);

                } else if ("InboundMessageRate".equalsIgnoreCase(name)) {
                    long inboundMessageRate = serverInfo.getInboundMessageRate();
                    value = BigDecimal.valueOf(inboundMessageRate);

                } else if ("OutboundBytesRate".equalsIgnoreCase(name)) {
                    long outboundBytesRate = serverInfo.getOutboundBytesRate();
                    value = BigDecimal.valueOf(outboundBytesRate);

                } else if ("OutboundMessageRate".equalsIgnoreCase(name)) {
                    long outboundMessageRate = serverInfo.getOutboundMessageRate();
                    value = BigDecimal.valueOf(outboundMessageRate);

                } else if ("ConnectionCount".equalsIgnoreCase(name)) {
                    int connectionCount = serverInfo.getConnectionCount();
                    value = BigDecimal.valueOf(connectionCount);

                } else if ("MaxConnections".equalsIgnoreCase(name)) {
                    int maxConnections = serverInfo.getMaxConnections();
                    value = BigDecimal.valueOf(maxConnections);

                } else if ("ProducerCount".equalsIgnoreCase(name)) {
                    int producerCount = serverInfo.getProducerCount();
                    value = BigDecimal.valueOf(producerCount);

                } else if ("ConsumerCount".equalsIgnoreCase(name)) {
                    int consumerCount = serverInfo.getConsumerCount();
                    value = BigDecimal.valueOf(consumerCount);

                } else if ("SessionCount".equalsIgnoreCase(name)) {
                    int sessionCount = serverInfo.getSessionCount();
                    value = BigDecimal.valueOf(sessionCount);

                } else if ("StartTime".equalsIgnoreCase(name)) {
                    long startTime = serverInfo.getStartTime();
                    value = BigDecimal.valueOf(startTime);

                } else if ("UpTime".equalsIgnoreCase(name)) {
                    long upTime = serverInfo.getUpTime();
                    value = BigDecimal.valueOf(upTime);

                } else if ("PendingMessageCount".equalsIgnoreCase(name)) {
                    long pendingMessageCount = serverInfo.getPendingMessageCount();
                    value = BigDecimal.valueOf(pendingMessageCount);

                } else if ("PendingMessageSize".equalsIgnoreCase(name)) {
                    long pendingMessageSize = serverInfo.getPendingMessageSize();
                    value = BigDecimal.valueOf(pendingMessageSize);

                } else if ("InboundMessageCount".equalsIgnoreCase(name)) {
                    long inboundMessageCount = serverInfo.getInboundMessageCount();
                    value = BigDecimal.valueOf(inboundMessageCount);

                } else if ("OutboundMessageCount".equalsIgnoreCase(name)) {
                    long outboundMessageCount = serverInfo.getOutboundMessageCount();
                    value = BigDecimal.valueOf(outboundMessageCount);

                } else if ("DurableCount".equalsIgnoreCase(name)) {
                    int durableCount = serverInfo.getDurableCount();
                    value = BigDecimal.valueOf(durableCount);

                } else if ("LogFileSize".equalsIgnoreCase(name)) {
                    long logFileSize = serverInfo.getLogFileSize();
                    value = BigDecimal.valueOf(logFileSize);

                } else if ("ServerHeartbeatClientInterval".equalsIgnoreCase(name)) {
                    int serverHeartbeatClientInterval = serverInfo.getServerHeartbeatClientInterval();
                    value = BigDecimal.valueOf(serverHeartbeatClientInterval);

                } else if ("ServerTimeoutClientConnection".equalsIgnoreCase(name)) {
                    int serverTimeoutClientConnection = serverInfo.getServerTimeoutClientConnection();
                    value = BigDecimal.valueOf(serverTimeoutClientConnection);

                } else if ("FaultTolerantActivation".equalsIgnoreCase(name)) {
                    int faultTolerantActivation = serverInfo.getFaultTolerantActivation();
                    value = BigDecimal.valueOf(faultTolerantActivation);

                } else if ("FaultTolerantHeartbeat".equalsIgnoreCase(name)) {
                    int faultTolerantHeartbeat = serverInfo.getFaultTolerantHeartbeat();
                    value = BigDecimal.valueOf(faultTolerantHeartbeat);

                } else if ("FaultTolerantReconnectTimeout".equalsIgnoreCase(name)) {
                    int faultTolerantReconnectTimeout = serverInfo.getFaultTolerantReconnectTimeout();
                    value = BigDecimal.valueOf(faultTolerantReconnectTimeout);

                } else if ("LogFileMaxSize".equalsIgnoreCase(name)) {
                    long logFileMaxSize = serverInfo.getLogFileMaxSize();
                    value = BigDecimal.valueOf(logFileMaxSize);

                } else if ("MaxStatisticsMemory".equalsIgnoreCase(name)) {
                    long maxStatisticsMemory = serverInfo.getMaxStatisticsMemory();
                    value = BigDecimal.valueOf(maxStatisticsMemory);

                } else if ("ReserveMemory".equalsIgnoreCase(name)) {
                    long reserveMemory = serverInfo.getReserveMemory();
                    value = BigDecimal.valueOf(reserveMemory);

                } else if ("RouteRecoverCount".equalsIgnoreCase(name)) {
                    int routeRecoverCount = serverInfo.getRouteRecoverCount();
                    value = BigDecimal.valueOf(routeRecoverCount);

                } else if ("RouteRecoverInterval".equalsIgnoreCase(name)) {
                    long routeRecoverInterval = serverInfo.getRouteRecoverInterval();
                    value = BigDecimal.valueOf(routeRecoverInterval);

                } else if ("ServerHeartbeatServerInterval".equalsIgnoreCase(name)) {
                    int serverHeartbeatServerInterval = serverInfo.getServerHeartbeatServerInterval();
                    value = BigDecimal.valueOf(serverHeartbeatServerInterval);

                } else if ("ClientHeartbeatServerInterval".equalsIgnoreCase(name)) {
                    int clientHeartbeatServerInterval = serverInfo.getClientHeartbeatServerInterval();
                    value = BigDecimal.valueOf(clientHeartbeatServerInterval);

                } else if ("ClientTimeoutServerConnection".equalsIgnoreCase(name)) {
                    int clientTimeoutServerConnection = serverInfo.getClientTimeoutServerConnection();
                    value = BigDecimal.valueOf(clientTimeoutServerConnection);

                } else if ("ServerTimeoutServerConnection".equalsIgnoreCase(name)) {
                    int serverTimeoutServerConnection = serverInfo.getServerTimeoutServerConnection();
                    value = BigDecimal.valueOf(serverTimeoutServerConnection);

                } else if ("StatisticsCleanupInterval".equalsIgnoreCase(name)) {
                    long statisticsCleanupInterval = serverInfo.getStatisticsCleanupInterval();
                    value = BigDecimal.valueOf(statisticsCleanupInterval);

                } else if ("IsActiveServer".equalsIgnoreCase(name)) {
                    long isActiveServer = serverInfo.getState() == ServerInfo.SERVER_ACTIVE ? 1 : 0;
                    value = BigDecimal.valueOf(isActiveServer);

                } else if ("IsFaultTolerantStandbyServer".equalsIgnoreCase(name)) {
                    long isFaultTolerantStandbyServer = serverInfo.getState() == ServerInfo.SERVER_FT_STANDBY ? 1 : 0;
                    value = BigDecimal.valueOf(isFaultTolerantStandbyServer);
                } else {
                    logger.info("Invalid metric name [{}] configured in metrics.xml. Ignoring this metric", name);
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
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting metrics", e);
        } finally {
            logger.debug("ServerMetricCollector Phaser arrived");
            phaser.arriveAndDeregister();
        }
    }
}