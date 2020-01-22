package org.apache.flink.metrics.dogstatsd;

import java.util.List;

public abstract class DMetric {

    private final String metric;
    private final MetricType type;
    private final List<String> tags;

    public DMetric(MetricType metricType, String metric, List<String> tags) {
        this.type = metricType;
        this.metric = metric;
        this.tags = tags;
    }

    public MetricType getType() {
        return type;
    }

    public String getMetric() {
        return metric;
    }

    public List<String> getTags() {
        return tags;
    }
}
