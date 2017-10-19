ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

## Build xml_to_json_run_task lambda
xml_to_json_run_task-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(ROOT)/miro_preprocessor/xml_to_json_run_task:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test xml_to_json_run_task lambda
xml_to_json_run_task-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/xml_to_json_run_task/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish xml_to_json_run_task lambda
xml_to_json_run_task-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "miro_preprocessor/xml_to_json_run_task/src" \
		--key="lambdas/miro_preprocessor/xml_to_json_run_task.zip" \
		--bucket="$(INFRA_BUCKET)";