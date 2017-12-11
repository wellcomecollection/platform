#!/usr/bin/env bash

envsubst < /opt/docker/conf/application.ini.template > /opt/docker/conf/application.ini

/opt/docker/bin/blergh