/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco;

import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.tibco.collectors.ConsumerMetricCollector;
import com.appdynamics.extensions.tibco.collectors.DurableMetricCollector;
import com.appdynamics.extensions.tibco.collectors.ProducerMetricCollector;
import com.appdynamics.extensions.tibco.collectors.QueueMetricCollector;
import com.appdynamics.extensions.tibco.collectors.RouteMetricCollector;
import com.appdynamics.extensions.tibco.collectors.ServerMetricCollector;
import com.appdynamics.extensions.tibco.collectors.TopicMetricCollector;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.cache.Cache;
import com.tibco.tibjms.admin.ServerInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Satish Muddam
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({TibcoEMSMetricFetcher.class})
public class TibcoEMSMetricFetcherTest {

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    @Mock
    private MonitorContextConfiguration configuration;

    @Mock
    private MonitorContext monitorContext;

    @Mock
    private MonitorExecutorService executorService;

    @Mock
    private Metrics.EMSMetrics emsMetrics;

    @Mock
    private ServerMetricCollector serverMetricCollector;

    @Mock
    private QueueMetricCollector queueMetricCollector;

    @Mock
    private TopicMetricCollector topicMetricCollector;

    @Mock
    private ProducerMetricCollector producerMetricCollector;

    @Mock
    private ConsumerMetricCollector consumerMetricCollector;

    @Mock
    private RouteMetricCollector routeMetricCollector;

    @Mock
    private DurableMetricCollector durableMetricCollector;


    @Mock
    private TibjmsAdmin tibjmsAdmin;

    @Mock
    private ServerInfo serverInfo;

    @Mock
    private Cache<String, TibjmsAdmin> connectionCache;


    @Test
    public void testShouldExecuteAllCollectorsWhenEnabled() throws Exception {

        Map<String, Object> emsServer = setupEMSServer();

        when(configuration.getMetricsXml()).thenReturn(emsMetrics);

        when(emsMetrics.isShowSystem()).thenReturn(false);
        when(emsMetrics.isShowTemp()).thenReturn(false);

        Metrics[] allMetrics = setupMetrics("true", "true", "true", "true", "true", "true", "true", "true");


        when(emsMetrics.getMetrics()).thenReturn(allMetrics);

        PowerMockito.whenNew(TibjmsAdmin.class).withAnyArguments().thenReturn(tibjmsAdmin);

        when(tibjmsAdmin.getInfo()).thenReturn(serverInfo);
        when(serverInfo.getConnectionCount()).thenReturn(10);

        when(configuration.getMetricPrefix()).thenReturn("Custom Metrics|Tibco EMS");

        when(configuration.getContext()).thenReturn(monitorContext);
        when(monitorContext.getExecutorService()).thenReturn(executorService);


        doAnswer((Answer<Object>) invocationOnMock -> {
            Runnable runnable = invocationOnMock.getArgumentAt(1, Runnable.class);
            runnable.run();
            return null;
        }).when(executorService).execute(anyString(), any(Runnable.class));


        PowerMockito.whenNew(ServerMetricCollector.class).withAnyArguments().thenReturn(serverMetricCollector);
        PowerMockito.whenNew(QueueMetricCollector.class).withAnyArguments().thenReturn(queueMetricCollector);
        PowerMockito.whenNew(TopicMetricCollector.class).withAnyArguments().thenReturn(topicMetricCollector);
        PowerMockito.whenNew(ProducerMetricCollector.class).withAnyArguments().thenReturn(producerMetricCollector);
        PowerMockito.whenNew(ConsumerMetricCollector.class).withAnyArguments().thenReturn(consumerMetricCollector);
        PowerMockito.whenNew(RouteMetricCollector.class).withAnyArguments().thenReturn(routeMetricCollector);
        PowerMockito.whenNew(DurableMetricCollector.class).withAnyArguments().thenReturn(durableMetricCollector);

        //when(serverMetricCollector.run()).then()

        TibcoEMSMetricFetcher tibcoEMSMetricFetcher = new TibcoEMSMetricFetcher(serviceProvider, configuration, emsServer, connectionCache);
        tibcoEMSMetricFetcher.run();

        verify(serverMetricCollector, times(1)).run();
        verify(queueMetricCollector, times(1)).run();
        verify(topicMetricCollector, times(1)).run();
        verify(producerMetricCollector, times(1)).run();
        verify(consumerMetricCollector, times(1)).run();
        verify(routeMetricCollector, times(1)).run();
        verify(durableMetricCollector, times(1)).run();
    }

