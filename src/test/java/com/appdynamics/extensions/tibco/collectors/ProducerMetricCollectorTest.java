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
import com.google.common.collect.Maps;
import com.tibco.tibjms.admin.ProducerInfo;
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
public class ProducerMetricCollectorTest {


    @Mock
    private TibjmsAdmin tibjmsAdmin;

    @Mock
    private ProducerInfo producerInfo1;
    @Mock
    private ProducerInfo producerInfo2;

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
        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();
        Map<String, String> queueTopicMetricPrefixes = Maps.newHashMap();
        queueTopicMetricPrefixes.put("Queue", "Queues");
        queueTopicMetricPrefixes.put("Topic", "Topics");
        Boolean displayDynamicIdsInMetricPath = false;

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);

        when(tibjmsAdmin.getProducersStatistics()).thenReturn(new ProducerInfo[]{producerInfo1, producerInfo2});

        when(producerInfo1.getID()).thenReturn(1l);
        when(producerInfo2.getID()).thenReturn(2l);

        when(producerInfo1.getDestinationName()).thenReturn("Queue1");
        when(producerInfo2.getDestinationName()).thenReturn("Queue2");

        when(producerInfo1.getDestinationType()).thenReturn(1);
        when(producerInfo2.getDestinationType()).thenReturn(1);

        when(producerInfo1.getStatistics()).thenReturn(statData1);
        when(producerInfo2.getStatistics()).thenReturn(statData2);

        when(statData1.getMessageRate()).thenReturn(10l);
        when(statData1.getTotalMessages()).thenReturn(100l);
        when(statData1.getTotalBytes()).thenReturn(1000l);

        when(statData2.getMessageRate()).thenReturn(20l);
        when(statData2.getTotalMessages()).thenReturn(200l);
        when(statData2.getTotalBytes()).thenReturn(2000l);

        ProducerMetricCollector producerMetricCollector = new ProducerMetricCollector(tibjmsAdmin, Lists.newArrayList(includeAllPattern), false, false, metrics, metricPrefix,
                phaser, collectedMetrics, queueTopicMetricPrefixes, displayDynamicIdsInMetricPath);
        producerMetricCollector.run();

        Assert.assertEquals(6, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Queues|Queue1|Producers|TotalMessages", "Custom Metrics|EMS|Queues|Queue1|Producers|TotalBytes", "Custom Metrics|EMS|Queues|Queue1|Producers|MessageRate",
                "Custom Metrics|EMS|Queues|Queue2|Producers|TotalMessages", "Custom Metrics|EMS|Queues|Queue2|Producers|TotalBytes", "Custom Metrics|EMS|Queues|Queue2|Producers|MessageRate");
        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Queues|Queue1|Producers|TotalMessages".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue1|Producers|TotalBytes".equals(metricPath)) {
                Assert.assertEquals("1000", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue1|Producers|MessageRate".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue2|Producers|TotalMessages".equals(metricPath)) {
                Assert.assertEquals("200", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue2|Producers|TotalBytes".equals(metricPath)) {
                Assert.assertEquals("2000", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue2|Producers|MessageRate".equals(metricPath)) {
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

        when(tibjmsAdmin.getProducersStatistics()).thenReturn(new ProducerInfo[]{producerInfo1, producerInfo2});

        when(producerInfo1.getID()).thenReturn(1l);
        when(producerInfo2.getID()).thenReturn(2l);

        when(producerInfo1.getDestinationName()).thenReturn("Queue1");
        when(producerInfo2.getDestinationName()).thenReturn("Queue2");

        when(producerInfo1.getDestinationType()).thenReturn(1);
        when(producerInfo2.getDestinationType()).thenReturn(1);

        when(producerInfo1.getStatistics()).thenReturn(statData1);
        when(producerInfo2.getStatistics()).thenReturn(statData2);

        when(statData1.getMessageRate()).thenReturn(10l);
        when(statData1.getTotalMessages()).thenReturn(100l);
        when(statData1.getTotalBytes()).thenReturn(1000l);

        when(statData2.getMessageRate()).thenReturn(20l);
        when(statData2.getTotalMessages()).thenReturn(200l);
        when(statData2.getTotalBytes()).thenReturn(2000l);

        ProducerMetricCollector producerMetricCollector = new ProducerMetricCollector(tibjmsAdmin, Lists.newArrayList(includeOnlyQueue1Pattern), false, false, metrics, metricPrefix,
                phaser, collectedMetrics, queueTopicMetricPrefixes, displayDynamicIdsInMetricPath);
        producerMetricCollector.run();

        Assert.assertEquals(3, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Queues|Queue1|Producers|TotalMessages", "Custom Metrics|EMS|Queues|Queue1|Producers|TotalBytes", "Custom Metrics|EMS|Queues|Queue1|Producers|MessageRate");
        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Queues|Queue1|Producers|TotalMessages".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue1|Producers|TotalBytes".equals(metricPath)) {
                Assert.assertEquals("1000", metricValue);
            } else if ("Custom Metrics|EMS|Queues|Queue1|Producers|MessageRate".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            }
        }
    }

    private Metrics setupProducerMetrics() {

        Metrics producerMetrics = new Metrics();
        producerMetrics.setEnabled("true");
        producerMetrics.setType("Producer");
        producerMetrics.setMetricPrefix("Producers");

        List<Metric> metricList = Lists.newArrayList();

        Metric metric1 = new Metric();
        metric1.setAttr("TotalMessages");
        metric1.setAggregationType("SUM");
        metric1.setTimeRollUpType("CURRENT");
        metric1.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric1);

        Metric metric2 = new Metric();
        metric2.setAttr("TotalBytes");
        metric2.setAggregationType("SUM");
        metric2.setTimeRollUpType("CURRENT");
        metric2.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric2);

        Metric metric3 = new Metric();
        metric3.setAttr("MessageRate");
        metric3.setAggregationType("SUM");
        metric3.setTimeRollUpType("AVERAGE");
        metric3.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric3);

        producerMetrics.setMetrics(metricList.toArray(new Metric[]{}));

        return producerMetrics;
    }
}