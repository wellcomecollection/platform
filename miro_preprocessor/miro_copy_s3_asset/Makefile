ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

miro_copy_s3_derivative_asset-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(ROOT)/miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_derivative_asset/src:/data \
	-e OP=test \
	python3.6_ci:latest

miro_copy_s3_master_asset-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(ROOT)/miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_master_asset/src:/data \
	-e OP=test \
	python3.6_ci:latest

miro_copy_s3_derivative_asset-build: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(ROOT)/miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_derivative_asset:/data \
	-e OP=build-lambda \
	python3.6_ci:latest

miro_copy_s3_master_asset-build: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(ROOT)/miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_master_asset:/data \
	-e OP=build-lambda \
	python3.6_ci:latest

## Build miro_copy_s3_asset lambda
miro_copy_s3_asset-build: miro_copy_s3_master_asset-build miro_copy_s3_derivative_asset-build


## Test miro_copy_s3_asset lambda
miro_copy_s3_asset-test: miro_copy_s3_derivative_asset-test miro_copy_s3_master_asset-test

## Publish miro_copy_s3_asset lambda
miro_copy_s3_asset-publish: $(ROOT)/.docker/publish_lambda_zip
	for l in miro_copy_s3_asset/miro_copy_s3_derivative_asset miro_copy_s3_asset/miro_copy_s3_master_asset; \
    	do																		\
    		$(ROOT)/builds/docker_run.py --aws -- 								\
    			--volume $(ROOT):/repo 											\
    			publish_lambda_zip 												\
    			"miro_preprocessor/$$l/src" --key="lambdas/miro_preprocessor/$$l.zip" --bucket="$(INFRA_BUCKET)"; \
    	done