/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco;


import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.tibco.tibjms.TibjmsSSL;
import com.tibco.tibjms.admin.ConnectionInfo;
import com.tibco.tibjms.admin.ConsumerInfo;
import com.tibco.tibjms.admin.DestinationInfo;
import com.tibco.tibjms.admin.DurableInfo;
import com.tibco.tibjms.admin.ProducerInfo;
import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.RouteInfo;
import com.tibco.tibjms.admin.ServerInfo;
import com.tibco.tibjms.admin.StatData;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import com.tibco.tibjms.admin.TopicInfo;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam, Kevin Mcmanus
 */
public class TibcoEMSMetricFetcher implements Runnable {

    private static final Logger logger = Logger.getLogger(TibcoEMSMetricFetcher.class);

    private MonitorConfiguration configuration;
    private Map emsServer;
    protected volatile Map<String, String> valueMap;
    private Map<String, String> oldValuesMap;

    private enum DestinationType {
        QUEUE("Queue"), TOPIC("Topic"), PRODUCER("Producer"), CONSUMER("Consumer"), ROUTE("Route"), DURABLE("Durable"), CONNECTION("Connection");

        private String type;

        DestinationType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public TibcoEMSMetricFetcher(MonitorConfiguration configuration, Map emsServer, Map<String, Map<String, String>> cachedStats) {
        this.configuration = configuration;
        this.emsServer = emsServer;

        String displayName = (String) emsServer.get("displayName");
        oldValuesMap = cachedStats.get(displayName);

        if (oldValuesMap == null) {
            oldValuesMap = new HashMap<String, String>();
        }
        valueMap = new HashMap<String, String>();
    }

    public void run() {

        String displayName = (String) emsServer.get("displayName");
        String host = (String) emsServer.get("host");
        int port = (Integer) emsServer.get("port");
        String protocol = (String) emsServer.get("protocol");
        String user = (String) emsServer.get("user");
        String password = (String) emsServer.get("password");
        String encryptedPassword = (String) emsServer.get("encryptedPassword");
        String encryptionKey = (String) emsServer.get("encryptionKey");


        String emsURL = String.format("%s://%s:%s", protocol, host, port);
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

        TibjmsAdmin tibjmsAdmin = null;
        try {
            tibjmsAdmin = new TibjmsAdmin(emsURL, user, plainPassword, sslParams);
            collectMetrics(tibjmsAdmin, displayName);
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting metrics from Tibco EMS server [ " + displayName + " ]", e);
        } finally {
            if (tibjmsAdmin != null) {
                try {
                    tibjmsAdmin.close();
                } catch (TibjmsAdminException e) {
                    logger.error("Error while closing the connection", e);
                }
            }
        }
    }

    private List<Pattern> buildPattern(List<String> patternStrings) {
        List<Pattern> patternList = new ArrayList<Pattern>();

        if (patternStrings != null) {
            for (String pattern : patternStrings) {
                patternList.add(Pattern.compile(pattern));
            }
        }
        return patternList;
    }

