export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

## Build update_task_for_config_change lambda
update_task_for_config_change-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/update_task_for_config_change:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test update_task_for_config_change lambda
update_task_for_config_change-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/update_task_for_config_change/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish update_task_for_config_change lambda
update_task_for_config_change-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "shared_infra/update_task_for_config_change/src" \
		--key="lambdas/shared_infra/update_task_for_config_change.zip" \
		--bucket="$(INFRA_BUCKET)";