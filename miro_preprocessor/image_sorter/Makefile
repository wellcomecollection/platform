ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build image_sorter lambda
image_sorter-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(ROOT)/miro_preprocessor/image_sorter:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test image_sorter lambda
image_sorter-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/image_sorter/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish image_sorter lambda
image_sorter-publish:
	$(call publish_lambda,miro_preprocessor/image_sorter)
