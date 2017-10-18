export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

## Build schedule_reindexer lambda
schedule_reindexer-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/schedule_reindexer:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test schedule_reindexer lambda
schedule_reindexer-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/schedule_reindexer/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish schedule_reindexer lambda
schedule_reindexer-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "shared_infra/schedule_reindexer/src" \
		--key="lambdas/shared_infra/schedule_reindexer.zip" \
		--bucket="$(INFRA_BUCKET)";