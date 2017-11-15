ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Test xml_to_json_run_task lambda
xml_to_json_run_task-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/xml_to_json_run_task/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish xml_to_json_run_task lambda
xml_to_json_run_task-publish:
	$(call publish_lambda,miro_preprocessor/xml_to_json_run_task)
