package com.aroch.flink.metrics.dogstatsd;

import org.apache.flink.metrics.Meter;

import java.util.List;

public class DMeter extends DMetric {

    private final Meter meter;

    public DMeter(Meter m, String metricName, List<String> tags) {
        super(MetricType.meter, metricName, tags);
        meter = m;
    }

    public Meter getMeter() {
        return meter;
    }
}
