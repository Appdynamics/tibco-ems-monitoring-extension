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
import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.StatData;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */

@RunWith(MockitoJUnitRunner.class)
public class QueueMetricCollectorTest {

    @Mock
    private TibjmsAdmin tibjmsAdmin;

    @Mock
    private QueueInfo queueInfo1;
    @Mock
    private QueueInfo queueInfo2;

    @Mock
    private StatData inboundStatData1;
    @Mock
    private StatData inboundStatData2;

    @Mock
    private StatData outboundStatData1;
    @Mock
    private StatData outboundStatData2;

    @Mock
    private Phaser phaser;

    private String includeAllPatternString = ".*";
    private Pattern includeAllPattern = Pattern.compile(includeAllPatternString);

    private String includeOnlyQueue1PatternString = "Queue1";
    private Pattern includeOnlyQueue1Pattern = Pattern.compile(includeOnlyQueue1PatternString);

    @Test
    public void testCollectAll() throws TibjmsAdminException {

        Metrics metrics = setupQueueMetrics();

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);


        when(tibjmsAdmin.getQueuesStatistics()).thenReturn(new QueueInfo[]{queueInfo1, queueInfo2});

        when(queueInfo1.getName()).thenReturn("Queue1");
        when(queueInfo2.getName()).thenReturn("Queue2");

        when(queueInfo1.getDeliveredMessageCount()).thenReturn(10l);
        when(queueInfo2.getDeliveredMessageCount()).thenReturn(20l);

        when(queueInfo1.getInboundStatistics()).thenReturn(inboundStatData1);
        when(queueInfo2.getInboundStatistics()).thenReturn(inboundStatData2);

        when(inboundStatData1.getTotalMessages()).thenReturn(5l);
        when(inboundStatData2.getTotalMessages()).thenReturn(20l);

        when(inboundStatData1.getByteRate()).thenReturn(100l);
        when(inboundStatData2.getByteRate()).thenReturn(200l);


        when(queueInfo1.getOutboundStatistics()).thenReturn(outboundStatData1);
        when(queueInfo2.getOutboundStatistics()).thenReturn(outboundStatData2);

        when(outboundStatData1.getTotalMessages()).thenReturn(15l);
        when(outboundStatData2.getTotalMessages()).thenReturn(25l);

        when(outboundStatData1.getMessageRate()).thenReturn(300l);
        when(outboundStatData2.getMessageRate()).thenReturn(500l);


        String metricPrefix = "Custom Metrics|EMS";
        QueueMetricCollector queueMetricCollector = new QueueMetricCollector(tibjmsAdmin, Lists.newArrayList(includeAllPattern), false, false,
                metrics, metricPrefix, phaser, collectedMetrics);
        queueMetricCollector.run();

        Assert.assertEquals(10, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Queue|Queue1|DeliveredMessageCount", "Custom Metrics|EMS|Queue|Queue1|InboundMessageCount", "Custom Metrics|EMS|Queue|Queue1|InboundByteRate",
                "Custom Metrics|EMS|Queue|Queue1|OutboundMessageCount", "Custom Metrics|EMS|Queue|Queue1|OutboundMessageRate", "Custom Metrics|EMS|Queue|Queue2|DeliveredMessageCount",
                "Custom Metrics|EMS|Queue|Queue2|InboundMessageCount", "Custom Metrics|EMS|Queue|Queue2|InboundByteRate", "Custom Metrics|EMS|Queue|Queue2|OutboundMessageCount", "Custom Metrics|EMS|Queue|Queue2|OutboundMessageRate");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Queue|Queue1|DeliveredMessageCount".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue1|InboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("5", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue1|InboundByteRate".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue1|OutboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("15", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue1|OutboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("300", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue2|DeliveredMessageCount".equals(metricPath)) {
                Assert.assertEquals("20", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue2|InboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("20", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue2|InboundByteRate".equals(metricPath)) {
                Assert.assertEquals("200", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue2|OutboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("25", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue2|OutboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("500", metricValue);
            }
        }
    }

    @Test
    public void testCollectMatched() throws TibjmsAdminException {

        Metrics metrics = setupQueueMetrics();

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);


        when(tibjmsAdmin.getQueuesStatistics()).thenReturn(new QueueInfo[]{queueInfo1, queueInfo2});

        when(queueInfo1.getName()).thenReturn("Queue1");
        when(queueInfo2.getName()).thenReturn("Queue2");

        when(queueInfo1.getDeliveredMessageCount()).thenReturn(10l);
        when(queueInfo2.getDeliveredMessageCount()).thenReturn(20l);

        when(queueInfo1.getInboundStatistics()).thenReturn(inboundStatData1);
        when(queueInfo2.getInboundStatistics()).thenReturn(inboundStatData2);

        when(inboundStatData1.getTotalMessages()).thenReturn(5l);
        when(inboundStatData2.getTotalMessages()).thenReturn(20l);

        when(inboundStatData1.getByteRate()).thenReturn(100l);
        when(inboundStatData2.getByteRate()).thenReturn(200l);


        when(queueInfo1.getOutboundStatistics()).thenReturn(outboundStatData1);
        when(queueInfo2.getOutboundStatistics()).thenReturn(outboundStatData2);

        when(outboundStatData1.getTotalMessages()).thenReturn(15l);
        when(outboundStatData2.getTotalMessages()).thenReturn(25l);

        when(outboundStatData1.getMessageRate()).thenReturn(300l);
        when(outboundStatData2.getMessageRate()).thenReturn(500l);


        String metricPrefix = "Custom Metrics|EMS";
        QueueMetricCollector queueMetricCollector = new QueueMetricCollector(tibjmsAdmin, Lists.newArrayList(includeOnlyQueue1Pattern), false, false,
                metrics, metricPrefix, phaser, collectedMetrics);
        queueMetricCollector.run();

        Assert.assertEquals(5, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Queue|Queue1|DeliveredMessageCount", "Custom Metrics|EMS|Queue|Queue1|InboundMessageCount", "Custom Metrics|EMS|Queue|Queue1|InboundByteRate",
                "Custom Metrics|EMS|Queue|Queue1|OutboundMessageCount", "Custom Metrics|EMS|Queue|Queue1|OutboundMessageRate");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Queue|Queue1|DeliveredMessageCount".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue1|InboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("5", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue1|InboundByteRate".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue1|OutboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("15", metricValue);
            } else if ("Custom Metrics|EMS|Queue|Queue1|OutboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("300", metricValue);
            }
        }
    }

    private Metrics setupQueueMetrics() {

        Metrics queueMetrics = new Metrics();
        queueMetrics.setEnabled("true");
        queueMetrics.setType("Queue");
        queueMetrics.setMetricPrefix("Queue");

        List<Metric> metricList = Lists.newArrayList();

        Metric metric1 = new Metric();
        metric1.setAttr("DeliveredMessageCount");
        metric1.setAggregationType("AVERAGE");
        metric1.setTimeRollUpType("AVERAGE");
        metric1.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric1);

        Metric metric2 = new Metric();
        metric2.setAttr("InboundMessageCount");
        metric2.setAggregationType("AVERAGE");
        metric2.setTimeRollUpType("AVERAGE");
        metric2.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric2);

        Metric metric3 = new Metric();
        metric3.setAttr("InboundByteRate");
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
        metric5.setAttr("OutboundMessageRate");
        metric5.setAggregationType("AVERAGE");
        metric5.setTimeRollUpType("AVERAGE");
        metric5.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric5);

        queueMetrics.setMetrics(metricList.toArray(new Metric[]{}));


        return queueMetrics;
    }


}
