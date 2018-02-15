#!/usr/bin/env bash

set -o errexit
set -o nounset

tag=$1

cp /data/run_gremlin_server.sh /build/.
cp /data/janusgraph-dynamodb.Dockerfile /build/.

cp /data/dynamodb*.properties.template /template/
cp /data/gremlin-server.yaml /build/conf/gremlin-server/

if [ $tag == "with-endpoint" ] ; then
    echo "copying endpoint config file"
    cp /template/dynamodb-endpoint.properties.template /build/template/dynamodb.properties.template
else
    echo "copying aws credentials config file"
    cp /template/dynamodb.properties.template /build/template/dynamodb.properties.template
fi;

cd /build

docker build -f janusgraph-dynamodb.Dockerfile -t wellcome/janusgraph-dynamodb:$tag --build-arg tag=$tag .