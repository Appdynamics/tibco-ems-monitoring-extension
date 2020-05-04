/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco.collectors;

import static org.mockito.Mockito.when;

import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.collect.Lists;
import com.tibco.tibjms.admin.StatData;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import com.tibco.tibjms.admin.TopicInfo;
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
public class TopicMetricCollectorTest {

    @Mock
    private TibjmsAdmin tibjmsAdmin;

    @Mock
    private TopicInfo topicInfo1;
    @Mock
    private TopicInfo topicInfo2;

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

    private String includeOnlyTopic2PatternString = "Topic2";
    private Pattern includeOnlyTopic2Pattern = Pattern.compile(includeOnlyTopic2PatternString);

    @Test
    public void testCollectAll() throws TibjmsAdminException {

        Metrics metrics = setupQueueMetrics();

        List<Metric> collectedMetrics = Lists.newArrayList();

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);


        when(tibjmsAdmin.getTopicsStatistics()).thenReturn(new TopicInfo[]{topicInfo1, topicInfo2});

        when(topicInfo1.getName()).thenReturn("Topic1");
        when(topicInfo2.getName()).thenReturn("Topic2");

        when(topicInfo1.getActiveDurableCount()).thenReturn(10);
        when(topicInfo2.getActiveDurableCount()).thenReturn(20);

        when(topicInfo1.getInboundStatistics()).thenReturn(inboundStatData1);
        when(topicInfo2.getInboundStatistics()).thenReturn(inboundStatData2);

        when(inboundStatData1.getTotalMessages()).thenReturn(5l);
        when(inboundStatData2.getTotalMessages()).thenReturn(20l);

        when(inboundStatData1.getMessageRate()).thenReturn(100l);
        when(inboundStatData2.getMessageRate()).thenReturn(200l);


        when(topicInfo1.getOutboundStatistics()).thenReturn(outboundStatData1);
        when(topicInfo2.getOutboundStatistics()).thenReturn(outboundStatData2);

        when(outboundStatData1.getTotalMessages()).thenReturn(15l);
        when(outboundStatData2.getTotalMessages()).thenReturn(25l);

        when(outboundStatData1.getMessageRate()).thenReturn(300l);
        when(outboundStatData2.getMessageRate()).thenReturn(500l);


        String metricPrefix = "Custom Metrics|EMS";
        TopicMetricCollector topicMetricCollector = new TopicMetricCollector(tibjmsAdmin, Lists.newArrayList(includeAllPattern), false, false,
                metrics, metricPrefix, phaser, collectedMetrics);
        topicMetricCollector.run();

