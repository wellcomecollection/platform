#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace


echo "Substitute environment variables into conf/gremlin-server/dynamodb*.properties.template"

ls /template/dynamodb*.properties.template | xargs cat | envsubst > /janusgraph-0.2.0-hadoop2/conf/gremlin-server/dynamodb.properties

echo "Config: /janusgraph-0.2.0-hadoop2/conf/gremlin-server/dynamodb.properties ==="
cat /janusgraph-0.2.0-hadoop2/conf/gremlin-server/dynamodb.properties

/janusgraph-0.2.0-hadoop2/bin/gremlin-server.sh