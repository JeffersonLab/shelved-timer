# jaws-auto-override-processor [![Java CI with Gradle](https://github.com/JeffersonLab/jaws-auto-override-processor/workflows/Java%20CI%20with%20Gradle/badge.svg)](https://github.com/JeffersonLab/jaws-auto-override-processor/actions?query=workflow%3A%22Java+CI+with+Gradle%22) [![Docker](https://img.shields.io/docker/v/slominskir/jaws-auto-override-processor?sort=semver&label=DockerHub)](https://hub.docker.com/r/slominskir/jaws-auto-override-processor)
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to handle automated overrides in [JAWS](https://github.com/JeffersonLab/jaws).  Automated overrides include:
- **shelve expiration** - Remove Shelved override with an expiration timer
- **one-shot shelve** - Remove Shelved override when alarm is no longer active for overrides configured as one-shot
- **on-delay** - Add an OnDelayed override for alarms registered as on-delayed and removes the OnDelay after expiration
- **off-delay** - Add an OffDelayed override for alarms registered as off-delayed and remove the OffDelay after expiration
- **latch** - Add a Latched override for alarms registered as latching that become active
- **mask** - Add a Masked override to an alarm with an active parent alarm and removes the Masked override when the parent alarm is no longer active

---
 - [Quick Start with Compose](https://github.com/JeffersonLab/jaws-auto-override-processor#quick-start-with-compose)
 - [Build](https://github.com/JeffersonLab/jaws-auto-override-processor#build)
 - [Configure](https://github.com/JeffersonLab/jaws-auto-override-processor#configure)
 - [Deploy](https://github.com/JeffersonLab/jaws-auto-override-processor#deploy)
 - [Docker](https://github.com/JeffersonLab/jaws-auto-override-processor#docker)
 - [See Also](https://github.com/JeffersonLab/jaws-auto-override-processor#see-also)
 ---

## Quick Start with Compose 
1. Grab project
```
git clone https://github.com/JeffersonLab/jaws-auto-override-processor
cd jaws-auto-override-processor
```
2. Launch Docker
```
docker-compose up
```
3. Monitor for expiration tombstone message 
```
docker exec -it jaws /scripts/client/list-overridden.py --monitor 
```
4. Shelve an alarm for 5 seconds
```
docker exec -it jaws /scripts/client/set-overridden.py --override Shelved alarm1 --reason Other --expirationseconds 5
```

More [Examples](https://github.com/JeffersonLab/jaws-auto-override-processor/wiki/Examples)

## Build
This [Java 11](https://adoptopenjdk.net/) project uses the [Gradle 6](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/jaws-auto-override-processor
cd jaws-auto-override-processor
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

**Note**: When developing the app you can mount the build artifact into the container by substituting the `docker-compose up` command with:
```
docker-compose -f docker-compose.yml -f docker-compose-dev.yml up
```

## Configure
Environment Variables

| Name | Description |
|---|---|
| BOOTSTRAP_SERVERS | Comma-separated list of host and port pairs pointing to a Kafka server to bootstrap the client connection to a Kafka Cluser; example: `kafka:9092` |
| SCHEMA_REGISTRY | URL to Confluent Schema Registry; example: `http://registry:8081` |

## Deploy
The Kafka Streams app is a regular Java application, and start scripts are created and dependencies collected by the Gradle distribution targets:

```
gradlew assembleDist
```

[Releases](https://github.com/JeffersonLab/jaws-auto-override-processor/releases)

Launch with:

UNIX:
```
bin/jaws-auto-override-processor
```
Windows:
```
bin/jaws-auto-override-processor.bat
```

## Docker
```
docker pull slominskir/jaws-auto-override-processor
```
Image hosted on [DockerHub](https://hub.docker.com/r/slominskir/jaws-auto-override-processor)

## See Also
   - [Developer Notes](https://github.com/JeffersonLab/jaws-auto-override-processor/wiki/Developer-Notes)
