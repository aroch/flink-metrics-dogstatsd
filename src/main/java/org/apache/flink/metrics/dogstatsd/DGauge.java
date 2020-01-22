package org.apache.flink.metrics.dogstatsd;

import org.apache.flink.metrics.Gauge;

import java.util.List;

@SuppressWarnings("rawtypes")
public class DGauge extends DMetric {
	private final Gauge gauge;

	public DGauge(Gauge g, String metricName, List<String> tags) {
		super(MetricType.gauge, metricName, tags);
		gauge = g;
	}

	public Gauge getGauge() {
		return gauge;
	}
}
