
TibcoEMSMonitor
===============

## Introduction

An AppDynamics Machine Agent add-on to report metrics from a Tibco EMS 
Server and its queues.

Tibco EMS is messaging middleware that provides persistent queues as well as a 
publish/subscribe mechanism. It can be used as a JMS provider, or it can be 
used directly via native APIs.

This extension requires the Java Machine Agent.


## Prerequisites

Before starting this monitor, make sure your EMS server is configured to report 
statistics. You can do this by editing the file `tibemsd.conf` in your 
`TIBCO_HOME`, or by using the `tibemsadmin` command line utility.


#### Configuring statistics by editing tibemsd.conf

1. Add the following line to `tibemsd.conf`:

        statistics = enabled

2. Restart Tibco EMS.


#### Configuring statistics using the command line

Use the `tibemsadmin` utility to change the server configuration.

        root# tibemsadmin -server localhost:7222

        TIBCO Enterprise Message Service Administration Tool.
        Copyright 2003-2013 by TIBCO Software Inc.
        All rights reserved.

        Version 8.0.0 V9 6/7/2013

        Login name (admin):
        Password:
        Connected to: tcp://localhost:7222
        Type 'help' for commands help, 'exit' to exit:
        tcp://localhost:7222> set server statistics=enabled
        Server parameters have been changed
        tcp://localhost:7222> quit
        bye


## Installation

1. Download TibcoEMSMonitor.zip from the Community.
2. Copy TibcoEMSMonitor.zip into the directory where you installed the machine 
   agent, under `$MACHINE_AGENT_HOME/monitors`.
3. Unzip the file. This will create a new directory called `TibcoEMSMonitor`.
4. In `$MACHINE_AGENT_HOME/monitors/TibcoEMSMonitor`, edit the file `config.yml` and 
   configure the extension for your Tibco EMS installation.
5. Add the following Tibco EMS jars, tibcrypt.jar, tibemsd_sec.jar, tibjms.jar, tibjmsadmin.jar, tibjmsapps.jar, tibjmsufo.jar, tibrvjms.jar to the lib folder   
6. Restart the machine agent.


## Configuration

Configuration for this monitor is in the `config.yml` file in the monitor 
directory.

 * host - Name or IP address of the Tibco EMS server. Required.
 * port - TCP port number where the Tibco server is listening. The default value is 7222. Required.
 * protocol - Specify "tcp" to use standard TCP or "ssl" to use SSL. The default is "tcp".
 * user - Administrative user ID for the Tibco admin interface. The default value is "admin". Required.
 * password - Password for the administrative user ID. The default value is an empty password. Required.
 * collectConnections - configuration to collect connection related metrics
 * collectDurables - configuration to collect durables related metrics
 * collectRoutes - configuration to collect routes related metrics
 * collectConsumers - configuration to collect consumer related metrics
 * collectProducers - configuration to collect producer related metrics
 * collectQueues - configuration to collect queues related metrics
 * collectTopics - configuration to collect topics related metrics
 * showTemp - configuration to collect metrics from temp queues
 * showSystem - configuration to collect metrics from system queues
 * excludeQueues - Define queues which you want to exclude
 * excludeTopics - Define topics which you want to exclude
 
 Sample config.yml
 
 ```
 #########
## If you are using SSL to connect to EMS, make sure you also set the following:
##
## sslIdentityFile: path to the private key and client certificate file (example: conf/client_identity.p12)
## sslIdentityPassword: password to decrypt the private key
## sslTrustedCerts: path to the server certificate file (example: conf/server_cert.pem)
##########
emsServers:
   - displayName: "Local EMS"
     host: "localhost"
     port: 7222
     # Supports tcp and ssl
     protocol: "tcp"
     user: "admin"
     # password or encryptedPassword and encryptionKey are required
     password:
     encryptedPassword:
     encryptionKey:
     collectConnections: false
     collectDurables: false
     collectRoutes: false
     collectConsumers: false
     collectProducers: false
     collectQueues: true
     collectTopics: true
     #config to show temp queues and topics
     showTemp: false
     #config to show system queues and topics
     showSystem: false
     excludeQueues: []
     excludeTopics: []
     sslIdentityFile:
     sslIdentityPassword:
     sslTrustedCerts:
     #ssl config optional settings
     sslDebug:
     sslVerifyHost:
     sslVerifyHostName:
     sslVendor:
   - displayName: "Remote EMS"
     host: "localhost"
     port: 7222
     # Supports tcp and ssl
     protocol: "tcp"
     user: "admin"
     password:
     encryptedPassword:
     encryptionKey:
     collectConnections: true
     collectDurables: true
     collectRoutes: true
     collectConsumers: true
     collectProducers: true
     collectQueues: true
     collectTopics: true
     #config to show temp queues and topics
     showTemp: false
     #config to show system queues and topics
     showSystem: false
     excludeQueues: []
     excludeTopics: []


# number of concurrent tasks
numberOfThreads: 1

#This will create this metric in all the tiers, under this path. Please make sure to have a trailing |
#metricPrefix: "Custom Metrics|Tibco EMS|"

#This will create it in specific Tier aka Component. Replace <COMPONENT_ID>. Please make sure to have a trailing |.
#To find out the COMPONENT_ID, please see the screen shot here https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
metricPrefix: "Server|Component:<COMPONENT_ID>|Custom Metrics|Tibco EMS|"
 
 ```

## Metrics Provided

### Connection Metrics

| Metric Name        |
| :----------------- |
| SessionCount       |
| ConsumerCount      |
| ProducerCount      | 
| StartTime          | 
| UpTime             |