    private void collectMetrics(TibjmsAdmin tibjmsAdmin, String displayName) throws TibjmsAdminException {

        boolean showTemp = (Boolean) emsServer.get("showTemp");
        boolean showSystem = (Boolean) emsServer.get("showSystem");
        List<String> includeQueues = (List) emsServer.get("includeQueues");
        List<String> excludeQueues = (List) emsServer.get("excludeQueues");

        List<String> includeTopics = (List) emsServer.get("includeTopics");
        List<String> excludeTopics = (List) emsServer.get("excludeTopics");

        List<String> includeDurables = (List) emsServer.get("includeDurables");
        List<String> excludeDurables = (List) emsServer.get("excludeDurables");

        List<String> includeRoutes = (List) emsServer.get("includeRoutes");
        List<String> excludeRoutes = (List) emsServer.get("excludeRoutes");


        List<Pattern> includeQueuesPatterns = buildPattern(includeQueues);
        List<Pattern> excludeQueuePatterns = buildPattern(excludeQueues);

        List<Pattern> includeTopicsPatterns = buildPattern(includeTopics);
        List<Pattern> excludeTopicPatterns = buildPattern(excludeTopics);

        List<Pattern> includeDurablesPatterns = buildPattern(includeDurables);
        List<Pattern> excludeDurablesPatterns = buildPattern(excludeDurables);

        List<Pattern> includeRoutesPatterns = buildPattern(includeRoutes);
        List<Pattern> excludeRoutesPatterns = buildPattern(excludeRoutes);


        try {

            ServerInfo serverInfo = tibjmsAdmin.getInfo();
            collectServerInfo(serverInfo);

            Boolean collectConnections = (Boolean) emsServer.get("collectConnections");
            if (collectConnections != null && collectConnections) {
                collectConnectionInfo(tibjmsAdmin.getConnections());
            }

            Boolean collectDurables = (Boolean) emsServer.get("collectDurables");
            if (collectDurables != null && collectDurables) {
                collectDurableInfo(tibjmsAdmin.getDurables(), includeDurablesPatterns, excludeDurablesPatterns, showSystem, showTemp);
            }

            Boolean collectRoutes = (Boolean) emsServer.get("collectRoutes");
            if (collectRoutes != null && collectRoutes) {
                collectRoutesInfo(tibjmsAdmin.getRoutes(), includeRoutesPatterns, excludeRoutesPatterns, showSystem, showTemp);
            }

            Boolean collectConsumers = (Boolean) emsServer.get("collectConsumers");
            if (collectConsumers != null && collectConsumers) {
                collectConsumerInfo(tibjmsAdmin.getConsumers(), includeQueuesPatterns, excludeQueuePatterns, includeTopicsPatterns, excludeTopicPatterns, showSystem, showTemp);
            }
            Boolean collectProducers = (Boolean) emsServer.get("collectProducers");
            if (collectProducers != null && collectProducers) {
                collectProducerInfo(tibjmsAdmin.getProducersStatistics(), includeQueuesPatterns, excludeQueuePatterns, includeTopicsPatterns, excludeTopicPatterns, showSystem, showTemp);
            }

            Boolean collectQueues = (Boolean) emsServer.get("collectQueues");
            if (collectQueues != null && collectQueues) {
                putQueueInfos(tibjmsAdmin, includeQueuesPatterns, excludeQueuePatterns, showSystem, showTemp);
            }
            Boolean collectTopics = (Boolean) emsServer.get("collectTopics");
            if (collectTopics != null && collectTopics) {
                putTopicInfos(tibjmsAdmin, includeTopicsPatterns, excludeTopicPatterns, showSystem, showTemp);
            }

            printMetrics(displayName);

        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting metrics", e);
            throw e;
        }
    }


    private void printMetrics(String displayName) {
        String metricPrefix = configuration.getMetricPrefix();
        for (Map.Entry<String, String> metric : valueMap.entrySet()) {
            String metricPath = Strings.isNullOrEmpty(displayName) ? metricPrefix + "|" + metric.getKey() : metricPrefix + "|" + displayName + "|" + metric.getKey();
            ;
            configuration.getMetricWriter().printMetric(metricPath, metric.getValue(), MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
        }
    }

    private void putQueueInfos(TibjmsAdmin conn, List<Pattern> includeQueuesPatterns, List<Pattern> excludeQueuePatterns, boolean showSystem, boolean showTemp) throws TibjmsAdminException {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting queues info");
        }

        QueueInfo[] queueInfos = conn.getQueuesStatistics();

        if (queueInfos == null) {
            logger.warn("Unable to get queue statistics");
        } else {
            for (QueueInfo queueInfo : queueInfos) {
                if (shouldMonitorDestination(queueInfo.getName(), includeQueuesPatterns, excludeQueuePatterns, showSystem, showTemp, DestinationType.QUEUE)) {
                    logger.info("Publishing metrics for queue " + queueInfo.getName());
                    putQueueInfo(queueInfo);
                }
            }
        }
    }

    private void putQueueInfo(QueueInfo queueInfo) {

        String prefix = "Queues|" + queueInfo.getName();

        putDestinationInfo(prefix, queueInfo);
        putDestinationValue(prefix, "InTransitCount", queueInfo.getInTransitMessageCount());
        putDestinationValue(prefix, "ReceiverCount", queueInfo.getReceiverCount());
        putDestinationValue(prefix, "MaxRedelivery", queueInfo.getMaxRedelivery());

        putDestinationValue(prefix, "DeliveredMessageCount", queueInfo.getDeliveredMessageCount());
    }

