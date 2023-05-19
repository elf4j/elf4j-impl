[![Maven Central](https://img.shields.io/maven-central/v/io.github.elf4j/elf4j-provider.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.elf4j%22%20AND%20a:%22elf4j-provider%22)

# elf4j-provider

The native logging _service provider_ implementation of [ELF4J](https://github.com/elf4j/elf4j) (Easy Logging Facade for
Java), and an independent drop-in logging solution for any Java application

## User story

As an application developer using the ELF4J logging facade, I want to have the option of using a runtime log _service
provider_ that natively implements
the [API and SPI](https://github.com/elf4j/elf4j#log-service-interface-and-access-api) of ELF4J.

## Prerequisite

Java 8 or better

## Implementation notes

* Guiding principle: Reasonable default and Pareto's 80/20 rule

* This is simply a packaging unit of the [elf4j-engine](https://github.com/elf4j/elf4j-engine), using the
  Java [Service Provider Framework](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) mechanism.
  See [elf4j-engine](https://github.com/elf4j/elf4j-engine) for implementation details.

## Installation

[![Maven Central](https://img.shields.io/maven-central/v/io.github.elf4j/elf4j-provider.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.elf4j%22%20AND%20a:%22elf4j-provider%22)

```html
...
<dependency>
    <groupId>io.github.elf4j</groupId>
    <artifactId>elf4j</artifactId>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>io.github.elf4j</groupId>
    <artifactId>elf4j-provider</artifactId>
    <scope>runtime</scope>
</dependency>
...
```

## Features

### Async logging only

Logging output is always asynchronous, considering performance and moreover the 80/20 rule: When was the last time a use
case truly required that logging had to be synchronous, and always blocking the application's normal work flow?

### Standard streams output only

Besides the standard streams (stdout/stderr), it may be trivial for the application logging to support other output
channels. Yet it's arguably more trivial for the hosting system to redirect/forward standard streams as a data source to
other destinations than the system Console, e.g. log files and/or other central repositories. Such log data aggregation
process may be as simple as a Linux shell redirect, or as sophisticated as running collector agents of comprehensive
monitoring/observability services, but no longer a concern of the application-level logging.

### Log patterns including JSON

JSON is a supported output pattern, in hopes of helping external log analysis tools. This is in addition to the usual
line-based patterns - timestamp, level, thread, class, method, file name, line number, and log message.

### Service administration

* Supports configuration refresh during runtime via API, with option of passing in replacement properties instead of
  reloading the configuration file. The most frequent use case would be to change the minimum log output level, without
  restarting the application.
* To avoid loss of logs when the application shuts down, it is the user's responsibility to
  call `LogServiceManager.stop` before the application exits. Upon that call, the log service will
    1. stop accepting new log events
    2. block and wait for all the accepted log events to finish processing

  Alternatively, the user can register a JVM shutdown hook using the thread returned
  by `LogServiceManager.getShutdownHookThread`.

## Usage

* As with any other [ELF4J](https://github.com/elf4j/elf4j) logging provider, client application should code
  against [service API](https://github.com/elf4j/elf4j#service-interface-and-access-api) of the ELF4J facade, and drop
  in this provider implementation as a runtime dependency shown in the "Installation" section.

* See ELF4J for [API sample usage](https://github.com/elf4j/elf4j#use-it---for-log-service-api-clients).

* In case of multiple ELF4J service providers in classpath, pick this one like so:
  ```
  java -Delf4j.logger.factory.fqcn="elf4j.engine.NativeLoggerFactory" MyApplication
  ```  
  More details [here](https://github.com/elf4j/elf4j/blob/main/README.md#only-one-in-effect-logging-provider).

## Configuration

### Properties file

The default configuration file location is at the root of the application class path, with file
name `elf4j-test.properties`, or if that is missing, `elf4j.properties`. Alternatively, to override the default
location, use Java system property to provide an absolute path:

```
java -Delf4j.properties.location=/absoloute/path/to/myConfigurationFile -jar MyApplicaiton.jar
``` 

Absence of a configuration file results in no logging (no-op) at runtime. When present, though, the configuration file
requires zero/no configuration thus can be empty: the default configuration is a stdout writer with a minimum level
of `TRACE` and a basic line-based logging pattern. To customize the default logging configuration, see the
configuration sample file below.

### Level

A log output is rendered only when the `Logger` instance severity level is on or above the minimum output levels for
both the log caller class and the log writer.

* The default severity level of a `Logger` instance from `Logger.instance()` is `INFO`, which is not configurable: The
  elf4j [API](https://github.com/elf4j/elf4j#logging-service-interface-and-access-api) should be used to
  programmatically obtain `Logger` instances of desired severity levels.
* The default minimum output level for both log caller classes and writers and is `TRACE`, which is configurable: For
  caller classes, the minimum output level can be configured on global, package, or individual class levels. For
  writers, the level can be configured on global level and overridden per each writer.

### Writer

The elf4j-engine supports multiple standard-stream writers. Each writer can have individual configurations on format
pattern and minimum output level. The same log entry will be output once per each writer. Practically, however, more
than one writer is rarely necessary given what a single writer can achieve with the comprehensive support on log
patterns and minimum output levels per caller classes.

### Output format pattern

All individual patterns, including the JSON pattern, can either be the only output of the log entry, or mixed together
with any other patterns. They take the form of `{pattern:displayOptions}`, where multiple display options are separated
by commas. Patterns inside curly brace pairs are predefined and will be interpreted before output, while patterns
outside curly brace pairs are output verbatim. The predefined patterns are:

* `timestamp`: Date time format configurable via Java
  `DateTimeFormatter` [pattern syntax](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#patterns),
  default to ISO datetime format with time zone offset of the application running host
* `level`: Length configurable, default to full length
* `thread`: Option of `name` or `id`, default to name
* `class`: Option of `simple`, `full`, or `compressed` (only the first letter for each package segment) for class names,
  default to `simple`
* `method`: No configuration options, simple method name
* `filename`: No configuration options, simple file name
* `linenumber`: No configuration options, the line number where the log is issued in the file
* `sysprop`: Option `name` for the JRE System Property
* `sysenv`: Option `name` for the system environment variable
* `message`: No configuration options, always prints user message, and exception stack trace if any
* `json`: Multiple options allowed - `caller-thread` to include caller thread (name and id), `caller-detail` to include
  caller stack detail (class, method, filename, linenumber), and option `pretty` to indent the JSON text to more
  readable format. Default is no thread/caller detail and the minified single-line format

At the end of each complete log entry output, a system-dependent line feed character is appended automatically; this is
not configurable.

### Output stream types

Either stdout (the default if omitted) or stderr, configured globally.

#### Pattern output samples

Line-based Default

* Pattern: none, which is the same as

  ```
  {timestamp} {level} {class} - {message}
  ```

* Output:

  ```
  2023-03-22T21:11:33.040-05:00 INFO IntegrationTest$defaultLogger - Hello, world!
  ```

Line-based Customized

* Pattern:
  ```
  {timestamp:yyyy-MM-dd'T'HH:mm:ss.SSSXXX} {level:5} [{thread:name}] {class:compressed}#{method}(L{linenumber}@{filename}) -- {message}
  ```
* Output:
  ```
  2023-03-30T17:14:54.735-05:00 INFO  [main] e.e.IntegrationTest$defaultLogger#hey(L50@IntegrationTest.java) -- Hello, world!
  ```

JSON Default

* Pattern:
  ```
  {json}
  ```
* Output:
  ```
  {"timestamp":"2023-03-22T21:21:29.7319284-05:00","level":"INFO","callerClass":"elf4j.engine.IntegrationTest$defaultLogger","message":"Hello, world!"}
  ```

JSON Customized

* Pattern:
  ```
  {json:caller-thread,caller-detail,pretty}
  ```
* Output:
  ```json
  {
    "timestamp": "2023-03-14T21:21:33.1180212-05:00",
    "level": "INFO",
    "callerThread": {
      "name": "main",
      "id": 1
    },
    "callerDetail": {
      "className": "elf4j.provider.IntegrationTest$defaultLogger",
      "methodName": "hey",
      "lineNumber": 41,
      "fileName": "IntegrationTest.java"
    },
    "message": "Hello, world!"
  }
  ```

#### Sample configuration file

```properties
### Zero configuration mandatory, this file can be empty - default to a line-based writer with simple log pattern
### global no-op flag, overriding and will turn off all logging if set true
#noop=true
### Minimum output level is optional, default to TRACE for all caller classes if omitted
level=info
### These override the output level of all caller classes included the specified package spaces
level@org.springframework=warn
level@org.apache=error
### Standard out stream type, stdout or stderr, default is stdout
stream=stderr
### Global writer output pattern if omitted on individual writer, default to a simple line based
#pattern={json}
### Any writer is optional, default to a simple standard streams writer
### 'standard' is currently the only supported writer type
writer1=standard
### This is the default output pattern, can be omitted
#writer1.pattern={timestamp} {level} {class} - {message}
### This would customize the format patterns of the specified writer
#writer1.pattern={timestamp:yyyy-MM-dd'T'HH:mm:ss.SSSXXX} {level:5} [{thread:id}] {class:compressed}#{method}(L{linenumber}@{filename}) - {message}
### Multiple writers are supported, each with its own configurations
writer2=standard
#writer2.level=trace
### Default json pattern does not include thread and caller details, and uses minified one-line format for the JSON string
#writer2.pattern={json}
### This would force the JSON to include the thread/caller details, and pretty print
writer2.pattern={json:caller-thread,caller-detail,pretty}
### Optional buffer - log event processor work queue capacity, default to "unlimited" log events (as hydrated in-memory objects)
#buffer=2048
### Optional log event processing concurrency, default is jvm runtime available processors at application start time
#concurrency=20
```

### Configuration refresh

`LogServiceManager.refresh()` will reload the configuration file and apply the latest file properties during
runtime. `LogServiceManager.refresh(java.util.Properties)` will apply the passed-in properties as the replacement of the
current properties, and the configuration file will be ignored.

## Performance

It's not how fast you fill up the target log file or repository, it's how fast you relieve the application from logging
duty back to its own business.

Performance benchmark metrics tend to be highly dependent on the nature of the work load and overall setup, but here is
a naive start (Recommend re-configuration based on individual use cases), comparing with some popular log engines:

* [elf4j-benchmark](https://github.com/elf4j/elf4j-benchmark)

Chronological order is generally required for log events to arrive at their final destination. The usual destination of
the standard out streams is the system Console, where an out-of-order display would be strange and confusing. That means
log events need to be processed [sequentially](https://github.com/q3769/conseq4j#concurrency-and-sequencing) - at least
for those events that are issued from the same application/caller thread. This inevitably imposes some limit on the log
processing throughput. No matter the log processing is synchronous or asynchronous to the main business workflow, if the
application's log issuing frequency is higher than the throughput of the log processing, then over time, the main
workflow should be blocked and bound ("back-pressured") by the log processing throughput limit.

Some logging information has to be collected by the main application thread, synchronously to the business workflow. For
example, caller detail information such as method name, line number, or file name are performance-wise expensive to
retrieve, yet unavailable for a different/asynchronous thread to look up. The elf4j-engine uses the main caller thread
to synchronously collect all required information into a log event, and then hands off the event to an asynchronous
process for the rest of the work. It helps, however, if the client application can do without performance-sensitive
information in the first place; the default log pattern configuration does not include such caller details.

The ideal situation of asynchronous logging is for the main application to "fire and forget" the log events, and
continue its main business flow without further blocking, in which case the log processing throughput has little impact
on the main application. That only happens, however, when the work queue hosting the asynchronous log events (a.k.a. the
asynchronous buffer) is not full between the main application and the asynchronous log process. When the asynchronous
buffer is full ("overloaded"), the log process becomes pseudo-synchronous, in which case not only will the main
application be back-pressured while awaiting available buffer capacity, but also the extra cost of facilitating
asynchronous communications will now add to that of the main workflow. By contrast, a true synchronous logging without
buffering will delay the main workflow in each transaction, albeit having no additional cost for asynchrony.

For asynchronous logging to work well, the log processing throughput should, over time, exceed the log event generation
rate; the work queue hosting the log events should serve as temporary buffer when the log eventing rate is momentarily
higher than the log processing throughput.

The elf4j-engine has a buffer that, on the one end, takes in log events from the main application and, on the other end,
hands off the log events to an asynchronous processor. Leveraging
the [conseq4j](https://github.com/q3769/conseq4j#style-2-submit-each-task-directly-for-execution-together-with-its-sequence-key)
concurrent API, the elf4j-engine processes log events issued by different caller threads in parallel, and those by the
same caller threads in sequence. This ensures all logs of the same caller thread arrives at the log destination in
chronological order (same order as they are issued by the thread). However, logs from different caller threads
are [not guaranteed](https://github.com/q3769/conseq4j#concurrency-and-sequencing) of any particular order of arrival.

If omitted in configuration file:

* The default buffer capacity is "unlimited" log events (as hydrated in-memory objects). This assumes the log processing
  throughput is in general higher than the log issuing rate of the application. Note that even an executor with
  "unlimited" buffer can reject tasks at runtime. In case of task rejections, the el4j-engine's handling policy is that
  the caller thread will block and retry the task until it is accepted. This temporarily imposes back-pressure to the
  caller application.
* The default concurrency (maximum number of threads in parallel) for asynchronous processing is the number of
  [Runtime#availableProcessors](https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#availableProcessors--)
  of the current JVM at the application startup time (or when the log service is refreshed). This is the thread pool
  capacity for log event processing.

The standard stream destinations are often redirected/replaced by the host environment or user, in which case further
manoeuvres may help such data channel's performance. For example, if the target repository is a log file on disk, then

```shell
java MyApplication | cat >logFile
```

may outperform

```shell
java MyApplication >logFile
```

due to the buffering effect of piping and `cat`.

Such external setups fall into the category of increasing channel bandwidth, and are considered outside the scope of
application-level logging.
