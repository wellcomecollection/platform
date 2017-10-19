ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

## Build miro_image_to_dynamo lambda
miro_image_to_dynamo-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(ROOT)/miro_preprocessor/miro_image_to_dynamo:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test miro_image_to_dynamo lambda
miro_image_to_dynamo-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/miro_image_to_dynamo/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish miro_image_to_dynamo lambda
miro_image_to_dynamo-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "miro_preprocessor/miro_image_to_dynamo/src" \
		--key="lambdas/miro_preprocessor/miro_image_to_dynamo.zip" \
		--bucket="$(INFRA_BUCKET)";