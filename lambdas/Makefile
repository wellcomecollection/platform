export ROOT = $(shell git rev-parse --show-toplevel)
export LAMBDAS = $(ROOT)/lambdas
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

## Build dynamo_event_to_sns lambda
dynamo_event_to_sns-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(LAMBDAS)/dynamo_event_to_sns:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test dynamo_event_to_sns lambda
dynamo_event_to_sns-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(LAMBDAS)/dynamo_event_to_sns/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish dynamo_event_to_sns lambda
dynamo_event_to_sns-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "lambdas/dynamo_event_to_sns/src" \
		--key="lambdas/dynamo_event_to_sns.zip" \
		--bucket="$(INFRA_BUCKET)";