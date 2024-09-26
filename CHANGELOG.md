### Version 3.0.2
* Updated to latest version of commons (2.2.13).
### Version 3.0.1
* Updated to latest version of commons (2.2.4).
### Version 3.0.0
* Ported to latest extension commons ( 2.2.3 ) which fixes the log4j issues.
* Added fault tolerant server configuration.
* Added configuration to include destinations.
* Added configuration to add/remove metrics in metrics.xml.
* Removed dynamic connection metrics which are creating stale metrics and moved these metrics to respected Queue/Topic.
* Added sslCiphers to provide list of ciphers to use to connect when using ssl.
### Version 2.4.2
* Revamped to support new extension framework.
* Added support for multiple EMS servers.
* Moved configuration to config.yml file.
* Display name can be null or empty.
### Version 2.4.1.3
* Encryption support for sslIdentityPassword.
* Adding additional metrics for Server, Route and Queue.
### Version 2.3.7
* Added missing commons-lang.jar to classpath.
### Version 2.3.6
* Changed logging level in shouldMonitorDestination() from INFO to DEBUG.
### Version 2.3.5
* Adds new SSL configuration properties: sslIdentityFile, sslIdentityPassword, sslTrustedCerts, sslIssuerCerts, sslDebug, sslVendor, sslVerifyHost, sslVerifyHostName. Please check the monitor.xml bundled with this release for details.
### Version 2.3.4
* Adding support for SSL and topics.
### Version 2.3.2
* Added new derived metrics: InboundMessagesPerMinute, OutboundMessagesPerMinute, InboundBytesPerMinute, and OutboundBytesPerMinute.
* General cleanup of code.
### Version 2.3.1
* Added queuesToExclude option to configuration.
### Version 2.3
* Added protocol (tcp/ssl) option to configuration.
### Version 2.2.1
* Recompiled for target JDK 1.5.
### Version 2.2.0
* Cleaned up directory structure.
* Rebuilt Ant scripts.