    @Test
    public void testShouldExecuteOnlyEnabledCollectors() throws Exception {

        Map<String, Object> emsServer = setupEMSServer();

        when(configuration.getMetricsXml()).thenReturn(emsMetrics);

        when(emsMetrics.isShowSystem()).thenReturn(false);
        when(emsMetrics.isShowTemp()).thenReturn(false);

        Metrics[] allMetrics = setupMetrics("true", "true", "true", "true", "false", "false", "false", "false");


        when(emsMetrics.getMetrics()).thenReturn(allMetrics);

        PowerMockito.whenNew(TibjmsAdmin.class).withAnyArguments().thenReturn(tibjmsAdmin);

        when(tibjmsAdmin.getInfo()).thenReturn(serverInfo);
        when(serverInfo.getConnectionCount()).thenReturn(10);

        when(configuration.getMetricPrefix()).thenReturn("Custom Metrics|Tibco EMS");

        when(configuration.getContext()).thenReturn(monitorContext);
        when(monitorContext.getExecutorService()).thenReturn(executorService);


        doAnswer((Answer<Object>) invocationOnMock -> {
            Runnable runnable = invocationOnMock.getArgumentAt(1, Runnable.class);
            runnable.run();
            return null;
        }).when(executorService).execute(anyString(), any(Runnable.class));


        PowerMockito.whenNew(ServerMetricCollector.class).withAnyArguments().thenReturn(serverMetricCollector);
        PowerMockito.whenNew(QueueMetricCollector.class).withAnyArguments().thenReturn(queueMetricCollector);
        PowerMockito.whenNew(TopicMetricCollector.class).withAnyArguments().thenReturn(topicMetricCollector);
        PowerMockito.whenNew(ProducerMetricCollector.class).withAnyArguments().thenReturn(producerMetricCollector);
        PowerMockito.whenNew(ConsumerMetricCollector.class).withAnyArguments().thenReturn(consumerMetricCollector);
        PowerMockito.whenNew(RouteMetricCollector.class).withAnyArguments().thenReturn(routeMetricCollector);
        PowerMockito.whenNew(DurableMetricCollector.class).withAnyArguments().thenReturn(durableMetricCollector);

        //when(serverMetricCollector.run()).then()

        TibcoEMSMetricFetcher tibcoEMSMetricFetcher = new TibcoEMSMetricFetcher(serviceProvider, configuration, emsServer, connectionCache);
        tibcoEMSMetricFetcher.run();

        verify(serverMetricCollector, times(1)).run();
        verify(queueMetricCollector, times(1)).run();
        verify(topicMetricCollector, times(1)).run();
        verify(producerMetricCollector, times(1)).run();
        verify(consumerMetricCollector, times(0)).run();
        verify(routeMetricCollector, times(0)).run();
        verify(durableMetricCollector, times(0)).run();
    }

    private Metrics[] setupMetrics(String enableServer, String enableQueue, String enableTopic, String enableProducer,
                                   String enableConsumer, String enableRoute, String enableDurable, String enableConnection) {
        List<Metrics> allMetricsList = new ArrayList<>();

        Metrics serverMetrics = new Metrics();
        serverMetrics.setEnabled(enableServer);
        serverMetrics.setType("Server");
        allMetricsList.add(serverMetrics);

        Metrics queueMetrics = new Metrics();
        queueMetrics.setEnabled(enableQueue);
        queueMetrics.setType("Queue");
        allMetricsList.add(queueMetrics);

        Metrics topicMetrics = new Metrics();
        topicMetrics.setEnabled(enableTopic);
        topicMetrics.setType("Topic");
        allMetricsList.add(topicMetrics);

        Metrics producerMetrics = new Metrics();
        producerMetrics.setEnabled(enableProducer);
        producerMetrics.setType("Producer");
        allMetricsList.add(producerMetrics);

        Metrics consumerMetrics = new Metrics();
        consumerMetrics.setEnabled(enableConsumer);
        consumerMetrics.setType("Consumer");
        allMetricsList.add(consumerMetrics);

        Metrics routeMetrics = new Metrics();
        routeMetrics.setEnabled(enableRoute);
        routeMetrics.setType("Route");
        allMetricsList.add(routeMetrics);

        Metrics durableMetrics = new Metrics();
        durableMetrics.setEnabled(enableDurable);
        durableMetrics.setType("Durable");
        allMetricsList.add(durableMetrics);

        Metrics connectionMetrics = new Metrics();
        connectionMetrics.setEnabled(enableConnection);
        connectionMetrics.setType("Connection");
        allMetricsList.add(connectionMetrics);


        return allMetricsList.toArray(new Metrics[]{});
    }

    private Map<String, Object> setupEMSServer() {
        Map<String, Object> emsServer = new HashMap<>();
        emsServer.put("displayName", "TestEMSServer");
        emsServer.put("host", "localhost");
        emsServer.put("port", "6222");
        emsServer.put("protocol", "tcp");
        emsServer.put("user", "admin");
        emsServer.put("password", "admin");
        return emsServer;
    }

}
