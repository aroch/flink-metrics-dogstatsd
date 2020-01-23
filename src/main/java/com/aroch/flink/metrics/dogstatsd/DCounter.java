package com.aroch.flink.metrics.dogstatsd;

import org.apache.flink.metrics.Counter;

import java.util.List;

public class DCounter extends DMetric {
    private final Counter counter;

    public DCounter(Counter c, String metricName, List<String> tags) {
        super(MetricType.counter, metricName, tags);
        counter = c;
    }

    public Counter getCounter() {
        return counter;
    }
}
