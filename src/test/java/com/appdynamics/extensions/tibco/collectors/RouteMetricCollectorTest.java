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
import com.tibco.tibjms.admin.RouteInfo;
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
public class RouteMetricCollectorTest {

    @Mock
    private TibjmsAdmin tibjmsAdmin;

    @Mock
    private Phaser phaser;

    @Mock
    private RouteInfo routeInfo1;
    @Mock
    private RouteInfo routeInfo2;

    @Mock
    private StatData inboundStatData1;
    @Mock
    private StatData inboundStatData2;

    @Mock
    private StatData outboundStatData1;
    @Mock
    private StatData outboundStatData2;

    private String includeAllPatternString = ".*";
    private Pattern includeAllPattern = Pattern.compile(includeAllPatternString);

    private String includeOnlyRoute1PatternString = "Route1";
    private Pattern includeOnlyRoute1Pattern = Pattern.compile(includeOnlyRoute1PatternString);

    @Test
    public void testCollect() throws TibjmsAdminException {

        Metrics metrics = setupRouteMetrics();
        String metricPrefix = "Custom Metrics|EMS";

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);

        when(tibjmsAdmin.getRoutes()).thenReturn(new RouteInfo[]{routeInfo1, routeInfo2});

        when(routeInfo1.getName()).thenReturn("Route1");
        when(routeInfo2.getName()).thenReturn("Route2");

        when(routeInfo1.getBacklogCount()).thenReturn(1l);
        when(routeInfo2.getBacklogCount()).thenReturn(2l);

        when(routeInfo1.getInboundStatistics()).thenReturn(inboundStatData1);
        when(routeInfo2.getInboundStatistics()).thenReturn(inboundStatData2);

        when(routeInfo1.getOutboundStatistics()).thenReturn(outboundStatData1);
        when(routeInfo2.getOutboundStatistics()).thenReturn(outboundStatData2);

        when(inboundStatData1.getMessageRate()).thenReturn(10l);
        when(inboundStatData1.getTotalMessages()).thenReturn(100l);
        when(outboundStatData1.getMessageRate()).thenReturn(15l);
        when(outboundStatData1.getTotalMessages()).thenReturn(150l);

        when(inboundStatData2.getMessageRate()).thenReturn(20l);
        when(inboundStatData2.getTotalMessages()).thenReturn(200l);
        when(outboundStatData2.getMessageRate()).thenReturn(25l);
        when(outboundStatData2.getTotalMessages()).thenReturn(250l);

        RouteMetricCollector routeMetricCollector = new RouteMetricCollector(tibjmsAdmin, Lists.newArrayList(includeAllPattern), false, false, metrics, metricPrefix,
                phaser, collectedMetrics);

        routeMetricCollector.run();

