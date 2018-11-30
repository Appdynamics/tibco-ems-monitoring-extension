package com.appdynamics.extensions.tibco.metrics;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Satish Muddam
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Metrics {

    @XmlAttribute
    private String type;
    @XmlAttribute
    private String metricPrefix;
    @XmlAttribute
    private String enabled;
    @XmlElement(name = "metric")
    private Metric[] metrics;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public Boolean isEnabled() {
        return Boolean.valueOf(enabled);
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public Metric[] getMetrics() {
        return metrics;
    }

    public void setMetrics(Metric[] metrics) {
        this.metrics = metrics;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EMSMetrics {
        @XmlAttribute
        private String showTemp;
        @XmlAttribute
        private String showSystem;

        @XmlElement(name = "metrics")
        private Metrics[] metrics;


        public Boolean isShowTemp() {
            return Boolean.valueOf(showTemp);
        }

        public void setShowTemp(String showTemp) {
            this.showTemp = showTemp;
        }

        public Boolean isShowSystem() {
            return Boolean.valueOf(showSystem);
        }

        public void setShowSystem(String showSystem) {
            this.showSystem = showSystem;
        }

        public Metrics[] getMetrics() {
            return metrics;
        }

        public void setMetrics(Metrics[] metrics) {
            this.metrics = metrics;
        }
    }
}