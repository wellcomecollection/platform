export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build update_dynamo_capacity lambda
update_dynamo_capacity-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/update_dynamo_capacity:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test update_dynamo_capacity lambda
update_dynamo_capacity-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/update_dynamo_capacity/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish update_dynamo_capacity lambda
update_dynamo_capacity-publish:
	$(call publish_lambda,shared_infra/update_dynamo_capacity)