        Assert.assertEquals(10, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Routes|Route1|BacklogCount", "Custom Metrics|EMS|Routes|Route1|InboundMessageRate", "Custom Metrics|EMS|Routes|Route1|InboundTotalMessages",
                "Custom Metrics|EMS|Routes|Route1|OutboundMessageRate", "Custom Metrics|EMS|Routes|Route1|OutboundTotalMessages", "Custom Metrics|EMS|Routes|Route2|BacklogCount", "Custom Metrics|EMS|Routes|Route2|InboundMessageRate",
                "Custom Metrics|EMS|Routes|Route2|InboundTotalMessages", "Custom Metrics|EMS|Routes|Route2|OutboundMessageRate", "Custom Metrics|EMS|Routes|Route2|OutboundTotalMessages");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Routes|Route1|BacklogCount".equals(metricPath)) {
                Assert.assertEquals("1", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route1|InboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route1|InboundTotalMessages".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route1|OutboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("15", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route1|OutboundTotalMessages".equals(metricPath)) {
                Assert.assertEquals("150", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route2|BacklogCount".equals(metricPath)) {
                Assert.assertEquals("2", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route2|InboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("20", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route2|InboundTotalMessages".equals(metricPath)) {
                Assert.assertEquals("200", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route2|OutboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("25", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route2|OutboundTotalMessages".equals(metricPath)) {
                Assert.assertEquals("250", metricValue);
            }
        }
    }

    @Test
    public void testCollectMatched() throws TibjmsAdminException {

        Metrics metrics = setupRouteMetrics();
        String metricPrefix = "Custom Metrics|EMS";

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = Lists.newArrayList();

        when(phaser.register()).thenReturn(1);
        when(phaser.arriveAndDeregister()).thenReturn(0);

        when(tibjmsAdmin.getRoutes()).thenReturn(new RouteInfo[]{routeInfo1, routeInfo2});

        when(routeInfo1.getName()).thenReturn("Route1");
        when(routeInfo2.getName()).thenReturn("Route2");

        when(routeInfo1.getBacklogCount()).thenReturn(1l);
        when(routeInfo2.getBacklogCount()).thenReturn(2l);

        when(routeInfo1.getInboundStatistics()).thenReturn(inboundStatData1);
        when(routeInfo2.getInboundStatistics()).thenReturn(inboundStatData2);

        when(routeInfo1.getOutboundStatistics()).thenReturn(outboundStatData1);
        when(routeInfo2.getOutboundStatistics()).thenReturn(outboundStatData2);

        when(inboundStatData1.getMessageRate()).thenReturn(10l);
        when(inboundStatData1.getTotalMessages()).thenReturn(100l);
        when(outboundStatData1.getMessageRate()).thenReturn(15l);
        when(outboundStatData1.getTotalMessages()).thenReturn(150l);

        when(inboundStatData2.getMessageRate()).thenReturn(20l);
        when(inboundStatData2.getTotalMessages()).thenReturn(200l);
        when(outboundStatData2.getMessageRate()).thenReturn(25l);
        when(outboundStatData2.getTotalMessages()).thenReturn(250l);

        RouteMetricCollector routeMetricCollector = new RouteMetricCollector(tibjmsAdmin, Lists.newArrayList(includeOnlyRoute1Pattern), false, false, metrics, metricPrefix,
                phaser, collectedMetrics);

        routeMetricCollector.run();

        Assert.assertEquals(5, collectedMetrics.size());

        List<String> allMetrics = Lists.newArrayList("Custom Metrics|EMS|Routes|Route1|BacklogCount", "Custom Metrics|EMS|Routes|Route1|InboundMessageRate", "Custom Metrics|EMS|Routes|Route1|InboundTotalMessages",
                "Custom Metrics|EMS|Routes|Route1|OutboundMessageRate", "Custom Metrics|EMS|Routes|Route1|OutboundTotalMessages");

        for (com.appdynamics.extensions.metrics.Metric metric : collectedMetrics) {
            String metricPath = metric.getMetricPath();
            String metricValue = metric.getMetricValue();

            Assert.assertTrue(allMetrics.contains(metricPath));
            if ("Custom Metrics|EMS|Routes|Route1|BacklogCount".equals(metricPath)) {
                Assert.assertEquals("1", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route1|InboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("10", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route1|InboundTotalMessages".equals(metricPath)) {
                Assert.assertEquals("100", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route1|OutboundMessageRate".equals(metricPath)) {
                Assert.assertEquals("15", metricValue);
            } else if ("Custom Metrics|EMS|Routes|Route1|OutboundTotalMessages".equals(metricPath)) {
                Assert.assertEquals("150", metricValue);
            }
        }
    }


    private Metrics setupRouteMetrics() {

        Metrics routeMetrics = new Metrics();
        routeMetrics.setEnabled("true");
        routeMetrics.setType("Route");
        routeMetrics.setMetricPrefix("Routes");

        List<Metric> metricList = Lists.newArrayList();

        Metric metric1 = new Metric();
        metric1.setAttr("BacklogCount");
        metric1.setAggregationType("AVERAGE");
        metric1.setTimeRollUpType("AVERAGE");
        metric1.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric1);

        Metric metric2 = new Metric();
        metric2.setAttr("InboundMessageRate");
        metric2.setAggregationType("AVERAGE");
        metric2.setTimeRollUpType("AVERAGE");
        metric2.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric2);

        Metric metric3 = new Metric();
        metric3.setAttr("InboundTotalMessages");
        metric3.setAggregationType("AVERAGE");
        metric3.setTimeRollUpType("AVERAGE");
        metric3.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric3);

        Metric metric4 = new Metric();
        metric4.setAttr("OutboundMessageRate");
        metric4.setAggregationType("AVERAGE");
        metric4.setTimeRollUpType("AVERAGE");
        metric4.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric4);

        Metric metric5 = new Metric();
        metric5.setAttr("OutboundTotalMessages");
        metric5.setAggregationType("AVERAGE");
        metric5.setTimeRollUpType("AVERAGE");
        metric5.setClusterRollUpType("COLLECTIVE");
        metricList.add(metric5);

        routeMetrics.setMetrics(metricList.toArray(new Metric[]{}));


        return routeMetrics;
    }
}
