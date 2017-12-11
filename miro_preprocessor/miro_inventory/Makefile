ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build miro_inventory lambda
miro_inventory-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(ROOT)/miro_preprocessor/miro_inventory:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test miro_inventory lambda
miro_inventory-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/miro_inventory/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish miro_inventory lambda
miro_inventory-publish:
	$(call publish_lambda,miro_preprocessor/miro_inventory)