    private void putDestinationInfo(String prefix, DestinationInfo destInfo) {
        putDestinationValue(prefix, "ConsumerCount", destInfo.getConsumerCount());
        putDestinationValue(prefix, "PendingMessageCount", destInfo.getPendingMessageCount());
        putDestinationValue(prefix, "PendingMessageSize", destInfo.getPendingMessageSize());
        putDestinationValue(prefix, "FlowControlMaxBytes", destInfo.getFlowControlMaxBytes());
        putDestinationValue(prefix, "MaxMsgs", destInfo.getMaxMsgs());
        putDestinationValue(prefix, "MaxBytes", destInfo.getMaxBytes());

        // Inbound metrics
        StatData inboundData = destInfo.getInboundStatistics();
        putDestinationValue(prefix, "InboundByteRate", inboundData.getByteRate());
        putDestinationValue(prefix, "InboundMessageRate", inboundData.getMessageRate());
        putDestinationValue(prefix, "InboundByteCount", inboundData.getTotalBytes());
        putDestinationValue(prefix, "InboundMessageCount", inboundData.getTotalMessages());

        // Outbound metrics
        StatData outboundData = destInfo.getOutboundStatistics();
        putDestinationValue(prefix, "OutboundByteRate", outboundData.getByteRate());
        putDestinationValue(prefix, "OutboundMessageRate", outboundData.getMessageRate());
        putDestinationValue(prefix, "OutboundByteCount", outboundData.getTotalBytes());
        putDestinationValue(prefix, "OutboundMessageCount", outboundData.getTotalMessages());

        putDestinationValue(prefix, "InboundMessagesPerMinute", getDeltaValue(prefix + "|InboundMessageCount"));
        putDestinationValue(prefix, "OutboundMessagesPerMinute", getDeltaValue(prefix + "|OutboundMessageCount"));
        putDestinationValue(prefix, "InboundBytesPerMinute", getDeltaValue(prefix + "|InboundByteCount"));
        putDestinationValue(prefix, "OutboundBytesPerMinute", getDeltaValue(prefix + "|OutboundByteCount"));
    }

    private void putDestinationValue(String prefix, String key, long value) {


        valueMap.put(prefix + "|" + key, Long.toString(value));
    }

    private void putTopicInfos(TibjmsAdmin conn, List<Pattern> includeTopicsPatterns, List<Pattern> excludeTopicPatterns, boolean showSystem, boolean showTemp) throws TibjmsAdminException {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting topics info");
        }

        TopicInfo[] topicInfos = conn.getTopicsStatistics();

        if (topicInfos == null) {
            logger.warn("Unable to get topic statistics");
        } else {
            for (TopicInfo topicInfo : topicInfos) {
                if (shouldMonitorDestination(topicInfo.getName(), includeTopicsPatterns, excludeTopicPatterns, showSystem, showTemp, DestinationType.TOPIC)) {
                    logger.info("Publishing metrics for topic " + topicInfo.getName());
                    putTopicInfo(topicInfo);
                }
            }
        }
    }

    private void putTopicInfo(TopicInfo topicInfo) {
        String prefix = "Topics|" + topicInfo.getName();

        putDestinationInfo(prefix, topicInfo);
        putDestinationValue(prefix, "ConsumerCount", topicInfo.getConsumerCount());
        putDestinationValue(prefix, "SubscriberCount", topicInfo.getSubscriberCount());
        putDestinationValue(prefix, "ActiveDurableCount", topicInfo.getActiveDurableCount());
        putDestinationValue(prefix, "DurableCount", topicInfo.getDurableCount());
    }

