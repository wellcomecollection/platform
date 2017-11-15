ROOT = $(shell git rev-parse --show-toplevel)
DEPLOYMENT_TRACKING = $(ROOT)/monitoring/deployment_tracking
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Test notify_old_deploys lambda
notify_old_deploys-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(DEPLOYMENT_TRACKING)/notify_old_deploys/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish notify_old_deploys lambda
notify_old_deploys-publish:
	$(call publish_lambda,monitoring/deployment_tracking/notify_old_deploys)

## Test service_deployment_status lambda
service_deployment_status-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(DEPLOYMENT_TRACKING)/service_deployment_status/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish service_deployment_status lambda
service_deployment_status-publish:
	$(call publish_lambda,monitoring/deployment_tracking/service_deployment_status)

## Test all for deployment_tracking
deployment_tracking-test: notify_old_deploys-test service_deployment_status-test

## Publish all for deployment_tracking
deployment_tracking-deploy: notify_old_deploys-publish service_deployment_status-publish

.PHONY: notify_old_deploys-test notify_old_deploys-publish \
		service_deployment_status-test service_deployment_status-publish \
		deployment_tracking-test deployment_tracking-deploy

