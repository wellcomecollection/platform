export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build drain_ecs_container_instance lambda
drain_ecs_container_instance-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/drain_ecs_container_instance:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test drain_ecs_container_instance lambda
drain_ecs_container_instance-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/drain_ecs_container_instance/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish drain_ecs_container_instance lambda
drain_ecs_container_instance-publish:
	$(call publish_lambda,shared_infra/drain_ecs_container_instance)
