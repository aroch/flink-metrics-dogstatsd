# flink-metrics-dogstatsd
A metrics reporter implementation for the DogStatsd protocol:
https://docs.datadoghq.com/developers/dogstatsd/datagram_shell?tab=metrics

StatsD (org.apache.flink.metrics.statsd.StatsDReporter)
In order to use this reporter you must copy /opt/flink-metrics-statsd-1.9.0.jar into the /lib folder of your Flink distribution.

## Parameters:

- host - the StatsD server host.
- port - the StatsD server port.
- tags - (optional) the global tags that will be applied to metrics when sending to Datadog. Tags should be separated by comma only. 

## Example configuration:
```
metrics.reporter.dgstsd.class: org.apache.flink.metrics.dogstatsd.DogStatsDReporter
metrics.reporter.dgstsd.host: localhost
metrics.reporter.dgstsd.port: 8125
metrics.reporter.dgstsd.tags: env:DEV,service:foo
```