package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.tibco.TibcoEMSMetricFetcher;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.tibco.tibjms.admin.TibjmsAdmin;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public abstract class AbstractMetricCollector implements Runnable {

    TibjmsAdmin conn;
    List<Pattern> includePatterns;
    List<Pattern> excludePatterns;
    boolean showSystem;
    boolean showTemp;
    Metrics metrics;
    String metricPrefix;

    protected static ObjectMapper objectMapper = new ObjectMapper();


    public AbstractMetricCollector(TibjmsAdmin conn, List<Pattern> includePatterns, List<Pattern> excludePatterns, boolean showSystem,
                                   boolean showTemp, Metrics metrics, String metricPrefix) {
        this.conn = conn;
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;
        this.showSystem = showSystem;
        this.showTemp = showTemp;
        this.metrics = metrics;
        this.metricPrefix = metricPrefix;
    }

    boolean shouldMonitorDestination(String destName, List<Pattern> patternsToInclude, List<Pattern> patternsToExclude, boolean showSystem, boolean showTemp, TibcoEMSMetricFetcher.DestinationType destinationType, Logger logger) {

        logger.debug("Checking includes and excludes for " + destinationType.getType() + " with name " + destName);

        try {
            if (destName.startsWith("$TMP$.") && !showTemp) {
                logger.debug("Skipping temporary " + destinationType.getType() + " '" + destName + "'");
                return false;
            }

            if (destName.startsWith("$sys.") && !showSystem) {
                logger.debug("Skipping system " + destinationType.getType() + " '" + destName + "'");
                return false;
            }

            if (patternsToInclude != null && patternsToInclude.size() > 0) {
                logger.debug("Using patterns to include [" + patternsToInclude + "] to filter");
                for (Pattern patternToInclude : patternsToInclude) {
                    Matcher matcher = patternToInclude.matcher(destName);
                    if (matcher.matches()) {
                        logger.debug(String.format("Including '%s' '%s' due to include pattern '%s'",
                                destinationType.getType(), destName, patternToInclude.pattern()));
                        return true;
                    }
                }
            } else if (patternsToExclude != null && patternsToExclude.size() > 0) {
                logger.debug("Using patterns to exclude [" + patternsToInclude + "] to filter");
                for (Pattern patternToExclude : patternsToExclude) {
                    Matcher matcher = patternToExclude.matcher(destName);
                    if (matcher.matches()) {
                        logger.debug(String.format("Skipping '%s' '%s' due to excluded pattern '%s'",
                                destinationType.getType(), destName, patternToExclude.pattern()));
                        return false;
                    }
                }

                logger.debug(String.format("Including '%s' '%s' due to not excluded by any exclude pattern",
                        destinationType.getType(), destName));
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.debug("Error in checking includes and excludes for  " + destinationType.getType() + " with name " + destName);
            return false;
        }
    }
}