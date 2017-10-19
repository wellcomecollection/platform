export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

## Build update_ecs_service_size lambda
update_ecs_service_size-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/update_ecs_service_size:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test update_ecs_service_size lambda
update_ecs_service_size-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/update_ecs_service_size/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish update_ecs_service_size lambda
update_ecs_service_size-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "shared_infra/update_ecs_service_size/src" \
		--key="lambdas/shared_infra/update_ecs_service_size.zip" \
		--bucket="$(INFRA_BUCKET)";