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
import com.tibco.tibjms.admin.DurableInfo;
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
public class DurableMetricCollectorTest {

    @Mock
    private TibjmsAdmin tibjmsAdmin;

    @Mock
    private Phaser phaser;

    @Mock
    private DurableInfo durableInfo1;
    @Mock
    private DurableInfo durableInfo2;


    private String includeAllPatternString = ".*";
    private Pattern includeAllPattern = Pattern.compile(includeAllPatternString);

    private String includeOnlyDurable1PatternString = "Durable1";
    private Pattern includeOnlyDurable1Pattern = Pattern.compile(includeOnlyDurable1PatternString);

    @Test
    public void testCollect() throws TibjmsAdminException {

        Metrics metrics = setupDurableMetrics();

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();

        String metricPrefix = "Custom Metrics|EMS";

        Map<String, String> queueTopicMetricPrefixes = Maps.newHashMap();
        queueTopicMetricPrefixes.put("Queue", "Queues");
        queueTopicMetricPrefixes.put("Topic", "Topics");


        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);

        when(tibjmsAdmin.getDurables()).thenReturn(new DurableInfo[]{durableInfo1, durableInfo2});

        when(durableInfo1.getDurableName()).thenReturn("Durable1");
        when(durableInfo2.getDurableName()).thenReturn("Durable2");

        when(durableInfo1.getTopicName()).thenReturn("Topic1");
        when(durableInfo2.getTopicName()).thenReturn("Topic1");

        when(durableInfo1.getPendingMessageCount()).thenReturn(100l);
        when(durableInfo1.getPendingMessageSize()).thenReturn(1000l);

        when(durableInfo2.getPendingMessageCount()).thenReturn(200l);
        when(durableInfo2.getPendingMessageSize()).thenReturn(2000l);


        DurableMetricCollector durableMetricCollector = new DurableMetricCollector(tibjmsAdmin, Lists.newArrayList(includeAllPattern), false, false, metrics,
                metricPrefix, phaser, collectedMetrics, queueTopicMetricPrefixes);
        durableMetricCollector.run();


        Assert.assertEquals(4, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Topics|Topic1|Durables|Durable1|PendingMessageCount", "Custom Metrics|EMS|Topics|Topic1|Durables|Durable1|PendingMessageSize",
                "Custom Metrics|EMS|Topics|Topic1|Durables|Durable2|PendingMessageCount", "Custom Metrics|EMS|Topics|Topic1|Durables|Durable2|PendingMessageSize");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Topics|Topic1|Durables|Durable1|PendingMessageCount".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Topics|Topic1|Durables|Durable1|PendingMessageSize".equals(metricPath)) {
                Assert.assertEquals("1000", metricValue);
            } else if ("Custom Metrics|EMS|Topics|Topic1|Durables|Durable2|PendingMessageCount".equals(metricPath)) {
                Assert.assertEquals("200", metricValue);
            } else if ("Custom Metrics|EMS|Topics|Topic1|Durables|Durable2|PendingMessageSize".equals(metricPath)) {
                Assert.assertEquals("2000", metricValue);
            }
        }
    }

    @Test
    public void testCollectMatched() throws TibjmsAdminException {

        Metrics metrics = setupDurableMetrics();

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();

        String metricPrefix = "Custom Metrics|EMS";

        Map<String, String> queueTopicMetricPrefixes = Maps.newHashMap();
        queueTopicMetricPrefixes.put("Queue", "Queues");
        queueTopicMetricPrefixes.put("Topic", "Topics");


        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);

        when(tibjmsAdmin.getDurables()).thenReturn(new DurableInfo[]{durableInfo1, durableInfo2});

        when(durableInfo1.getDurableName()).thenReturn("Durable1");
        when(durableInfo2.getDurableName()).thenReturn("Durable2");

        when(durableInfo1.getTopicName()).thenReturn("Topic1");
        when(durableInfo2.getTopicName()).thenReturn("Topic1");

        when(durableInfo1.getPendingMessageCount()).thenReturn(100l);
        when(durableInfo1.getPendingMessageSize()).thenReturn(1000l);

        when(durableInfo2.getPendingMessageCount()).thenReturn(200l);
        when(durableInfo2.getPendingMessageSize()).thenReturn(2000l);


        DurableMetricCollector durableMetricCollector = new DurableMetricCollector(tibjmsAdmin, Lists.newArrayList(includeOnlyDurable1Pattern), false, false, metrics,
                metricPrefix, phaser, collectedMetrics, queueTopicMetricPrefixes);
        durableMetricCollector.run();


        Assert.assertEquals(2, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Topics|Topic1|Durables|Durable1|PendingMessageCount", "Custom Metrics|EMS|Topics|Topic1|Durables|Durable1|PendingMessageSize");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Topics|Topic1|Durables|Durable1|PendingMessageCount".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Topics|Topic1|Durables|Durable1|PendingMessageSize".equals(metricPath)) {
                Assert.assertEquals("1000", metricValue);
            }
        }
    }


    private Metrics setupDurableMetrics() {

        Metrics queueMetrics = new Metrics();
        queueMetrics.setEnabled("true");
        queueMetrics.setType("Durable");
        queueMetrics.setMetricPrefix("Durables");

        List<Metric> metricList = Lists.newArrayList();

        Metric metric1 = new Metric();
        metric1.setAttr("PendingMessageCount");
        metric1.setAggregationType("AVERAGE");
        metric1.setTimeRollUpType("AVERAGE");
        metric1.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric1);

        Metric metric2 = new Metric();
        metric2.setAttr("PendingMessageSize");
        metric2.setAggregationType("AVERAGE");
        metric2.setTimeRollUpType("AVERAGE");
        metric2.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric2);

        queueMetrics.setMetrics(metricList.toArray(new Metric[]{}));


        return queueMetrics;
    }
}
