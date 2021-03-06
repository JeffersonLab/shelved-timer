#!/bin/bash

echo "------------------------------------------------------"
echo "Step 1: Waiting for Schema Registry to start listening"
echo "------------------------------------------------------"
url=$SCHEMA_REGISTRY
echo "waiting on: $url"
while [ $(curl -s -o /dev/null -w %{http_code} $url/subjects/overridden-alarms-value/versions) -ne 200 ] ; do
  echo -e $(date) " Kafka Registry listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} $url/subjects/overridden-alarms-value/versions) " (waiting for 200)"
  sleep 5
done

export JAWS_AUTO_OVERRIDE_PROCESSOR_OPTS=-Dlog.dir=/opt/jaws-auto-override-processor/logs
/opt/jaws-auto-override-processor/bin/jaws-auto-override-processor
