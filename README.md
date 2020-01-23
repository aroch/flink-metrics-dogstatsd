# flink-metrics-dogstatsd
A metrics reporter implementation for the DogStatsd protocol:
https://docs.datadoghq.com/developers/dogstatsd/datagram_shell?tab=metrics

Note any variables in Flink metrics, such as <host>, <job_name>, <tm_id>, <subtask_index>, <task_name>, and <operator_name>, will be sent to Datadog as tags. Tags will look like host:localhost and job_name:myjobname
## Build
```
./gradlew clean build
```

## Install
In order to use this reporter you must copy /opt/flink-metrics-dogstatsd-X.X.X.jar into the /lib folder of your Flink distribution.

## Parameters:

- host - the StatsD server host.
- port - the StatsD server port.
- tags - (optional) the global tags that will be applied to metrics when sending to Datadog. Tags should be separated by comma only. 

## Example configuration:
```
metrics.reporter.dgstsd.class: com.aroch.flink.metrics.dogstatsd.DogStatsDReporter
metrics.reporter.dgstsd.host: localhost
metrics.reporter.dgstsd.port: 8125
metrics.reporter.dgstsd.tags: env:DEV,service:foo
```