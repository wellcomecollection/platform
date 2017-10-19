export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

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
update_dynamo_capacity-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "shared_infra/update_dynamo_capacity/src" \
		--key="lambdas/shared_infra/update_dynamo_capacity.zip" \
		--bucket="$(INFRA_BUCKET)";