    private boolean shouldMonitorDestination(String destName, List<Pattern> patternsToInclude, List<Pattern> patternsToExclude, boolean showSystem, boolean showTemp, DestinationType destinationType) {

        logger.debug("Checking includes and excludes for " + destinationType.getType() + " with name " + destName);

        try {
            if (destName.startsWith("$TMP$.") && !showTemp) {
                logger.debug("Skipping temporary " + destinationType.getType() + " '" + destName + "'");
                return false;
            }

            if (destName.startsWith("$sys.") && !showSystem) {
                logger.debug("Skipping system " + destinationType.getType() + " '" + destName + "'");
                return false;
            }

            if (patternsToInclude != null && patternsToInclude.size() > 0) {
                logger.debug("Using patterns to include [" + patternsToInclude + "] to filter");
                for (Pattern patternToInclude : patternsToInclude) {
                    Matcher matcher = patternToInclude.matcher(destName);
                    if (matcher.matches()) {
                        logger.debug(String.format("Including '%s' '%s' due to include pattern '%s'",
                                destinationType.getType(), destName, patternToInclude.pattern()));
                        return true;
                    }
                }
            } else if (patternsToExclude != null && patternsToExclude.size() > 0) {
                logger.debug("Using patterns to exclude [" + patternsToInclude + "] to filter");
                for (Pattern patternToExclude : patternsToExclude) {
                    Matcher matcher = patternToExclude.matcher(destName);
                    if (matcher.matches()) {
                        logger.debug(String.format("Skipping '%s' '%s' due to excluded pattern '%s'",
                                destinationType.getType(), destName, patternToExclude.pattern()));
                        return false;
                    }
                }

                logger.debug(String.format("Including '%s' '%s' due to not excluded by any exclude pattern",
                        destinationType.getType(), destName));
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.debug("Error in checking includes and excludes for  " + destinationType.getType() + " with name " + destName);
            return false;
        }
    }

    private void putServerValue(String key, Number value) {

        if (value == null) {
            logger.warn("Found null value for key [" + key + "]. Ignoring this metric");
        }

        valueMap.put(key, value.toString());
    }

    private void collectProducerInfo(ProducerInfo[] producers, List<Pattern> includeQueuePatterns, List<Pattern> excludeQueuePatterns, List<Pattern> includeTopicPatterns, List<Pattern> excludeTopicPatterns, boolean showSystem, boolean showTemp) {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting producers info");
        }

        if (producers == null || producers.length <= 0) {
            logger.info("No producers found to get the producers metrics");
        }

        for (ProducerInfo producerInfo : producers) {
            String prefix = "Producers|" + producerInfo.getID() + "|";
            int destinationType = producerInfo.getDestinationType();
            String destinationName = producerInfo.getDestinationName();

            if (destinationType == 2) {
                boolean monitor = shouldMonitorDestination(destinationName, includeQueuePatterns, excludeTopicPatterns, showSystem, showTemp, DestinationType.PRODUCER);
                if (!monitor) { //Skipping this destination as configured
                    continue;
                }
            } else {
                boolean monitor = shouldMonitorDestination(destinationName, includeTopicPatterns, excludeQueuePatterns, showSystem, showTemp, DestinationType.PRODUCER);
                if (!monitor) { //Skipping this destination as configured
                    continue;
                }
            }

            if (destinationType == 2) {
                prefix += "topic|";
            } else {
                prefix += "queue|";
            }
            prefix += destinationName;

            putDestinationValue(prefix, "ConnectionID", producerInfo.getConnectionID());
            putDestinationValue(prefix, "SessionID", producerInfo.getSessionID());
            StatData statistics = producerInfo.getStatistics();
            if (statistics != null) {
                putDestinationValue(prefix, "TotalMessages", statistics.getTotalMessages());
                putDestinationValue(prefix, "TotalBytes", statistics.getTotalBytes());
                putDestinationValue(prefix, "MessageRate", statistics.getMessageRate());
            }
        }
    }

    private void collectConsumerInfo(ConsumerInfo[] consumers, List<Pattern> includeQueuePatterns, List<Pattern> excludeQueuePatterns, List<Pattern> inclludeTopicPatterns, List<Pattern> excludeTopicPatterns, boolean showSystem, boolean showTemp) {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting consumers info");
        }

        if (consumers == null || consumers.length <= 0) {
            logger.info("No consumers found to get the consumers metrics");
        }

