ROOT = $(shell git rev-parse --show-toplevel)
LOAD_TEST = $(ROOT)/monitoring/load_test

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build docker image for gatling
gatling-build:
	$(call build_image,gatling,monitoring/load_test/gatling/Dockerfile)

## Deploy docker image for gatling to ECR
gatling-deploy: gatling-build
	$(call publish_service,gatling)

## Build gatling_to_cloudwatch lambda
gatling_to_cloudwatch-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(LOAD_TEST)/gatling_to_cloudwatch:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test gatling_to_cloudwatch lambda
gatling_to_cloudwatch-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(LOAD_TEST)/gatling_to_cloudwatch/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish gatling_to_cloudwatch lambda
gatling_to_cloudwatch-publish:
	$(call publish_lambda,monitoring/load_test/gatling_to_cloudwatch)

## Build all for load_test
load_test-build: gatling-build gatling_to_cloudwatch-build

## Test all for load_test
load_test-test: gatling_to_cloudwatch-test

## Publish all for load_test
load_test-deploy: gatling-deploy gatling_to_cloudwatch-publish

.PHONY:	gatling-build gatling-deploy gatling_to_cloudwatch-build gatling_to_cloudwatch-test gatling_to_cloudwatch-publish\
		load_test-build load_test-test load_test-deploy
