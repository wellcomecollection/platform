export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build ecs_ec2_instance_tagger lambda
ecs_ec2_instance_tagger-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(SHARED_INFRA)/ecs_ec2_instance_tagger:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test ecs_ec2_instance_tagger lambda
ecs_ec2_instance_tagger-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(SHARED_INFRA)/ecs_ec2_instance_tagger/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish ecs_ec2_instance_tagger lambda
ecs_ec2_instance_tagger-publish:
	$(call publish_lambda,shared_infra/ecs_ec2_instance_tagger)