        for (ConsumerInfo consumerInfo : consumers) {

            int destinationType = consumerInfo.getDestinationType();
            String destinationName = consumerInfo.getDestinationName();

            if (destinationType == 2) {
                boolean monitor = shouldMonitorDestination(destinationName, includeQueuePatterns, excludeTopicPatterns, showSystem, showTemp, DestinationType.CONSUMER);
                if (!monitor) { //Skipping this destination as configured
                    continue;
                }
            } else {
                boolean monitor = shouldMonitorDestination(destinationName, inclludeTopicPatterns, excludeQueuePatterns, showSystem, showTemp, DestinationType.CONSUMER);
                if (!monitor) { //Skipping this destination as configured
                    continue;
                }
            }

            String prefix = "Consumers|" + consumerInfo.getID() + "|";

            if (destinationType == 2) {
                prefix += "topic|";
            } else {
                prefix += "queue|";
            }
            prefix += destinationName;

            putDestinationValue(prefix, "ConnectionID", consumerInfo.getConnectionID());
            putDestinationValue(prefix, "SessionID", consumerInfo.getSessionID());
            StatData statistics = consumerInfo.getStatistics();
            if (statistics != null) {
                putDestinationValue(prefix, "TotalMessages", statistics.getTotalMessages());
                putDestinationValue(prefix, "TotalBytes", statistics.getTotalBytes());
                putDestinationValue(prefix, "MessageRate", statistics.getMessageRate());
            }
        }

    }

    private void collectRoutesInfo(RouteInfo[] routes, List<Pattern> includeRoutesPatterns, List<Pattern> excludeRoutesPatterns, boolean showSystem, boolean showTemp) {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting routes info");
        }

        if (routes == null || routes.length <= 0) {
            logger.info("No routes found to get the routes metrics");
        }

        for (RouteInfo routeInfo : routes) {
            String routeName = routeInfo.getName();

            if (shouldMonitorDestination(routeName, includeRoutesPatterns, excludeRoutesPatterns, showSystem, showTemp, DestinationType.ROUTE)) {
                String prefix = "Routes|" + routeName;
                putDestinationValue(prefix, "InboundMessageRate", routeInfo.getInboundStatistics().getMessageRate());
                putDestinationValue(prefix, "InboundTotalMessages", routeInfo.getInboundStatistics().getTotalMessages());
                putDestinationValue(prefix, "OutboundMessageRate", routeInfo.getOutboundStatistics().getMessageRate());
                putDestinationValue(prefix, "OutboundTotalMessages", routeInfo.getOutboundStatistics().getTotalMessages());

                putDestinationValue(prefix, "BacklogCount", routeInfo.getBacklogCount());
                putDestinationValue(prefix, "BacklogSize", routeInfo.getBacklogSize());
                putDestinationValue(prefix, "InboundByteRate", routeInfo.getInboundStatistics().getByteRate());
                putDestinationValue(prefix, "IsConnected", routeInfo.isConnected() ? 1 : 0);
                putDestinationValue(prefix, "OutboundByteRate", routeInfo.getOutboundStatistics().getByteRate());
            }
        }
    }

    private void collectDurableInfo(DurableInfo[] durables, List<Pattern> includeDurablesPatterns, List<Pattern> excludeDurablesPatterns, boolean showSystem, boolean showTemp) {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting durables info");
        }

        if (durables == null || durables.length <= 0) {
            logger.info("No durables found to get the durables metrics");
        }

        for (DurableInfo durableInfo : durables) {
            String durableName = durableInfo.getDurableName();

            if (shouldMonitorDestination(durableName, includeDurablesPatterns, excludeDurablesPatterns, showSystem, showTemp, DestinationType.DURABLE)) {
                String prefix = "Durables|" + durableName;
                putDestinationValue(prefix, "PendingMessageCount", durableInfo.getPendingMessageCount());
                putDestinationValue(prefix, "PendingMessageSize", durableInfo.getPendingMessageSize());
            }
        }
    }

    private void collectConnectionInfo(ConnectionInfo[] connections) {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting connections info");
        }

        if (connections == null || connections.length <= 0) {
            logger.info("No Connections found to get the connection metrics");
        }

        for (ConnectionInfo connectionInfo : connections) {
            String prefix = "Connections|" + connectionInfo.getID() + "|" + connectionInfo.getHost() + "|" + connectionInfo.getType();
            putDestinationValue(prefix, "SessionCount", connectionInfo.getSessionCount());
            putDestinationValue(prefix, "ConsumerCount", connectionInfo.getConsumerCount());
            putDestinationValue(prefix, "ProducerCount", connectionInfo.getProducerCount());
            putDestinationValue(prefix, "StartTime", connectionInfo.getStartTime());
            putDestinationValue(prefix, "UpTime", connectionInfo.getUpTime());
        }

    }

    private void collectServerInfo(ServerInfo serverInfo) {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting server info");
        }

        putServerValue("DiskReadRate", serverInfo.getDiskReadRate());
        putServerValue("DiskWriteRate", serverInfo.getDiskWriteRate());


        putServerValue("State", serverInfo.getState());
        putServerValue("MsgMemory", serverInfo.getMsgMem());
        putServerValue("MaxMsgMemory", serverInfo.getMaxMsgMemory());
        putServerValue("MemoryPooled", serverInfo.getMsgMemPooled());

        putServerValue("SyncDBSize", serverInfo.getSyncDBSize());
        putServerValue("AsyncDBSize", serverInfo.getAsyncDBSize());
        putServerValue("QueueCount", serverInfo.getQueueCount());
        putServerValue("TopicCount", serverInfo.getTopicCount());
        putServerValue("DurableCount", serverInfo.getDurableCount());


        putServerValue("InboundBytesRate", serverInfo.getInboundBytesRate());
        putServerValue("InboundMessageRate", serverInfo.getInboundMessageRate());
        putServerValue("OutboundBytesRate", serverInfo.getOutboundBytesRate());
        putServerValue("OutboundMessageRate", serverInfo.getOutboundMessageRate());

        putServerValue("ConnectionCount", serverInfo.getConnectionCount());
        putServerValue("MaxConnections", serverInfo.getMaxConnections());

        putServerValue("ProducerCount", serverInfo.getProducerCount());
        putServerValue("ConsumerCount", serverInfo.getConsumerCount());
        putServerValue("SessionCount", serverInfo.getSessionCount());
        putServerValue("StartTime", serverInfo.getStartTime());
        putServerValue("UpTime", serverInfo.getUpTime());

        putServerValue("PendingMessageCount", serverInfo.getPendingMessageCount());
        putServerValue("PendingMessageSize", serverInfo.getPendingMessageSize());
        putServerValue("InboundMessageCount", serverInfo.getInboundMessageCount());
        putServerValue("OutboundMessageCount", serverInfo.getOutboundMessageCount());

        putServerValue("InboundMessagesPerMinute", getDeltaValue("InboundMessageCount"));
        putServerValue("OutboundMessagesPerMinute", getDeltaValue("OutboundMessageCount"));

        putServerValue("DurableCount", serverInfo.getDurableCount());
        putServerValue("LogFileSize", serverInfo.getLogFileSize());
        putServerValue("ServerHeartbeatClientInterval", serverInfo.getServerHeartbeatClientInterval());
        putServerValue("ServerTimeoutClientConnection", serverInfo.getServerTimeoutClientConnection());
        putServerValue("FaultTolerantActivation", serverInfo.getFaultTolerantActivation());
        putServerValue("FaultTolerantHeartbeat", serverInfo.getFaultTolerantHeartbeat());
        putServerValue("FaultTolerantReconnectTimeout", serverInfo.getFaultTolerantReconnectTimeout());
        putServerValue("LogFileMaxSize", serverInfo.getLogFileMaxSize());
        putServerValue("MaxStatisticsMemory", serverInfo.getMaxStatisticsMemory());
        putServerValue("ReserveMemory", serverInfo.getReserveMemory());
        putServerValue("RouteRecoverCount", serverInfo.getRouteRecoverCount());
        putServerValue("RouteRecoverInterval", serverInfo.getRouteRecoverInterval());
        putServerValue("ServerHeartbeatClientInterval", serverInfo.getServerHeartbeatClientInterval());
        putServerValue("ClientHeartbeatServerInterval", serverInfo.getClientHeartbeatServerInterval());
        putServerValue("ClientTimeoutServerConnection", serverInfo.getClientTimeoutServerConnection());
        putServerValue("ServerTimeoutServerConnection", serverInfo.getServerTimeoutServerConnection());
        putServerValue("StatisticsCleanupInterval", serverInfo.getStatisticsCleanupInterval());
        putServerValue("IsActiveServer", serverInfo.getState() == ServerInfo.SERVER_ACTIVE ? 1 : 0);
        putServerValue("IsFaultTolerantStandbyServer", serverInfo.getState() == ServerInfo.SERVER_FT_STANDBY ? 1 : 0);
    }

    protected long getDeltaValue(String key) {
        long delta = 0;

        String resultStr = valueMap.get(key);
        String oldResultStr = null;

        if (resultStr != null) {
            long result = Long.valueOf(resultStr);

            oldResultStr = oldValuesMap.get(key);
            if (oldResultStr != null) {
                long oldResult = Long.valueOf(oldResultStr);
                delta = result - oldResult;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("key='%s' old=%s new=%s diff=%d",
                    key, oldResultStr, resultStr, delta));
        }
        return delta;
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
            args.put(TaskInputArgs.PASSWORD_ENCRYPTED, encryptedPassword);
            args.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
            return CryptoUtil.getPassword(args);
        } catch (IllegalArgumentException e) {
            String msg = "Encryption Key not specified. Please set the value in config.yml.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }
}