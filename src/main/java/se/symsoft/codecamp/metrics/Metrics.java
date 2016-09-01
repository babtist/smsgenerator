/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class Metrics {
    private static final String GRAPHITE_HOST = "graphite.service.consul";

    private static final int GRAPHITE_PORT = 2003;
    private static final String GRAPHITE_PREFIX = "smsgenerator";

    public static final MetricRegistry METRIC_REGISTRY = SharedMetricRegistries.getOrCreate("metrics");

    public static void startGraphiteMetricsReporter() {
        startGraphiteMetricsReporter(1, TimeUnit.SECONDS);
    }

    public static void startGraphiteMetricsReporter(long period, TimeUnit timeUnit) {
        final Graphite graphite =
                new Graphite(new InetSocketAddress(GRAPHITE_HOST, GRAPHITE_PORT));
        final GraphiteReporter reporter =
                GraphiteReporter.forRegistry(METRIC_REGISTRY)
                         .prefixedWith(GRAPHITE_PREFIX)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .filter(MetricFilter.ALL)
                        .build(graphite);
        reporter.start(period, timeUnit);
    }

}
