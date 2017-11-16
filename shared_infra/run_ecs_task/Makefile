export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build run_ecs_task lambda
run_ecs_task-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/run_ecs_task:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test run_ecs_task lambda
run_ecs_task-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/run_ecs_task/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish run_ecs_task lambda
run_ecs_task-publish:
	$(call publish_lambda,shared_infra/run_ecs_task)
