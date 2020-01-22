package org.apache.flink.metrics.dogstatsd;

import org.apache.flink.metrics.Histogram;

import java.util.List;

public class DHistogram extends DMetric {

    private final Histogram histogram;

    public DHistogram(Histogram h, String metricName, List<String> tags) {
        super(MetricType.histogram, metricName, tags);
        histogram = h;
    }

    public Histogram getHistogram() {
        return histogram;
    }
}
