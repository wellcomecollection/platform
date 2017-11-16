export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

## Build register_ecs_task lambda
register_ecs_task-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/register_ecs_task:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test register_ecs_task lambda
register_ecs_task-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/register_ecs_task/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish register_ecs_task lambda
register_ecs_task-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "shared_infra/run_ecs_task/src" \
		--key="lambdas/shared_infra/register_ecs_task.zip" \
		--bucket="$(INFRA_BUCKET)";