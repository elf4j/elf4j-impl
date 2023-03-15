[![](https://img.shields.io/static/v1?label=github&message=repo&color=blue)](https://github.com/elf4j/elf4j-impl)

# elf4j-impl

A native logging service provider implementation of [ELF4J](https://github.com/elf4j/elf4j) (Easy Logging Facade for
Java)

* This is simply a packaging unit of the [elf4j-impl-core](https://github.com/elf4j/elf4j-impl-core) logging library,
  using the Java [Service Provider Framework](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
  mechanism. See [elf4j-impl-core](https://github.com/elf4j/elf4j-impl-core) for implementation detail.

## User Story

As an application developer using the ELF4J logging service API, I want to select a logging service provider that
natively implements the ELF4J API, so that I can opt to use it at application deploy time, without code change, as with
any other ELF4J logging service providers.

## Prerequisite

Java 8 or better

## Implementation Notes

* Guiding principle: Reasonable default and Pareto's 80/20 rule

## Features

* Async Logging Only

  Logging output is always asynchronous, considering performance and moreover the 80/20 rule: When was the last time a
  use case truly required that logging had to be synchronous, and always blocking the application's normal work flow?

* Standard Streams Output Only

  Other than the standard streams (stdout/stderr), it may be trivial for logging to support other output channels. Yet
  on the system level, it's arguably more trivial to forward standard streams as a data source to other destinations
  than the Console, e.g. log files and various types of central repositories. A recent trend has been that such data
  aggregation be done via system-level collector agents (open source or commercial), and no longer a concern of the
  application-level logging.

* Logging Format Patterns Including JSON

  JSON is a supported output pattern, in hopes of helping external log analysis tools. This is in addition to the usual
  line-based patterns - timestamp, level, thread, class, method, file name, line number, and log message. The JSON
  pattern can either be the only output of the log entry, or mixed together with other patterns.

* Configuration Refresh at Runtime

  Supports configuration refresh during runtime via API, with option of passing in overriding properties in addition to
  reloading the configuration file. The most frequent use case would be to change the minimum log output level, without
  restarting the application.

## Get It...

[![Maven Central](https://img.shields.io/maven-central/v/io.github.elf4j/elf4j-impl.svg?label=Maven%20Central)](https://central.sonatype.com/search?smo=true&q=pkg%253Amaven%252Fio.github.elf4j%252Felf4j-impl)

## Use It...

As with any other [ELF4J](https://github.com/elf4j/elf4j) logging provider, client application should code
against [service API](https://github.com/elf4j/elf4j#service-interface-and-access-api) of the ELF4J facade, and include
this module as runtime-scope dependency in Maven or other build tools alike:

```html
...
<dependency>
    <groupId>io.github.elf4j</groupId>
    <artifactId>elf4j</artifactId>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>io.github.elf4j</groupId>
    <artifactId>elf4j-impl</artifactId>
    <scope>runtime</scope>
</dependency>
...
```

### Usage Sample

See the ELF4J api [usage sample](https://github.com/elf4j/elf4j#for-logging-service-api-users).

### Configuration

* Properties File Configuration Only

  The default configuration file location is at the root of the application class path, with file
  name `elf4j-test.properties`, or if that is missing, `elf4j.properties`. Alternatively, to override the default
  location, use Java system property to provide an absolute path:

  ```
  java -Delf4j.properties.location=/absoloute/path/to/myConfigurationFile -jar MyApplicaiton.jar
  ``` 

  Absence of a configuration file results in no logging (no-op) at runtime. When present, the configuration file
  requires zero/no configuration thus can be empty - the default configuration is a stdout writer with a minimum level
  of `TRACE` and a basic line-based logging pattern. To customize the default logging configuration, see the
  configuration sample file below.

* Level

  The default global logger minimum output level is `TRACE`. Supports global and package level customizations.

* Writer

  Supports multiple console writers. Each writer can have individual configurations on output level and format pattern.
  However, with the comprehensive configuration support on a single writer's pattern and levels per various loggers,
  more than one console writer is rarely necessary.

* Output Format Pattern
    * timestamp: Format configurable per Java
      `DateTimeFormatter` [pattern syntax](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#patterns),
      default to ISO local datetime
    * level: Length configurable, default to full length
    * thread: Option of thread name or id, default to name
    * class: Option of simple, full, or compressed (only the first letter for a package segment), default to full
    * method: Not configurable, simple method name
    * file name: Not configurable, simple file name
    * line number: Not configurable, where the log is issued in the file
    * log message: Not configurable, always prints user message, and exception stack trace if any
    * json: Options to include thread and caller (method, line number, file name) details and minify the JSON string,
      default to no thread/caller detail and pretty print format

* Output samples
    * Line-based (default pattern: {timestamp} {level} [{thread}] {class} - {message})
      ```
      2023-03-14T21:21:33.118-05:00 INFO [main] elf4j.impl.core.IntegrationTest$defaultLogger - Hello, world!
      ```
    * JSON (default one-line, no thread or caller detail: {json})
      ```json
      {"timestamp":"2023-03-14T21:21:33.1180212-05:00","level":"INFO","callerClass":"elf4j.impl.core.IntegrationTest$defaultLogger","message":"Hello, world!"}
      ```
    * JSON (pretty print, with thread and caller detail: {json:caller-thread,caller-detail,pretty})
      ```json
      {
        "timestamp": "2023-03-14T21:21:33.1180212-05:00",
        "level": "INFO",
        "callerThread": {
          "name": "main",
          "id": 1
        },
        "callerDetail": {
          "className": "elf4j.impl.core.IntegrationTest$defaultLogger",
          "methodName": "hey",
          "lineNumber": 41,
          "fileName": "IntegrationTest.java"
        },
        "message": "Hello, world!"
      }
      ```

* Sample Configuration File
    * When in doubt, use lower-case.

  ```properties
  ### If noop is set to true, it takes precedence and no logging will be performed
  #noop=true
  ### Any level is optional, default to TRACE if omitted
  ### This override the default global level
  level=info
  ### Global console output stream type, default to stdout; cannot differ per individual writers
  #console.out.stream=stderr
  ### These override level of all caller classes included the specified package
  #level@elf4j.impl=error
  #level@org.springframework=warn
  ### Any writer is optional, default to a single console writer if no writer configured
  writer1=console
  ### This is the default output pattern, can be omitted
  writer1.pattern={timestamp} {level} [{thread}] {class} - {message}
  ### This would customize the format patterns of the specified writer
  #writer1.pattern={timestamp:yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ} {level:5} [{thread:name|id}] {class:simple|full|compressed}: {message}
  ### Multiple writers are supported, each with its own configurations
  writer2=console
  writer2.level=trace
  ### Default json pattern does not include thread and caller details, and uses one-line minified JSON string
  #writer2.pattern={json}
  ### This would force the JSON to include the thread/caller details, and use pretty print format
  writer2.pattern={json:caller-thread,caller-detail,pretty}
  ```