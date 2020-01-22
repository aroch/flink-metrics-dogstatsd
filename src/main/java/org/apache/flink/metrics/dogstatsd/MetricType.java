package org.apache.flink.metrics.dogstatsd;

/**
 * Metric types supported by Datadog.
 */
public enum MetricType {
    gauge('g'), counter('c'), histogram('h'), meter('m');

    private char letter;

    MetricType(char letter) {
        this.letter = letter;
    }

    public char getLetter() {
        return letter;
    }
}
