export ROOT = $(shell git rev-parse --show-toplevel)
export LAMBDA_UTILS = $(ROOT)/lambda_utils
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

export PUBLISH_VERSION := $(shell curl "https://pypi.python.org/pypi/wellcome-lambda-utils/json" 2>/dev/null | jq -r '.info.version')
export SETUP_VERSION := $(shell grep 'version' $(LAMBDA_UTILS)/setup.py | tr "'" ' ' | awk '{print $$2}')

lambda_utils-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(LAMBDA_UTILS):/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" \
		python3.6_ci:latest

lambda_utils-build:
	docker run \
		--volume $(LAMBDA_UTILS):/src \
		--workdir /src \
		python:3-alpine python setup.py sdist

lambda_utils-publish: lambda_utils-build
	$(LAMBDA_UTILS)/publish.sh


.PHONY: lambda_utils-test lambda_utils-build lambda_utils-publish
