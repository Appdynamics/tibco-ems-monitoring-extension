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
import com.google.common.collect.Maps;
import com.tibco.tibjms.admin.ConsumerInfo;
import com.tibco.tibjms.admin.StatData;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */

@RunWith(MockitoJUnitRunner.class)
public class ConsumerMetricCollectorTest {
    @Mock
    private TibjmsAdmin tibjmsAdmin;

    @Mock
    private ConsumerInfo consumerInfo1;
    @Mock
    private ConsumerInfo consumerInfo2;

    @Mock
    private StatData statData1;
    @Mock
    private StatData statData2;

    @Mock
    private Phaser phaser;

    private String includeAllPatternString = ".*";
    private Pattern includeAllPattern = Pattern.compile(includeAllPatternString);

    private String includeOnlyQueue1PatternString = "Queue1";
    private Pattern includeOnlyQueue1Pattern = Pattern.compile(includeOnlyQueue1PatternString);


    @Test
    public void collectTest() throws TibjmsAdminException {

        Metrics metrics = setupProducerMetrics();
        String metricPrefix = "Custom Metrics|EMS";
        List<Metric> collectedMetrics = Lists.newArrayList();
        Map<String, String> queueTopicMetricPrefixes = Maps.newHashMap();
        queueTopicMetricPrefixes.put("Queue", "Queues");
        queueTopicMetricPrefixes.put("Topic", "Topics");
        Boolean displayDynamicIdsInMetricPath = false;

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);

        when(tibjmsAdmin.getConsumersStatistics()).thenReturn(new ConsumerInfo[]{consumerInfo1, consumerInfo2});

        when(consumerInfo1.getID()).thenReturn(1l);
        when(consumerInfo2.getID()).thenReturn(2l);

        when(consumerInfo1.getDestinationName()).thenReturn("Queue1");
        when(consumerInfo2.getDestinationName()).thenReturn("Queue2");

        when(consumerInfo1.getDestinationType()).thenReturn(1);
        when(consumerInfo2.getDestinationType()).thenReturn(1);

        when(consumerInfo1.getStatistics()).thenReturn(statData1);
        when(consumerInfo2.getStatistics()).thenReturn(statData2);

        when(statData1.getMessageRate()).thenReturn(10l);
        when(statData1.getTotalMessages()).thenReturn(100l);
        when(statData1.getTotalBytes()).thenReturn(1000l);

        when(statData2.getMessageRate()).thenReturn(20l);
        when(statData2.getTotalMessages()).thenReturn(200l);
        when(statData2.getTotalBytes()).thenReturn(2000l);

        ConsumerMetricCollector consumerMetricCollector = new ConsumerMetricCollector(tibjmsAdmin, Lists.newArrayList(includeAllPattern), false, false, metrics, metricPrefix,
                phaser, collectedMetrics, queueTopicMetricPrefixes, displayDynamicIdsInMetricPath);
        consumerMetricCollector.run();

        Assert.assertEquals(6, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Queues|Queue1|Consumers|TotalMessages", "Custom Metrics|EMS|Queues|Queue1|Consumers|TotalBytes", "Custom Metrics|EMS|Queues|Queue1|Consumers|MessageRate",
                "Custom Metrics|EMS|Queues|Queue2|Consumers|TotalMessages", "Custom Metrics|EMS|Queues|Queue2|Consumers|TotalBytes", "Custom Metrics|EMS|Queues|Queue2|Consumers|MessageRate");
        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Queues|Queue1|Consumers|TotalMessages".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue1|Consumers|TotalBytes".equals(metricPath)) {
                Assert.assertEquals("1000", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue1|Consumers|MessageRate".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue2|Consumers|TotalMessages".equals(metricPath)) {
                Assert.assertEquals("200", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue2|Consumers|TotalBytes".equals(metricPath)) {
                Assert.assertEquals("2000", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue2|Consumers|MessageRate".equals(metricPath)) {
                Assert.assertEquals("20", metricValue);
            }
        }
    }

    @Test
    public void collectTestMatched() throws TibjmsAdminException {

        Metrics metrics = setupProducerMetrics();
        String metricPrefix = "Custom Metrics|EMS";
        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();
        Map<String, String> queueTopicMetricPrefixes = Maps.newHashMap();
        queueTopicMetricPrefixes.put("Queue", "Queues");
        queueTopicMetricPrefixes.put("Topic", "Topics");
        Boolean displayDynamicIdsInMetricPath = false;

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);

        when(tibjmsAdmin.getConsumersStatistics()).thenReturn(new ConsumerInfo[]{consumerInfo1, consumerInfo2});

        when(consumerInfo1.getID()).thenReturn(1l);
        when(consumerInfo2.getID()).thenReturn(2l);

        when(consumerInfo1.getDestinationName()).thenReturn("Queue1");
        when(consumerInfo2.getDestinationName()).thenReturn("Queue2");

        when(consumerInfo1.getDestinationType()).thenReturn(1);
        when(consumerInfo2.getDestinationType()).thenReturn(1);

        when(consumerInfo1.getStatistics()).thenReturn(statData1);
        when(consumerInfo2.getStatistics()).thenReturn(statData2);

        when(statData1.getMessageRate()).thenReturn(10l);
        when(statData1.getTotalMessages()).thenReturn(100l);
        when(statData1.getTotalBytes()).thenReturn(1000l);

        when(statData2.getMessageRate()).thenReturn(20l);
        when(statData2.getTotalMessages()).thenReturn(200l);
        when(statData2.getTotalBytes()).thenReturn(2000l);

        ConsumerMetricCollector consumerMetricCollector = new ConsumerMetricCollector(tibjmsAdmin, Lists.newArrayList(includeOnlyQueue1Pattern), false, false, metrics, metricPrefix,
                phaser, collectedMetrics, queueTopicMetricPrefixes, displayDynamicIdsInMetricPath);
        consumerMetricCollector.run();

        Assert.assertEquals(3, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Queues|Queue1|Consumers|TotalMessages", "Custom Metrics|EMS|Queues|Queue1|Consumers|TotalBytes", "Custom Metrics|EMS|Queues|Queue1|Consumers|MessageRate");
        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Queues|Queue1|Consumers|TotalMessages".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue1|Consumers|TotalBytes".equals(metricPath)) {
                Assert.assertEquals("1000", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue1|Consumers|MessageRate".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            }
        }
    }

    private Metrics setupProducerMetrics() {

        Metrics producerMetrics = new Metrics();
        producerMetrics.setEnabled("true");
        producerMetrics.setType("Consumer");
        producerMetrics.setMetricPrefix("Consumers");

        List<com.appdynamics.extensions.tibco.metrics.Metric> metricList = Lists.newArrayList();

        com.appdynamics.extensions.tibco.metrics.Metric metric1 = new com.appdynamics.extensions.tibco.metrics.Metric();
        metric1.setAttr("TotalMessages");
        metric1.setAggregationType("SUM");
        metric1.setTimeRollUpType("CURRENT");
        metric1.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric1);

        com.appdynamics.extensions.tibco.metrics.Metric metric2 = new com.appdynamics.extensions.tibco.metrics.Metric();
        metric2.setAttr("TotalBytes");
        metric2.setAggregationType("SUM");
        metric2.setTimeRollUpType("CURRENT");
        metric2.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric2);

        com.appdynamics.extensions.tibco.metrics.Metric metric3 = new com.appdynamics.extensions.tibco.metrics.Metric();
        metric3.setAttr("MessageRate");
        metric3.setAggregationType("SUM");
        metric3.setTimeRollUpType("AVERAGE");
        metric3.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric3);

        producerMetrics.setMetrics(metricList.toArray(new com.appdynamics.extensions.tibco.metrics.Metric[]{}));

        return producerMetrics;
    }
}
