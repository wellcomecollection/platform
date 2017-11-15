export ROOT = $(shell git rev-parse --show-toplevel)
export CATALOGUE_PIPELINE = $(ROOT)/catalogue_pipeline
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build schedule_reindexer lambda
schedule_reindexer-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(CATALOGUE_PIPELINE)/schedule_reindexer:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test schedule_reindexer lambda
schedule_reindexer-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(CATALOGUE_PIPELINE)/schedule_reindexer/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish schedule_reindexer lambda
schedule_reindexer-publish:
	$(call publish_lambda,catalogue_pipeline,schedule_reindexer)
