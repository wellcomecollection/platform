ROOT = $(shell git rev-parse --show-toplevel)

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Test miro_image_to_dynamo lambda
miro_image_to_dynamo-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/miro_image_to_dynamo/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish miro_image_to_dynamo lambda
miro_image_to_dynamo-publish:
	$(call publish_lambda,miro_preprocessor/miro_image_to_dynamo)
