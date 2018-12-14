/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco.collectors;

import static org.mockito.Mockito.when;

import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.collect.Lists;
import com.tibco.tibjms.admin.ServerInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.Phaser;

/**
 * @author Satish Muddam
 */

@RunWith(MockitoJUnitRunner.class)
public class ServerMetricCollectorTest {

    @Mock
    private TibjmsAdmin tibjmsAdmin;

    @Mock
    private Phaser phaser;

    @Mock
    private ServerInfo serverInfo;

    @Test
    public void testCollect() throws TibjmsAdminException {

        Metrics metrics = setupServerMetrics();
        String metricPrefix = "Custom Metrics|EMS";

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);

        when(tibjmsAdmin.getInfo()).thenReturn(serverInfo);

        when(serverInfo.getConnectionCount()).thenReturn(10);
        when(serverInfo.getConsumerCount()).thenReturn(5);
        when(serverInfo.getInboundMessageCount()).thenReturn(50l);
        when(serverInfo.getOutboundMessageCount()).thenReturn(100l);
        when(serverInfo.getQueueCount()).thenReturn(15);
        when(serverInfo.getTopicCount()).thenReturn(25);

        ServerMetricCollector serverMetricCollector = new ServerMetricCollector(tibjmsAdmin, false, false, metrics, metricPrefix,
                phaser, collectedMetrics);
        serverMetricCollector.run();

        Assert.assertEquals(6, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|ConnectionCount", "Custom Metrics|EMS|ConsumerCount", "Custom Metrics|EMS|InboundMessageCount", "Custom Metrics|EMS|OutboundMessageCount",
                "Custom Metrics|EMS|QueueCount", "Custom Metrics|EMS|TopicCount");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|ConnectionCount".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            } else if ("Custom Metrics|EMS|ConsumerCount".equals(metricPath)) {
                Assert.assertEquals("5", metricValue);
            } else if ("Custom Metrics|EMS|InboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("50", metricValue);
            } else if ("Custom Metrics|EMS|OutboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|QueueCount".equals(metricPath)) {
                Assert.assertEquals("15", metricValue);
            } else if ("Custom Metrics|EMS|TopicCount".equals(metricPath)) {
                Assert.assertEquals("25", metricValue);
            }
        }
    }

    private Metrics setupServerMetrics() {

        Metrics serverMetrics = new Metrics();
        serverMetrics.setEnabled("true");
        serverMetrics.setType("Server");

        List<Metric> metricList = Lists.newArrayList();

        Metric metric1 = new Metric();
        metric1.setAttr("ConnectionCount");
        metric1.setAggregationType("AVERAGE");
        metric1.setTimeRollUpType("AVERAGE");
        metric1.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric1);

        Metric metric2 = new Metric();
        metric2.setAttr("ConsumerCount");
        metric2.setAggregationType("AVERAGE");
        metric2.setTimeRollUpType("AVERAGE");
        metric2.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric2);

        Metric metric3 = new Metric();
        metric3.setAttr("InboundMessageCount");
        metric3.setAggregationType("AVERAGE");
        metric3.setTimeRollUpType("AVERAGE");
        metric3.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric3);

        Metric metric4 = new Metric();
        metric4.setAttr("OutboundMessageCount");
        metric4.setAggregationType("AVERAGE");
        metric4.setTimeRollUpType("AVERAGE");
        metric4.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric4);

        Metric metric5 = new Metric();
        metric5.setAttr("QueueCount");
        metric5.setAggregationType("AVERAGE");
        metric5.setTimeRollUpType("AVERAGE");
        metric5.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric5);

        Metric metric6 = new Metric();
        metric6.setAttr("TopicCount");
        metric6.setAggregationType("AVERAGE");
        metric6.setTimeRollUpType("AVERAGE");
        metric6.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric6);

        serverMetrics.setMetrics(metricList.toArray(new Metric[]{}));

        return serverMetrics;
    }
}