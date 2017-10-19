ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

## Build miro_copy_s3_asset lambda
miro_copy_s3_asset-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(ROOT)/miro_preprocessor/miro_copy_s3_asset:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test miro_copy_s3_asset lambda
miro_copy_s3_asset-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/miro_copy_s3_asset/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish miro_copy_s3_asset lambda
miro_copy_s3_asset-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "shared_infra/service_scheduler/src" \
		--key="lambdas/miro_preprocessor/miro_copy_s3_asset.zip" \
		--bucket="$(INFRA_BUCKET)";