### Durable Metrics

| Metric Name            |
| :--------------------- |
| PendingMessageCount    |
| PendingMessageSize     |

### Routes Metrics

| Metric Name            |
| :--------------------- |
| InboundMessageRate     |
| InboundTotalMessages   |
| OutboundMessageRate    |
| OutboundTotalMessages  |

### Consumer and Producer Metrics

| Metric Name       |
| :---------------- |
| ConnectionID      |
| SessionID         |
| TotalMessages     |
| TotalBytes        |
| MessageRate       |

### Queue Metrics

| Metric Name                  |
| :--------------------------- |
| ConsumerCount                |
| PendingMessageCount          |
| PendingMessageSize           |
| FlowControlMaxBytes          |
| MaxMsgs                      |
| MaxBytes                     |
| InboundByteRate              |
| InboundMessageRate           |
| InboundByteCount             |
| InboundMessageCount          |
| OutboundByteRate             |
| OutboundMessageRate          |
| OutboundByteCount            |
| OutboundMessageCount         |
| InboundMessagesPerMinute     |
| OutboundMessagesPerMinute    |
| InboundBytesPerMinute        |
| OutboundBytesPerMinute       |
| InTransitCount               | 
| ReceiverCount                |
| MaxRedelivery                |

### Topic Metrics

| Metric Name                  |
| :--------------------------- |
| ConsumerCount                |
| PendingMessageCount          |
| PendingMessageSize           |
| FlowControlMaxBytes          |
| MaxMsgs                      |
| MaxBytes                     |
| InboundByteRate              |
| InboundMessageRate           |
| InboundByteCount             |
| InboundMessageCount          |
| OutboundByteRate             |
| OutboundMessageRate          |
| OutboundByteCount            |
| OutboundMessageCount         |
| InboundMessagesPerMinute     |
| OutboundMessagesPerMinute    |
| InboundBytesPerMinute        |
| OutboundBytesPerMinute       |
| ConsumerCount                | 
| SubscriberCount              |
| ActiveDurableCount           |
| DurableCount                 |



## Caution

This monitor can potentially register hundred of new metrics, depending on how 
many queues are in EMS. By default, the Machine Agent will only report 200 
metrics to the controller, so you may need to increase that limit when 
installing this monitor. To increase the metric limit, you must add a parameter 
when starting the Machine Agent, like this:

    java -Dappdynamics.agent.maxMetrics=1000 -jar machineagent.jar

## Password Encryption Support

To avoid setting the clear text password in the config.yml, please follow the process to encrypt the password and set the encrypted password and the encryptionKey in the config.yml

1.  To encrypt password from the commandline go to `<Machine_Agent>`/monitors/TibcoEMSMonitor dir and run the below common

<pre>java -cp "tibcoems-monitoring-extension.jar" com.appdynamics.extensions.crypto.Encryptor myKey myPassword</pre>

## Workbench

Workbench is a feature by which you can preview the metrics before registering it with the controller. This is useful if you want to fine tune the configurations. Workbench is embedded into the extension jar.

To use the workbench

* Follow all the installation steps
* Start the workbench with the command
~~~
  java -jar /path/to/MachineAgent/monitors/TibcoEMSMonitor/tibcoems-monitoring-extension.jar
  This starts an http server at http://host:9090/. This can be accessed from the browser.
~~~
* If the server is not accessible from outside/browser, you can use the following end points to see the list of registered metrics and errors.
~~~
    #Get the stats
    curl http://localhost:9090/api/stats
    #Get the registered metrics
    curl http://localhost:9090/api/metric-paths
~~~
* You can make the changes to config.yml and validate it from the browser or the API
* Once the configuration is complete, you can kill the workbench and start the Machine Agent



## Support

For any questions or feature requests, please contact the [AppDynamics Center 
of Excellence][].

**Version:** 2.4.2
**Controller Compatibility:** 3.6 or later  
**Last Updated:** 23-Feb-2017

------------------------------------------------------------------------------

## Release Notes

### Version 2.4.2
  - Revamped to support new extension framework
  - Added support for multiple EMS servers
  - Moved configuration to config.yml file
  - Display name can be null or empty
  
### Version 2.4.1.3
  - Encryption support for sslIdentityPassword
  - Adding additional metrics for Server, Route and Queue  
  
### Version 2.3.7
  - Added missing `commons-lang.jar` to classpath.

### Version 2.3.6
  - Changed logging level in shouldMonitorDestination() from `INFO` to `DEBUG`.

### Version 2.3.5
  - Adds new SSL configuration properties: `sslIdentityFile`, `sslIdentityPassword`, `sslTrustedCerts`, 
    `sslIssuerCerts`, `sslDebug`, `sslVendor`, `sslVerifyHost`, `sslVerifyHostName`. 
    Please check the `monitor.xml` bundled with this release for details.

### Version 2.3.4
  - Adding support for SSL and topics.

### Version 2.3.2
  - Added new derived metrics: InboundMessagesPerMinute, OutboundMessagesPerMinute, 
    InboundBytesPerMinute, and OutboundBytesPerMinute.
  - General cleanup of code.

### Version 2.3.1
  - Added `queuesToExclude` option to configuration.

### Version 2.3
  - Added protocol (tcp/ssl) option to configuration.

### Version 2.2.1
  - Recompiled for target JDK 1.5.

### Version 2.2.0
  - Cleaned up directory structure.
  - Rebuilt Ant scripts.
  

[AppDynamics Center of Excellence]: mailto:help@appdynamics.com
[help@appdynamics.com]: mailto:help@appdynamics.com
