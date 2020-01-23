package com.aroch.flink.metrics.dogstatsd;

import org.apache.flink.metrics.*;
import org.apache.flink.metrics.reporter.MetricReporter;
import org.apache.flink.metrics.reporter.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DogStatsDReporter implements MetricReporter, Scheduled, CharacterFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogStatsDReporter.class);

    private static final String ARG_HOST = "host";
    private static final String ARG_PORT = "port";
    private static final String TAGS = "tags";

    @SuppressWarnings("rawtypes")
    protected final Map<Gauge, DGauge> gauges = new ConcurrentHashMap<>();
    protected final Map<Counter, DCounter> counters = new ConcurrentHashMap<>();
    protected final Map<Meter, DMeter> meters = new ConcurrentHashMap<>();
    protected final Map<Histogram, DHistogram> histograms = new HashMap<>();

    protected List<String> configTags;

    private boolean closed = false;

    private DatagramSocket socket;
    private InetSocketAddress address;

    @Override
    public void open(MetricConfig config) {
        String host = config.getString(ARG_HOST, null);
        int port = config.getInteger(ARG_PORT, -1);

        configTags = getTagsFromConfig(config.getString(TAGS, ""));

        if (host == null || host.length() == 0 || port < 1) {
            throw new IllegalArgumentException("Invalid host/port configuration. Host: " + host + " Port: " + port);
        }

        this.address = new InetSocketAddress(host, port);

        try {
            this.socket = new DatagramSocket(0);
        } catch (SocketException e) {
            throw new RuntimeException("Could not create datagram socket. ", e);
        }
        LOGGER.info("Configured DatadogStatsDReporter with {host:{}, port:{}, tags:{}}", host, port, configTags);
    }

    @Override
    public void close() {
        closed = true;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void notifyOfAddedMetric(Metric metric, String metricName, MetricGroup group) {

        final String name = group.getMetricIdentifier(metricName, this);

        List<String> tags = new ArrayList<>(configTags);
        tags.addAll(getTagsFromMetricGroup(group));

        if (metric instanceof Counter) {
            Counter c = (Counter) metric;
            counters.put(c, new DCounter(c, name, tags));
        } else if (metric instanceof Gauge) {
            Gauge g = (Gauge) metric;
            gauges.put(g, new DGauge(g, name, tags));
        } else if (metric instanceof Meter) {
            Meter m = (Meter) metric;
            meters.put(m, new DMeter(m, name, tags));
        } else if (metric instanceof Histogram) {
            Histogram h = (Histogram) metric;
            histograms.put(h, new DHistogram(h, name, tags));
        } else {
            LOGGER.warn("Cannot add unknown metric type {}. This indicates that the metrics " +
                    "does not support this metric type.", metric.getClass().getName());
        }
    }

    @Override
    public void notifyOfRemovedMetric(Metric metric, String metricName, MetricGroup group) {
        if (metric instanceof Counter) {
            counters.remove(metric);
        } else if (metric instanceof Gauge) {
            gauges.remove(metric);
        } else if (metric instanceof Meter) {
            meters.remove(metric);
        } else if (metric instanceof Histogram) {
            histograms.remove(metric);
        } else {
            LOGGER.warn("Cannot remove unknown metric type {}. This indicates that the metrics " +
                    "does not support this metric type.", metric.getClass().getName());
        }
    }

    // ------------------------------------------------------------------------

    @Override
    public void report() {

        // instead of locking here, we tolerate exceptions
        // we do this to prevent holding the lock for very long and blocking
        // operator creation and shutdown
        try {
            for (DGauge g : gauges.values()) {
                if (closed) {
                    return;
                }
                reportGauge(g);
            }

            for (DCounter c : counters.values()) {
                if (closed) {
                    return;
                }
                reportCounter(c);
            }

            for (DMeter m : meters.values()) {
                reportMeter(m);
            }

            for (DHistogram h : histograms.values()) {
                reportHistogram(h);
            }
        } catch (ConcurrentModificationException | NoSuchElementException e) {
            // ignore - may happen when metrics are concurrently added or removed
            // report next time
        }
    }

    // ------------------------------------------------------------------------

    private void reportCounter(final DCounter counter) {
        send(counter.getMetric(), counter.getCounter().getCount(), counter.getTags());
    }

    private void reportGauge(final DGauge gauge) {
        Object value = gauge.getGauge().getValue();
        if (value == null) {
            return;
        }

        if (value instanceof Number) {
            send(numberIsNegative((Number) value), gauge.getMetric(), value.toString(), gauge.getTags());
        }

        send(gauge.getMetric(), value.toString(), gauge.getTags());
    }

    private void reportHistogram(final DHistogram histogram) {
        if (histogram != null) {

            HistogramStatistics statistics = histogram.getHistogram().getStatistics();

            if (statistics != null) {
                String name = histogram.getMetric();
                send(prefix(name, "count"), histogram.getHistogram().getCount(), histogram.getTags());
                send(prefix(name, "max"), statistics.getMax(), histogram.getTags());
                send(prefix(name, "min"), statistics.getMin(), histogram.getTags());
                send(prefix(name, "mean"), statistics.getMean(), histogram.getTags());
                send(prefix(name, "stddev"), statistics.getStdDev(), histogram.getTags());
                send(prefix(name, "p50"), statistics.getQuantile(0.5), histogram.getTags());
                send(prefix(name, "p75"), statistics.getQuantile(0.75), histogram.getTags());
                send(prefix(name, "p95"), statistics.getQuantile(0.95), histogram.getTags());
                send(prefix(name, "p98"), statistics.getQuantile(0.98), histogram.getTags());
                send(prefix(name, "p99"), statistics.getQuantile(0.99), histogram.getTags());
                send(prefix(name, "p999"), statistics.getQuantile(0.999), histogram.getTags());
            }
        }
    }

    private void reportMeter(final DMeter meter) {
        if (meter != null) {
            String name = meter.getMetric();
            send(prefix(name, "rate"), meter.getMeter().getRate(), meter.getTags());
            send(prefix(name, "count"), meter.getMeter().getCount(), meter.getTags());
        }
    }

    private String prefix(String... names) {
        if (names.length > 0) {
            StringBuilder stringBuilder = new StringBuilder(names[0]);

            for (int i = 1; i < names.length; i++) {
                stringBuilder.append('.').append(names[i]);
            }

            return stringBuilder.toString();
        } else {
            return "";
        }
    }

    private void send(String name, double value, List<String> tags) {
        send(numberIsNegative(value), name, String.valueOf(value), tags);
    }

    private void send(String name, long value, List<String> tags) {
        send(value < 0, name, String.valueOf(value), tags);
    }

    private void send(boolean resetToZero, String name, String value, List<String> tags) {
        if (resetToZero) {
            // negative values are interpreted as reductions instead of absolute values
            // reset value to 0 before applying reduction as a workaround
            send(name, "0", tags);
        }
        send(name, value, tags);
    }

    private void send(final String name, final String value, List<String> tags) {
        try {
            String tagsString = tags.isEmpty() ? "" : "|#" + String.join(",", tags);
            String formatted = String.format("%s:%s|g%s", name, value, tagsString);
            byte[] data = formatted.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, this.address));
        } catch (IOException e) {
            LOGGER.error("unable to send packet to statsd at '{}:{}'", address.getHostName(), address.getPort());
        }
    }

    @Override
    public String filterCharacters(String input) {
        char[] chars = null;
        final int strLen = input.length();
        int pos = 0;

        for (int i = 0; i < strLen; i++) {
            final char c = input.charAt(i);
            if (c == ':') {
                if (chars == null) {
                    chars = input.toCharArray();
                }
                chars[pos++] = '-';
            } else {
                if (chars != null) {
                    chars[pos] = c;
                }
                pos++;
            }
        }

        return chars == null ? input : new String(chars, 0, pos);
    }

    private boolean numberIsNegative(Number input) {
        return Double.compare(input.doubleValue(), 0) < 0;
    }

    /**
     * Get config tags from config 'metrics.metrics.dgstatsd.tags'.
     */
    private List<String> getTagsFromConfig(String str) {
        return str.isEmpty() ? new ArrayList<>() : Arrays.asList(str.split(","));
    }

    /**
     * Get tags from MetricGroup#getAllVariables().
     */
    private List<String> getTagsFromMetricGroup(MetricGroup metricGroup) {
        List<String> tags = new ArrayList<>();

        for (Map.Entry<String, String> entry : metricGroup.getAllVariables().entrySet()) {
            tags.add(getVariableName(entry.getKey()) + ":" + entry.getValue());
        }
        return tags;
    }

    /**
     * Removes leading and trailing angle brackets.
     */
    private String getVariableName(String str) {
        return str.substring(1, str.length() - 1);
    }
}
