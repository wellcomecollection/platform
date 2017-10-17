export ROOT = $(shell git rev-parse --show-toplevel)
export LAMBDA_UTILS = $(ROOT)/lambda_utils
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


lambda_utils-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(LAMBDA_UTILS):/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" \
		python3.6_ci:latest

lambda_utils-publish: