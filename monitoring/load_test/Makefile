ROOT = $(shell git rev-parse --show-toplevel)
LOAD_TEST = $(ROOT)/monitoring/load_test
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

gatling-build: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder \
		--project=gatling \
		--file=$(LOAD_TEST)/gatling/Dockerfile

gatling-deploy: gatling-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=gatling ./builds/publish_service.sh

gatling_to_cloudwatch-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(ECS_DASHBOARD)/gatling_to_cloudwatch:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

gatling_to_cloudwatch-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ECS_DASHBOARD)/gatling_to_cloudwatch/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

gatling_to_cloudwatch-publish: $(ROOT)/.docker/publish_lambda_zip \
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "gatling_to_cloudwatch/src" \
		--key="lambdas/ecs_dashboard/gatling_to_cloudwatch.zip" \
		--bucket="$(INFRA_BUCKET)";

load_test-build: gatling-build gatling_to_cloudwatch-build
load_test-test: gatling_to_cloudwatch-test
load_test-publish: gatling-deploy gatling_to_cloudwatch-publish

.PHONY:	gatling-build gatling-deploy gatling_to_cloudwatch-build gatling_to_cloudwatch-test gatling_to_cloudwatch-publish\
		load_test-build load_test-test load_test-publish