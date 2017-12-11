export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build dynamo_to_sns lambda
dynamo_to_sns-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/dynamo_to_sns:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test dynamo_to_sns lambda
dynamo_to_sns-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/dynamo_to_sns/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish dynamo_to_sns lambda
dynamo_to_sns-publish:
	$(call publish_lambda,shared_infra/dynamo_to_sns)