        Assert.assertEquals(10, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Topic|Topic1|ActiveDurableCount", "Custom Metrics|EMS|Topic|Topic1|InboundMessageCount", "Custom Metrics|EMS|Topic|Topic1|InboundMessageRate",
                "Custom Metrics|EMS|Topic|Topic1|OutboundMessageCount", "Custom Metrics|EMS|Topic|Topic1|OutboundMessageRate", "Custom Metrics|EMS|Topic|Topic2|ActiveDurableCount",
                "Custom Metrics|EMS|Topic|Topic2|InboundMessageCount", "Custom Metrics|EMS|Topic|Topic2|InboundMessageRate", "Custom Metrics|EMS|Topic|Topic2|OutboundMessageCount", "Custom Metrics|EMS|Topic|Topic2|OutboundMessageRate");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Topic|Topic1|ActiveDurableCount".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic1|InboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("5", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic1|InboundByteRate".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic1|OutboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("15", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic1|OutboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("300", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|ActiveDurableCount".equals(metricPath)) {
                Assert.assertEquals("20", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|InboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("20", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|InboundByteRate".equals(metricPath)) {
                Assert.assertEquals("200", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|OutboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("25", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|OutboundMessageRate".equals(metricPath)) {
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


        when(tibjmsAdmin.getTopicsStatistics()).thenReturn(new TopicInfo[]{topicInfo1, topicInfo2});

        when(topicInfo1.getName()).thenReturn("Topic1");
        when(topicInfo2.getName()).thenReturn("Topic2");

        when(topicInfo1.getActiveDurableCount()).thenReturn(10);
        when(topicInfo2.getActiveDurableCount()).thenReturn(20);

        when(topicInfo1.getInboundStatistics()).thenReturn(inboundStatData1);
        when(topicInfo2.getInboundStatistics()).thenReturn(inboundStatData2);

        when(inboundStatData1.getTotalMessages()).thenReturn(5l);
        when(inboundStatData2.getTotalMessages()).thenReturn(20l);

        when(inboundStatData1.getMessageRate()).thenReturn(100l);
        when(inboundStatData2.getMessageRate()).thenReturn(200l);


        when(topicInfo1.getOutboundStatistics()).thenReturn(outboundStatData1);
        when(topicInfo2.getOutboundStatistics()).thenReturn(outboundStatData2);

        when(outboundStatData1.getTotalMessages()).thenReturn(15l);
        when(outboundStatData2.getTotalMessages()).thenReturn(25l);

        when(outboundStatData1.getMessageRate()).thenReturn(300l);
        when(outboundStatData2.getMessageRate()).thenReturn(500l);


        String metricPrefix = "Custom Metrics|EMS";
        TopicMetricCollector topicMetricCollector = new TopicMetricCollector(tibjmsAdmin, Lists.newArrayList(includeOnlyTopic2Pattern), false, false,
                metrics, metricPrefix, phaser, collectedMetrics);
        topicMetricCollector.run();

        Assert.assertEquals(5, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Topic|Topic2|ActiveDurableCount", "Custom Metrics|EMS|Topic|Topic2|InboundMessageCount", "Custom Metrics|EMS|Topic|Topic2|InboundMessageRate",
                "Custom Metrics|EMS|Topic|Topic2|OutboundMessageCount", "Custom Metrics|EMS|Topic|Topic2|OutboundMessageRate");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Topic|Topic2|ActiveDurableCount".equals(metricPath)) {
                Assert.assertEquals("20", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|InboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("20", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|InboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("200", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|OutboundMessageCount".equals(metricPath)) {
                Assert.assertEquals("25", metricValue);
            } else if ("Custom Metrics|EMS|Topic|Topic2|OutboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("500", metricValue);
            }
        }
    }

    private Metrics setupQueueMetrics() {

        Metrics topicMetrics = new Metrics();
        topicMetrics.setEnabled("true");
        topicMetrics.setType("Topic");
        topicMetrics.setMetricPrefix("Topic");

        List<com.appdynamics.extensions.tibco.metrics.Metric> metricList = Lists.newArrayList();

        com.appdynamics.extensions.tibco.metrics.Metric metric1 = new com.appdynamics.extensions.tibco.metrics.Metric();
        metric1.setAttr("ActiveDurableCount");
        metric1.setAggregationType("AVERAGE");
        metric1.setTimeRollUpType("AVERAGE");
        metric1.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric1);

        com.appdynamics.extensions.tibco.metrics.Metric metric2 = new com.appdynamics.extensions.tibco.metrics.Metric();
        metric2.setAttr("InboundMessageCount");
        metric2.setAggregationType("AVERAGE");
        metric2.setTimeRollUpType("AVERAGE");
        metric2.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric2);

        com.appdynamics.extensions.tibco.metrics.Metric metric3 = new com.appdynamics.extensions.tibco.metrics.Metric();
        metric3.setAttr("InboundMessageRate");
        metric3.setAggregationType("AVERAGE");
        metric3.setTimeRollUpType("AVERAGE");
        metric3.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric3);

        com.appdynamics.extensions.tibco.metrics.Metric metric4 = new com.appdynamics.extensions.tibco.metrics.Metric();
        metric4.setAttr("OutboundMessageCount");
        metric4.setAggregationType("AVERAGE");
        metric4.setTimeRollUpType("AVERAGE");
        metric4.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric4);

        com.appdynamics.extensions.tibco.metrics.Metric metric5 = new com.appdynamics.extensions.tibco.metrics.Metric();
        metric5.setAttr("OutboundMessageRate");
        metric5.setAggregationType("AVERAGE");
        metric5.setTimeRollUpType("AVERAGE");
        metric5.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric5);

        topicMetrics.setMetrics(metricList.toArray(new com.appdynamics.extensions.tibco.metrics.Metric[]{}));


        return topicMetrics;
    }

}
