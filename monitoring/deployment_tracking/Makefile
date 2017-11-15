ROOT = $(shell git rev-parse --show-toplevel)
DEPLOYMENT_TRACKING = $(ROOT)/monitoring/deployment_tracking
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build notify_old_deploys lambda
notify_old_deploys-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(DEPLOYMENT_TRACKING)/notify_old_deploys:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test notify_old_deploys lambda
notify_old_deploys-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(DEPLOYMENT_TRACKING)/notify_old_deploys/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish notify_old_deploys lambda
notify_old_deploys-publish:
	$(call publish_lambda,monitoring,deployment_tracking/notify_old_deploys)

## Build service_deployment_status lambda
service_deployment_status-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(DEPLOYMENT_TRACKING)/service_deployment_status:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test service_deployment_status lambda
service_deployment_status-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(DEPLOYMENT_TRACKING)/service_deployment_status/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish service_deployment_status lambda
service_deployment_status-publish:
	$(call publish_lambda,monitoring,deployment_tracking/service_deployment_status)

## Build all for deployment_tracking
deployment_tracking-build: notify_old_deploys-build service_deployment_status-build

## Test all for deployment_tracking
deployment_tracking-test: notify_old_deploys-test service_deployment_status-test

## Publish all for deployment_tracking
deployment_tracking-deploy: notify_old_deploys-publish service_deployment_status-publish

.PHONY: notify_old_deploys-build notify_old_deploys-test notify_old_deploys-publish \
		service_deployment_status-build service_deployment_status-test service_deployment_status-publish \
		deployment_tracking-build deployment_tracking-test deployment_tracking-deploy

